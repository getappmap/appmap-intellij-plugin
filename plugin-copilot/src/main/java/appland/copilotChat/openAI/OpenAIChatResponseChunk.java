package appland.copilotChat.openAI;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record OpenAIChatResponseChunk(
        @SerializedName("id") String id,
        @SerializedName("model") String model,
        @SerializedName("created") long created,
        @SerializedName("choices") List<OpenAIChatResponseChoice> choices,
        @SerializedName("system_fingerprint") String systemFingerprint,
        @SerializedName("object") String object
) {
    public record OpenAIChatResponseChoice(
            @SerializedName("index") int index,
            @SerializedName("delta") @NotNull OpenAIChatResponseChunk.Delta delta,
            @SerializedName("finish_reason") @Nullable String finishReason
    ) {
    }

    public record Delta(@Nullable @SerializedName("role") String role,
                        @Nullable @SerializedName("content") String content) {
    }
}
