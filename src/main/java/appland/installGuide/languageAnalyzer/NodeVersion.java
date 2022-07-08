package appland.installGuide.languageAnalyzer;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public final class NodeVersion {
    @SerializedName("major") int major;
    @SerializedName("minor") int minor;
    @SerializedName("path") int patch;
}
