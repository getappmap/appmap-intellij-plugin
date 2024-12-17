package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Items returned as part of a streaming Copilot chat response.
 */
public record CopilotChatResponseChoice(
        @SerializedName("index") int index,
        @SerializedName("delta") Delta delta,
        @SerializedName("finish_reason") @Nullable String finishReason
) {
    public record Delta(
            @SerializedName("role") @NotNull String role,
            @SerializedName("content") @NotNull String content
    ) {
    }
}
