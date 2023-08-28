package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode
@ToString
public class ScannerFindingEvent {
    @SerializedName("id")
    public @Nullable Integer id = null;

    @SerializedName("path")
    public @Nullable String path = null;

    @SerializedName("http_server_request")
    public @Nullable ScannerHttpRequestEvent httpServerRequest;
}
