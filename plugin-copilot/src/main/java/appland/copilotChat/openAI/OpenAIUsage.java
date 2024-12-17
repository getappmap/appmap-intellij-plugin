package appland.copilotChat.openAI;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record OpenAIUsage(
        @SerializedName("completion_tokens") int completionTokens,
        @SerializedName("prompt_tokens") int promptTokens,
        @SerializedName("total_tokens") int totalTokens,
        @SerializedName("prompt_tokens_details") Map<String, Integer> promptTokensDetails,
        @SerializedName("completion_tokens_details") Map<String, Integer> completionTokensDetails
) {
}
