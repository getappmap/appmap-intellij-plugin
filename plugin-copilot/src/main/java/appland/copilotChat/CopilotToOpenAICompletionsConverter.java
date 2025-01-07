package appland.copilotChat;

import appland.copilotChat.copilot.CopilotChatCompletionsStreamChunk.CopilotChatResponseChoice;
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
                               @NotNull List<CopilotChatResponseChoice> choices) {
        var openAIChoices = choices.stream().map(c -> {
            var delta = new OpenAIChatResponseChunk.Delta(c.delta().role(), c.delta().content());
            return new OpenAIChatResponseChunk.OpenAIChatResponseChoice(c.index(), delta, c.finishReason());
        }).toList();

        onNewChunk(new OpenAIChatResponseChunk(
                id,
                model,
                created,
                openAIChoices,
                CopilotChatSession.systemFingerprint,
                "chat-completion-chunk"));
    }
}
