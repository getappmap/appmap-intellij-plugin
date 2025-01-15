package appland.copilotChat.openAI;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record OpenAIChatCompletionsRequest(
        @SerializedName("model") String model,
        @SerializedName("stream") boolean stream,
        @SerializedName("messages") List<Message> messages,
        @SerializedName("temperature") @Nullable Double temperature,
        @SerializedName("top_p") @Nullable Double topP,
        @SerializedName("n") @Nullable Integer n
) {

    public record Message(
            @SerializedName("content") String content,
            @SerializedName("role") String role,
            @SerializedName("name") @Nullable String name
    ) {
    }
}
