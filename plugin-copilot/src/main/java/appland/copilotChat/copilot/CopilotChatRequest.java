package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CopilotChatRequest(
        @SerializedName("model") String model,
        @SerializedName("messages") List<Message> messages,
        @SerializedName("max_tokens") int maxTokens,
        @SerializedName("stream") boolean stream,
        @SerializedName("temperature") @Nullable Double temperature,
        @SerializedName("top_p") @Nullable Double topP,
        @SerializedName("n") @Nullable Integer n
) {
    public record Message(
            @SerializedName("content") String content,
            @SerializedName("role") CopilotChatRole role
    ) {
    }
}