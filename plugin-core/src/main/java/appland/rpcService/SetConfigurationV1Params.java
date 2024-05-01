package appland.rpcService;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * JSON-RPC parameters for message "v1.configuration.set".
 */
class SetConfigurationV1Params {
    @SerializedName("appmapConfigFiles")
    public final Collection<String> appmapConfigFiles;

    SetConfigurationV1Params(@NotNull Collection<String> appmapConfigFiles) {
        this.appmapConfigFiles = appmapConfigFiles;
    }
}
