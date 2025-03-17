package appland.copilotChat.copilot;

import appland.utils.GsonUtils;
import appland.utils.UserLog;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CopilotChatSession {
    private static final Logger LOG = Logger.getInstance(CopilotChatSession.class);

    private final @NotNull String endpoint;
    private final @NotNull UpdatingCopilotToken copilotToken;
    private final @NotNull Map<@NotNull String, @NotNull String> baseHeaders;
    private final @NotNull UserLog userLog;

    CopilotChatSession(@NotNull String endpoint,
                       @NotNull UpdatingCopilotToken copilotToken,
                       @NotNull Map<String, String> baseHeaders,
                       @NotNull UserLog userLog) {
        this.endpoint = endpoint;
        this.copilotToken = copilotToken;
        this.baseHeaders = baseHeaders;
        this.userLog = userLog;
    }

    public List<CopilotModelDefinition> loadChatModels() throws IOException {
        record ModelsResponse(@SerializedName("data") CopilotModelDefinition[] models) {
        }

        var modelsEndpoint = Urls.newFromEncoded(endpoint + "/models");
        userLog.log("Loading models from " + modelsEndpoint);
        var response = HttpRequests.request(modelsEndpoint.toExternalForm())
                .accept("application/json")
                .gzip(true)
                .isReadResponseOnError(true)
                .tuner(connection -> {
                    applyHeaders(connection, baseHeaders);
                    connection.setRequestProperty(HttpHeaderNames.AUTHORIZATION.toString(), copilotToken.getAuthorizationHeader());
                })
                .readString();

        return Arrays.stream(GsonUtils.GSON.fromJson(response, ModelsResponse.class).models)
                .filter(model -> "chat".equals(model.capabilities().type()))
                .toList();
    }

    public void ask(@NotNull CopilotChatResponseListener responseListener,
                    @NotNull CopilotModelDefinition model,
                    @NotNull List<CopilotChatRequest.Message> messages,
                    @NotNull Map<String, String> proxiedRequestHeaders,
                    @Nullable Double temperature,
                    @Nullable Double topP,
                    @Nullable Integer n) throws IOException {
        var maxOutputTokens = model.capabilities().limits().maxOutputTokens();
        var appliedTemp = temperature != null ? temperature : GitHubCopilot.CHAT_DEFAULT_TEMPERATURE;
        var copilotRequest = new CopilotChatRequest(model.id(), messages, maxOutputTokens, true, appliedTemp, topP, n);

        try {
            var chatEndpoint = Urls.newFromEncoded(endpoint + "/chat/completions");
            userLog.log("Sending chat request to " + chatEndpoint);
            HttpRequests.post(chatEndpoint.toExternalForm(), "application/json")
                    .gzip(true)
                    .throwStatusCodeException(true)
                    .isReadResponseOnError(true)
                    .tuner(connection -> {
                        applyHeaders(connection, baseHeaders);
                        applyHeaders(connection, proxiedRequestHeaders);
                        connection.setRequestProperty(HttpHeaderNames.AUTHORIZATION.toString(), copilotToken.getAuthorizationHeader());
                        connection.setRequestProperty(GitHubCopilot.HEADER_OPENAI_INTENT, "conversation-panel");
                        connection.setRequestProperty(GitHubCopilot.HEADER_OPENAI_ORGANIZATION, "github-copilot");
                        connection.setRequestProperty(GitHubCopilot.HEADER_REQUEST_ID, requestId());
                    })
                    .connectTimeout(10000)
                    .readTimeout(60000)
                    .connect(httpRequest -> {
                        httpRequest.write(GsonUtils.GSON.toJson(copilotRequest));

                        // Don't call "getReader()" for error responses,
                        // to not prevent the method calling this processor will throw an HTTP error exception.
                        if (httpRequest.getConnection() instanceof HttpURLConnection) {
                            var responseCode = ((HttpURLConnection) httpRequest.getConnection()).getResponseCode();
                            userLog.log("Chat request response code: " + responseCode);
                            if (responseCode >= 400) {
                                return null;
                            }
                        }

                        readServerEvents(httpRequest.getReader(), responseListener);
                        return null;
                    });
        } catch (IOException e) {
            userLog.log("Failed to send chat request. " + e.toString());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to send chat request. ", e);
            }
            throw e;
        }
    }

    private static void readServerEvents(@NotNull BufferedReader response,
                                         @NotNull CopilotChatResponseListener responseListener) throws IOException {
        var pendingEventData = new StringBuilder();
        try (response) {
            do {
                var line = response.readLine();
                if (line == null) {
                    break;
                }

                if (line.startsWith("data:")) {
                    pendingEventData.append(line.substring("data:".length()).trim());
                } else if (line.isEmpty()) {
                    var jsonString = pendingEventData.toString();
                    pendingEventData.setLength(0);

                    var chunkText = jsonString.trim();
                    if ("[DONE]".equals(chunkText)) {
                        responseListener.end();
                        break;
                    }

                    processSSEChunk(responseListener, chunkText);
                }
            } while (true);
        }
    }

    private static void processSSEChunk(@NotNull CopilotChatResponseListener responseListener,
                                        @NotNull String sseData) {
        var chunk = GsonUtils.GSON.fromJson(sseData, CopilotChatCompletionsStreamChunk.class);

        var id = chunk.id();
        var modelName = chunk.model();
        var created = chunk.created();
        responseListener.onChatResponse(id, modelName, created, chunk.choices());
    }

    private void applyHeaders(@NotNull URLConnection connection, @NotNull Map<String, String> headers) {
        for (var entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    public static @NotNull String requestId() {
        return UUID.randomUUID().toString();
    }

    // dummy ID to send as system_fingerprint property to OpenAI/Navie
    public static @NotNull String systemFingerprint = UUID.randomUUID().toString();
}
