package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;

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
            @SerializedName("limits") CopilotCapabilityLimits limits
    ) {
    }

    // only including the limits we actually need
    public record CopilotCapabilityLimits(
            @SerializedName("max_output_tokens") int maxOutputTokens,
            @SerializedName("max_prompt_tokens") int maxPromptTokens
    ) {
    }
}