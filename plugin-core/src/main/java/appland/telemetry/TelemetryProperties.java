package appland.telemetry;

import appland.AppMapPlugin;
import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A utility class for creating and formatting telemetry properties.
 * It provides static methods to generate the base properties, add prefixes, and serialize them to JSON.
 */
public final class TelemetryProperties {
    public static final @NonNls String EXT_NAME = "extname";
    public static final @NonNls String EXT_VERSION = "extversion";
    public static final @NonNls String IDE = "ide";
    public static final @NonNls String IDE_VERSION = "ideversion";
    public static final @NonNls String OS = "os";
    public static final @NonNls String OS_VERSION = "osversion";
    public static final @NonNls String JVM_VERSION = "jvmversion";
    public static final @NonNls String PRODUCT = "product";
    public static final @NonNls String SOURCE = "source";
    public static final @NonNls String USERNAME = "username";

    private TelemetryProperties() {
    }

    /**
     * Creates a new map of telemetry properties.
     *
     * @param includeUsername A boolean indicating whether to include the username.
     * @return A map containing the telemetry properties.
     */
    public static @NotNull Map<String, String> create(boolean includeUsername) {
        var properties = new HashMap<String, String>();
        properties.put(EXT_NAME, AppMapPlugin.getDescriptor().getPluginId().getIdString());
        properties.put(EXT_VERSION, AppMapPlugin.getDescriptor().getVersion());
        properties.put(IDE, ApplicationInfo.getInstance().getFullApplicationName());
        properties.put(IDE_VERSION, ApplicationInfo.getInstance().getFullVersion());
        properties.put(OS, SystemInfo.OS_NAME);
        properties.put(OS_VERSION, SystemInfo.OS_VERSION);
        properties.put(JVM_VERSION, System.getProperty("java.version"));
        properties.put(PRODUCT, ApplicationInfo.getInstance().getBuild().getProductCode());
        properties.put(SOURCE, "JetBrains");

        if (includeUsername) {
            try {
                properties.put(USERNAME, System.getProperty("user.name"));
            } catch (Exception e) {
                // ignore
            }
        }
        return properties;
    }

    /**
     * Returns an unmodifiable map containing the telemetry properties with the "common." prefix
     * added to each key.
     *
     * @param properties The input properties map.
     * @return An unmodifiable map with prefixed keys.
     */
    public static @NotNull Map<String, String> withCommonPrefix(@NotNull Map<String, String> properties) {
        return Collections.unmodifiableMap(properties.entrySet().stream()
                .collect(Collectors.toMap(entry -> "common." + entry.getKey(), Map.Entry::getValue)));
    }

    /**
     * Returns a JSON representation of a limited set of properties required by the CLI.
     *
     * @param properties The input properties map.
     * @return A JSON string containing the CLI properties.
     */
    public static @NotNull String toCliJson(@NotNull Map<String, String> properties) {
        return GsonUtils.GSON.toJson(new CliProperties(properties));
    }

    /**
     * Class to serialize for the CLI env variable.
     * It's re-serializing from a map to apply the @SerializedName annotations.
     */
    private static class CliProperties {
        @SerializedName(EXT_NAME)
        final String extName;
        @SerializedName(EXT_VERSION)
        final String extVersion;
        @SerializedName(IDE)
        final String ide;
        @SerializedName(IDE_VERSION)
        final String ideVersion;

        public CliProperties(@NotNull Map<String, String> properties) {
            // Note these are the only properties we need for the CLI
            this.extName = properties.get(EXT_NAME);
            this.extVersion = properties.get(EXT_VERSION);
            this.ide = properties.get(IDE);
            this.ideVersion = properties.get(IDE_VERSION);
        }
    }
}
