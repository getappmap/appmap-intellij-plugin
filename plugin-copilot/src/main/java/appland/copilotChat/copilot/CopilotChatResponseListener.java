package appland.copilotChat.copilot;

import org.jetbrains.annotations.NotNull;

public interface CopilotChatResponseListener {
    void onChatResponse(@NotNull String id,
                        @NotNull String model,
                        long created,
                        @NotNull CopilotChatResponseChoice item);

    void end();
}
