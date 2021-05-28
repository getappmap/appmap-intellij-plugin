package appland.remote;

import com.google.gson.annotations.SerializedName;

/**
 * Response as returned by "GET /_appmap/record".
 */
public class RecordGetResponse {
    @SerializedName("enabled")
    public boolean enabled;
}
