package appland.copilotChat.openAI;

import appland.copilotChat.copilot.CopilotChatRole;
import org.jetbrains.annotations.NotNull;

public final class OpenAIChatRoles {
    public static final String System = "system";
    public static final String User = "user";
    public static final String Assistant = "assistant";
    // OpenAI: With o1 models and newer, developer messages replace the previous system messages.
    public static final String Developer = "developer";

    public static @NotNull CopilotChatRole fromOpenAIRole(@NotNull String role) {
        return switch (role) {
            case System, Developer -> CopilotChatRole.System;
            case Assistant -> CopilotChatRole.Assistant;
            default -> CopilotChatRole.User;
        };
    }
}