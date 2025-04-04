package appland.cli;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Extension to abstract the models available for the AppMap JSON-RPC service.
 */
public interface AppLandModelInfoProvider {
    ExtensionPointName<AppLandModelInfoProvider> EP_NAME = ExtensionPointName.create("appland.cli.modelInfoProvider");

    /**
     * @return List of available models for the AppMap JSON-RPC service.
     */
    @Nullable List<ModelInfo> getModelInfo() throws IOException;

    /**
     * Record representing a language model with its configuration details.
     *
     * @param name           User-friendly model name, e.g., "GPT 4"
     * @param id             Unique model identifier, e.g., "gpt-4"
     * @param provider       Provider name, e.g., "Copilot"
     * @param createdAt      ISO 8601 timestamp string when the model was created
     * @param baseUrl        Optional base URL for the model API, e.g., the proxy URL. Can be null.
     * @param apiKey         Optional API key if available directly, e.g., token used for proxy authentication. Can be null.
     * @param maxInputTokens Optional maximum number of input tokens the model supports, e.g., 8192. Can be null.
     */
    record ModelInfo(
            @SerializedName("name") @NotNull String name,
            @SerializedName("id") @NotNull String id,
            @SerializedName("provider") @NotNull String provider,
            @SerializedName("createdAt") @NotNull String createdAt,
            @SerializedName("baseUrl") @Nullable String baseUrl,
            @SerializedName("apiKey") @Nullable String apiKey,
            @SerializedName("maxInputTokens") @Nullable Integer maxInputTokens
    ) {
    }
}
