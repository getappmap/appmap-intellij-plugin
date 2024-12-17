package appland.copilotChat.copilot;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CopilotChatSession {
    private final @NotNull String endpoint;
    private final @NotNull UpdatingCopilotToken copilotToken;
    private final @NotNull Map<@NotNull String, @NotNull String> baseHeaders;

    CopilotChatSession(@NotNull String endpoint,
                       @NotNull UpdatingCopilotToken copilotToken,
                       @NotNull Map<String, String> baseHeaders) {
        this.endpoint = endpoint;
        this.copilotToken = copilotToken;
        this.baseHeaders = baseHeaders;
    }

    public List<CopilotModelDefinition> loadModels() throws IOException {
        record ModelsResponse(@SerializedName("data") CopilotModelDefinition[] models) {
        }

        var modelsEndpoint = Urls.newFromEncoded(endpoint + "/models");
        var response = HttpRequests.request(modelsEndpoint.toExternalForm())
                .accept("application/json")
                .gzip(true)
                .isReadResponseOnError(true)
                .tuner(connection -> applyHeaders(connection, baseHeaders))
                .readString();
        return List.of(GsonUtils.GSON.fromJson(response, ModelsResponse.class).models);
    }

    public void ask(@NotNull CopilotChatResponseListener responseListener,
                    @NotNull CopilotModelDefinition model,
                    @NotNull List<CopilotChatRequest.Message> messages,
                    @Nullable Double temperature,
                    @Nullable Double topP,
                    @Nullable Integer n) throws IOException {
        var maxOutputTokens = model.capabilities().limits().get(CopilotModelDefinition.CopilotCapabilityLimit.MaxOutputTokens);
        var appliedTemp = temperature != null ? temperature : 0.1;
        var copilotRequest = new CopilotChatRequest(model.id(), messages, maxOutputTokens, true, appliedTemp, topP, n);

        var chatEndpoint = Urls.newFromEncoded(endpoint + "/chat/completions");
        HttpRequests.post(chatEndpoint.toExternalForm(), "application/json")
                .isReadResponseOnError(true)
                .gzip(true)
                .tuner(connection -> {
                    applyHeaders(connection, baseHeaders);
                    connection.setRequestProperty("openai-intent", "conversation-panel");
                    connection.setRequestProperty("openai-organization", "github-copilot");
                    connection.setRequestProperty("x-request-id", requestId());
                })
                .connect(httpRequest -> {
                    httpRequest.write(GitHubCopilotService.gson.toJson(copilotRequest));
                    readServerEvents(httpRequest.getReader(), responseListener);
                    return null;
                });
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
        var chunk = GitHubCopilotService.gson.fromJson(sseData, CopilotChatCompletionsStreamChunk.class);

        var id = chunk.id();
        var modelName = chunk.model();
        var created = chunk.created();
        for (var choice : chunk.choices()) {
            responseListener.onChatResponse(id, modelName, created, choice);
        }
    }

    private void applyHeaders(@NotNull URLConnection connection, @NotNull Map<String, String> headers) {
        connection.addRequestProperty(HttpHeaderNames.AUTHORIZATION.toString(), copilotToken.getAuthorizationHeader());
        for (var entry : headers.entrySet()) {
            connection.addRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    public static @NotNull String requestId() {
        return UUID.randomUUID().toString();
    }

    // dummy ID to send as system_fingerprint property to OpenAI/Navie
    public static @NotNull String systemFingerprint = UUID.randomUUID().toString();

    private record CopilotChatCompletionsStreamChunk(
            @SerializedName("id") @NotNull String id,
            @SerializedName("model") @NotNull String model,
            @SerializedName("created") long created,
            @SerializedName("choices") List<@NotNull CopilotChatResponseChoice> choices
    ) {
    }
}
