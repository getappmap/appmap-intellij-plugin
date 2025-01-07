package appland.copilotChat;

import appland.copilotChat.copilot.*;
import appland.copilotChat.openAI.*;
import appland.utils.GsonUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.NettyUtil;
import org.jetbrains.io.Responses;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adds a new request handler to the IDE's built-in HTTP server.
 * Navie sends a request to this request handler.
 * The handler requests data from GitHub Copilot and sends it back to Navie in the expected OpenAI response format.
 */
public class NavieCopilotChatRequestHandler extends HttpRequestHandler {
    public static final String BASE_PATH = "/vscode/copilot";
    private static final Logger LOG = Logger.getInstance(NavieCopilotChatRequestHandler.class);

    private volatile @Nullable CopilotChatSession _cachedChat;
    private volatile @Nullable List<CopilotModelDefinition> _cachedModels;

    public static @NotNull String getBaseUrl() {
        var port = BuiltInServerManager.getInstance().getPort();
        return Urls.newUrl("http", "127.0.0.1:" + port, NavieCopilotChatRequestHandler.BASE_PATH).toExternalForm();
    }

    @Override
    public boolean isSupported(@NotNull FullHttpRequest request) {
        return request.method() == HttpMethod.POST;
    }

    @Override
    public boolean process(@NotNull QueryStringDecoder queryStringDecoder,
                           @NotNull FullHttpRequest fullHttpRequest,
                           @NotNull ChannelHandlerContext channelHandlerContext) {
        String requestPath;
        try {
            requestPath = queryStringDecoder.path();
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!requestPath.startsWith(BASE_PATH + "/") && !requestPath.equals(BASE_PATH)) {
            return false;
        }

        if (isEqualByHash("/chat/completions", requestPath.substring(BASE_PATH.length()))) {
            // verify the authorization header, which must contain our random ide session id
            var expectedAuthHeader = "Bearer " + GitHubCopilotService.RandomIdeSessionId;
            if (isEqualByHash(expectedAuthHeader, fullHttpRequest.headers().get(HttpHeaderNames.AUTHORIZATION))) {
                handleChatCompletions(fullHttpRequest, channelHandlerContext);
                return true;
            }
        }

        return false;
    }

    private void handleChatCompletions(@NotNull FullHttpRequest fullHttpRequest,
                                       @NotNull ChannelHandlerContext channelHandlerContext) {
        var chatSession = cachedCopilotChatSession();
        if (chatSession == null) {
            Responses.response(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    fullHttpRequest,
                    "No GitHub Copilot session found");
            return;
        }

        var requestBody = fullHttpRequest.content().toString(StandardCharsets.UTF_8);
        var openAIRequest = GsonUtils.GSON.fromJson(requestBody, OpenAIChatCompletionsRequest.class);

        var copilotModel = getCopilotModel(openAIRequest.model(), GitHubCopilot.FALLBACK_MODEL_NAME);
        if (copilotModel == null) {
            Responses.response(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    fullHttpRequest,
                    "Unable to find GitHub Copilot model " + openAIRequest.model());
            return;
        }

        try {
            if (openAIRequest.stream()) {
                sendStreamingChatResponse(chatSession, copilotModel, openAIRequest, channelHandlerContext);
            } else {
                sendCompleteChatResponse(chatSession, copilotModel, fullHttpRequest, openAIRequest, channelHandlerContext);
            }
        } catch (HttpRequests.HttpStatusException e) {
            Responses.response(HttpResponseStatus.valueOf(e.getStatusCode()), fullHttpRequest, e.getMessage());
            NettyUtil.logAndClose(e, Logger.getInstance(this.getClass()), channelHandlerContext.channel());
        } catch (Exception e) {
            var message = "Unknown error executing HTTP request: " + e.getMessage();
            Responses.response(HttpResponseStatus.INTERNAL_SERVER_ERROR, fullHttpRequest, message);
            NettyUtil.logAndClose(e, Logger.getInstance(this.getClass()), channelHandlerContext.channel());
        }
    }

    private void sendStreamingChatResponse(@NotNull CopilotChatSession chatSession,
                                           @NotNull CopilotModelDefinition copilotModel,
                                           @NotNull OpenAIChatCompletionsRequest openAIRequest,
                                           @NotNull ChannelHandlerContext context) throws IOException {
        var channel = context.channel();

        var converter = new CopilotToOpenAICompletionsConverter() {
            @Override
            protected void onNewChunk(@NotNull OpenAIChatResponseChunk chunk) {
                var json = GsonUtils.GSON.toJson(chunk);
                var httpContent = new DefaultHttpContent(Unpooled.copiedBuffer("data: " + json + "\n\n", StandardCharsets.UTF_8));
                try {
                    channel.writeAndFlush(httpContent);
                } finally {
                    httpContent.release();
                }
            }

            @Override
            public void end() {
                var httpContent = new DefaultHttpContent(Unpooled.copiedBuffer("data: [DONE]\n\n", StandardCharsets.UTF_8));
                try {
                    channel.writeAndFlush(httpContent);
                } finally {
                    httpContent.release();
                }

                var future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                future.addListener(ChannelFutureListener.CLOSE);
            }
        };

        var response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, must-revalidate");
        response.headers().set(HttpHeaderNames.LAST_MODIFIED, new Date(Calendar.getInstance().getTimeInMillis()));
        channel.writeAndFlush(response);

        chatSession.ask(converter,
                copilotModel,
                asCopilotChatMessages(openAIRequest.messages()),
                openAIRequest.temperature(),
                openAIRequest.topP(),
                openAIRequest.n());
    }

    private void sendCompleteChatResponse(@NotNull CopilotChatSession chatSession,
                                          @NotNull CopilotModelDefinition copilotModel,
                                          @NotNull FullHttpRequest fullHttpRequest,
                                          @NotNull OpenAIChatCompletionsRequest openAIRequest,
                                          @NotNull ChannelHandlerContext channelHandlerContext) throws IOException {
        var tokenizer = GitHubCopilotService.getInstance().loadTokenizer(copilotModel.capabilities().tokenizer());

        var promptTokens = new AtomicInteger(0);
        for (var message : openAIRequest.messages()) {
            promptTokens.addAndGet(tokenizer.countTokens(message.content()));
        }

        var listener = new CopilotToOpenAICompletionsConverter() {
            private final StringBuffer jsonOutput = new StringBuffer();
            private final AtomicInteger completionTokens = new AtomicInteger();
            private volatile @Nullable String lastFinishReason;

            @Override
            protected void onNewChunk(@NotNull OpenAIChatResponseChunk chunk) {
                for (var choice : chunk.choices()) {
                    var content = choice.delta().content();
                    if (StringUtil.isNotEmpty(content)) {
                        jsonOutput.append(content);
                        completionTokens.addAndGet(tokenizer.countTokens(content));
                    }

                    if (StringUtil.isNotEmpty(choice.finishReason())) {
                        lastFinishReason = choice.finishReason();
                    }
                }
            }

            @Override
            public void end() {
                var message = new OpenAICompletionsStaticResponse.Choice(
                        0,
                        new OpenAICompletionsStaticResponse.Message(OpenAIChatRoles.Assistant, jsonOutput.toString(), null),
                        null,
                        lastFinishReason);

                var usage = new OpenAIUsage(completionTokens.get(),
                        promptTokens.get(),
                        promptTokens.get() + completionTokens.get(),
                        Map.of("cached_tokens", 0,
                                "audio_tokens", 0),
                        Map.of("reasoning_tokens", 0,
                                "audio_tokens", 0,
                                "accepted_prediction_tokens", 0,
                                "rejected_prediction_tokens", 0));

                var response = new OpenAICompletionsStaticResponse(
                        UUID.randomUUID().toString(),
                        "chat.completion",
                        Instant.now().getEpochSecond(),
                        openAIRequest.model(),
                        List.of(message),
                        usage,
                        CopilotChatSession.systemFingerprint
                );

                sendData(GsonUtils.GSON.toJson(response).getBytes(StandardCharsets.UTF_8),
                        "_.json",
                        fullHttpRequest,
                        channelHandlerContext.channel(),
                        new ReadOnlyHttpHeaders(true,
                                HttpHeaderNames.CONTENT_TYPE, "application/json",
                                "openai-organization", GitHubCopilot.OPEN_AI_ORGANIZATION,
                                "openai-version", GitHubCopilot.OPEN_AI_VERSION,
                                "x-request-id", CopilotChatSession.requestId()));
            }
        };

        chatSession.ask(listener,
                copilotModel,
                asCopilotChatMessages(openAIRequest.messages()),
                openAIRequest.temperature(),
                openAIRequest.topP(),
                openAIRequest.n());
    }

    private @NotNull List<CopilotChatRequest.Message> asCopilotChatMessages(List<OpenAIChatCompletionsRequest.Message> messages) {
        return messages.stream().map(message -> {
            return new CopilotChatRequest.Message(message.content(), OpenAIChatRoles.fromOpenAIRole(message.role()));
        }).toList();
    }

    private @Nullable CopilotModelDefinition getCopilotModel(@NotNull String name, @NotNull String fallbackModelName) {
        try {
            var models = cachedCopilotModels();
            if (models == null) {
                return null;
            }

            var model = models.stream().filter(m -> name.equals(m.id())).findFirst();
            return model.orElseGet(() -> {
                return models.stream().filter(m -> fallbackModelName.equals(m.id())).findFirst().orElse(null);
            });
        } catch (Exception e) {
            LOG.warn("Failed to load GitHub Copilot model " + name, e);
            return null;
        }
    }

    private @Nullable List<CopilotModelDefinition> cachedCopilotModels() throws IOException {
        if (_cachedModels == null) {
            var chatSession = cachedCopilotChatSession();
            if (chatSession != null) {
                _cachedModels = chatSession.loadModels();
            }
        }
        return _cachedModels;
    }

    private @Nullable CopilotChatSession cachedCopilotChatSession() {
        if (_cachedChat == null) {
            _cachedChat = GitHubCopilotService.getInstance().createChatSession();
        }
        return _cachedChat;
    }

    /**
     * Compare two strings using a hash comparison to avoid timing attacks.
     */
    private static boolean isEqualByHash(@NotNull String expectedValue, @NotNull String value) {
        return MessageDigest.isEqual(expectedValue.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
    }
}
