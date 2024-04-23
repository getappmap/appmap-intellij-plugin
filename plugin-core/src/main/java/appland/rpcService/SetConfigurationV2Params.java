package appland.rpcService;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * JSON-RPC parameters for message "v2.configuration.set".
 */
class SetConfigurationV2Params {
    @SerializedName("projectDirectories")
    public final Collection<String> projectDirectories;
    @SerializedName("appmapConfigFiles")
    public final Collection<String> appmapConfigFiles;

    SetConfigurationV2Params(@NotNull Collection<@NotNull String> projectDirectories,
                             @NotNull Collection<@NotNull String> appmapConfigFiles) {
        this.appmapConfigFiles = appmapConfigFiles;
        this.projectDirectories = projectDirectories;
    }
}
