package appland.webviews.aihelp;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class AiHelpWebViewSettings {
    @SerializedName("apiKey")
    @NotNull String apiKey;

    @SerializedName("apiUrl")
    @NotNull String apiUrl = "https://api.getappmap.com";
}
