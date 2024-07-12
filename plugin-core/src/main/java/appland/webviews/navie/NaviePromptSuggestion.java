package appland.webviews.navie;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the JSON data of a Navie prompt suggestion.
 */
public record NaviePromptSuggestion(
        @SerializedName("label")
        @NotNull String label,
        @SerializedName("prompt")
        @NotNull String prompt) {
}