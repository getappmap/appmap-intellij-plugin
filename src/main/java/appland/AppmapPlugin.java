package appland;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class AppmapPlugin {
    @NotNull
    public static Path getPluginPath() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        assert plugin != null;

        var basePath = plugin.getPluginPath();
        assert basePath != null;

        return basePath;
    }

    public static Path getAppMapHTMLPath() {
        return getPluginPath().resolve("appmap").resolve("index.html");
    }

    private static final String PLUGIN_ID = "app.land.appmap";
}
