package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record CopilotModelDefinition(
        @SerializedName("id") String id,
        @SerializedName("name") String name,
        @SerializedName("vendor") String vendor,
        @SerializedName("version") String version,
        @SerializedName("preview") boolean preview,
        @SerializedName("capabilities") CopilotModelCapabilities capabilities
) {
    public record CopilotModelCapabilities(
            @SerializedName("family") String family,
            @SerializedName("type") String type,
            @SerializedName("tokenizer") String tokenizer,
            @SerializedName("limits") Map<CopilotCapabilityLimit, Integer> limits
    ) {
    }

    public enum CopilotCapabilityLimit {
        @SerializedName("max_context_window_tokens")
        MaxContextWindowTokens,

        @SerializedName("max_output_tokens")
        MaxOutputTokens,

        @SerializedName("max_prompt_tokens")
        MaxPromptTokens
    }
}