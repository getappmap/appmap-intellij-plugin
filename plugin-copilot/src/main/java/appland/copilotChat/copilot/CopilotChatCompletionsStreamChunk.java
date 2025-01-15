package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CopilotChatCompletionsStreamChunk(
        @SerializedName("id") @NotNull String id,
        @SerializedName("model") @NotNull String model,
        @SerializedName("created") long created,
        @SerializedName("choices") List<@NotNull CopilotChatResponseChoice> choices
) {
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
}
