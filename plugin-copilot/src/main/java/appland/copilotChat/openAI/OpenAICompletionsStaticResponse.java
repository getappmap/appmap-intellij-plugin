package appland.copilotChat.openAI;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record OpenAICompletionsStaticResponse(
        @SerializedName("id") String id,
        @SerializedName("object") String object,
        @SerializedName("created") long created,
        @SerializedName("model") String model,
        @SerializedName("choices") List<Choice> choices,
        @SerializedName("usage") OpenAIUsage usage,
        @SerializedName("system_fingerprint") String systemFingerprint
) {
    public record Choice(
            @SerializedName("index") int index,
            @SerializedName("message") @NotNull Message message,
            @SerializedName("logprobs") @Nullable String[] logprobs,
            @SerializedName("finish_reason") @Nullable String finishReason) {
    }

    public record Message(
            @SerializedName("role") String role,
            @SerializedName("content") String content,
            @SerializedName("refusal") String refusal // fixme unclear
    ) {
    }
}
