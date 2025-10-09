package appland;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class AppMapPlugin {
    public static final String REMOTE_RECORDING_HELP_URL = "https://appmap.io/docs/recording-methods.html#remote-recording";
    public static final @NotNull Url DEFAULT_SERVER_URL = Urls.newUrl("https", "getappmap.com", "");

    private static final String PLUGIN_ID = "appland.appmap";

    private AppMapPlugin() {
    }

    public static @NotNull Path getPluginPath() {
        var plugin = getDescriptor();
        var basePath = plugin.getPluginPath();
        assert basePath != null;

        return basePath;
    }

    public static @NotNull PluginDescriptor getDescriptor() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        assert plugin != null;
        return plugin;
    }

    public static @NotNull Path getAppMapJavaAgentPath() {
        return getPluginPath().resolve("resources").resolve("appmap-agent.jar");
    }
}
