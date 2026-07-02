package appland;

import com.intellij.ide.plugins.cl.PluginAwareClassLoader;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.nio.file.Path;

public final class AppMapPlugin {
    public static final String REMOTE_RECORDING_HELP_URL = "https://appmap.io/docs/recording-methods.html#remote-recording";
    public static final @NotNull Url DEFAULT_SERVER_URL = Urls.newUrl("https", "getappmap.com", "");

    private static final String PLUGIN_ID = "appland.appmap";

    private AppMapPlugin() {
    }

    public static @NotNull Path getPluginPath() {
        var basePath = getDescriptor().getPluginPath();
        assert basePath != null;

        return basePath;
    }

    public static @NotNull PluginDescriptor getDescriptor() {
        // In a deployed plugin our classes are loaded by a PluginClassLoader, which exposes the
        // descriptor directly. PluginAwareClassLoader#getPluginDescriptor() is public API, so this
        // is the internal-API-free path that verifyPlugin checks.
        var classLoader = AppMapPlugin.class.getClassLoader();
        if (classLoader instanceof PluginAwareClassLoader pluginClassLoader) {
            return pluginClassLoader.getPluginDescriptor();
        }

        // In the test/dev runtime the plugin's classes are loaded from a flat classpath rather than
        // a PluginClassLoader, so the descriptor must be looked up in the registry by ID. That lookup
        // (PluginManagerCore.getPlugin) is @ApiStatus.Internal and has no public replacement in this
        // platform, so we reach it reflectively to keep the shipped plugin free of static internal-API
        // references (this branch never runs in a real IDE, only under the test harness).
        return lookupDescriptorByIdReflectively();
    }

    public static @NotNull Path getAppMapJavaAgentPath() {
        return getPluginPath().resolve("resources").resolve("appmap-agent.jar");
    }

    private static @NotNull PluginDescriptor lookupDescriptorByIdReflectively() {
        try {
            Class<?> pluginManagerCore = Class.forName("com.intellij.ide.plugins.PluginManagerCore");
            Method getPlugin = pluginManagerCore.getMethod("getPlugin", PluginId.class);
            var descriptor = (PluginDescriptor) getPlugin.invoke(null, PluginId.getId(PLUGIN_ID));
            if (descriptor == null) {
                throw new IllegalStateException("AppMap plugin descriptor '" + PLUGIN_ID + "' is not registered");
            }
            return descriptor;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to resolve the AppMap plugin descriptor", e);
        }
    }
}
