package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record CopilotToken(
        @SerializedName("annotations_enabled") boolean isAnnotationsEnabled,
        @SerializedName("chat_enabled") boolean isChatEnabled,
        @SerializedName("chat_jetbrains_enabled") boolean isJetBrainsChatEnabled,
        @SerializedName("code_quote_enabled") boolean isCodeQuoteEnabled,
        @SerializedName("code_review_enabled") boolean isCodeReviewEnabled,
        @SerializedName("codesearch") boolean codeSearch,
        @SerializedName("copilotignore_enabled") boolean copilotIgnoreEnabled,
        @SerializedName("endpoints") Map<CopilotEndpoint, String> endpoints,
        @SerializedName("expires_at") long expiresAt,
        @SerializedName("individual") boolean individual,
        @SerializedName("nes_enabled") boolean nesEnabled,
        @SerializedName("prompt_8k") boolean prompt8k,
        @SerializedName("public_suggestions") String publicSuggestions,
        @SerializedName("refresh_in") int refreshIn,
        @SerializedName("sku") String sku,
        @SerializedName("snippy_load_test_enabled") boolean snippyLoadTestEnabled,
        @SerializedName("telemetry") String telemetry,
        @SerializedName("token") String token,
        @SerializedName("tracking_id") String trackingId
) {
    public enum CopilotEndpoint {
        @SerializedName("api") API,
        @SerializedName("proxy") Proxy,
        @SerializedName("origin-tracker") OriginTracker,
        @SerializedName("telemetry") Telemetry,
    }
}