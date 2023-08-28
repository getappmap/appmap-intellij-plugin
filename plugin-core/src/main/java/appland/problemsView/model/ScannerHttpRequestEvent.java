package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode
@ToString
public class ScannerHttpRequestEvent {
    @SerializedName("request_method")
    @Nullable
    public String requestMethod;

    @SerializedName("path_info")
    @Nullable
    public String pathInfo;
}
