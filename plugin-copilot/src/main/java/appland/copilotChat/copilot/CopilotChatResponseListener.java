package appland.copilotChat.copilot;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CopilotChatResponseListener {
    void onChatResponse(@NotNull String id,
                        @NotNull String model,
                        long created,
                        @NotNull List<CopilotChatCompletionsStreamChunk.CopilotChatResponseChoice> choices);

    void end();
}
