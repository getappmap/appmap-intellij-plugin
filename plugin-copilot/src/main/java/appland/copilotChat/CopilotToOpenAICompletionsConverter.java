package appland.copilotChat;

import appland.copilotChat.copilot.CopilotChatResponseChoice;
import appland.copilotChat.copilot.CopilotChatResponseListener;
import appland.copilotChat.copilot.CopilotChatSession;
import appland.copilotChat.openAI.OpenAIChatResponseChunk;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Takes items from the stream of GitHub Copilot completions and send them out as OpenAI-compatible completion stream items.
 */
abstract class CopilotToOpenAICompletionsConverter implements CopilotChatResponseListener {
    protected abstract void onNewChunk(@NotNull OpenAIChatResponseChunk choice);

    @Override
    public void onChatResponse(@NotNull String id,
                               @NotNull String model,
                               long created,
                               @NotNull CopilotChatResponseChoice item) {
        var delta = new OpenAIChatResponseChunk.Delta(item.delta().role(), item.delta().content());
        var choice = new OpenAIChatResponseChunk.OpenAIChatResponseChoice(item.index(), delta, item.finishReason());
        var chunk = new OpenAIChatResponseChunk(id,
                "chat-completion-chunk",
                created,
                model,
                CopilotChatSession.systemFingerprint,
                List.of(choice));

        onNewChunk(chunk);
    }
}
