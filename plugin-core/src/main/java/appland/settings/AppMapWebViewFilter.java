package appland.settings;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AppMapWebViewFilter {
    @SerializedName("filterName")
    String filterName;

    @SerializedName("state")
    String state;

    @SerializedName("default")
    boolean isDefault;
}
