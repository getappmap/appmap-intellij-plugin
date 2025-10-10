package appland.deployment;

import appland.AppMapPlugin;
import appland.utils.GsonUtils;
import com.google.gson.JsonParseException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Service providing cached access to the deployment settings.
 * The settings are expected to never change during the lifetime of the plugin.
 * For tests, the settings can be reset.
 */
@Service(Service.Level.APP)
public final class AppMapDeploymentSettingsService {
    public static final String SITE_CONFIG_FILENAME = "site-config.json";

    private volatile @Nullable AppMapDeploymentSettings cachedDeploymentSettings = null;

    public static @NotNull AppMapDeploymentSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(AppMapDeploymentSettingsService.class);
    }

    /**
     * @return The deployment settings.
     * If there are no deployment settings, then an empty settings instance with defaults is returned.
     */
    public static @NotNull AppMapDeploymentSettings getCachedDeploymentSettings() {
        return getInstance().getDeploymentSettings();
    }

    @TestOnly
    public static void reset() {
        getInstance().cachedDeploymentSettings = null;
    }

    /**
     * @return The deployment settings.
     * If there are no deployment settings, then an empty settings instance with defaults is returned.
     */
    private @NotNull AppMapDeploymentSettings getDeploymentSettings() {
        var settings = this.cachedDeploymentSettings;
        if (settings == null) {
            var newSettings = readDeploymentSettings();
            settings = newSettings != null ? newSettings : new AppMapDeploymentSettings();
            this.cachedDeploymentSettings = settings;
        }
        return settings;
    }

    /**
     * @return Paths where the deployment settings file is searched.
     */
    public static @NotNull Collection<Path> deploymentSettingsFileSearchPath() {
        return List.of(
                AppMapPlugin.getPluginPath().resolve(SITE_CONFIG_FILENAME),
                AppMapPlugin.getPluginPath().resolve("extension").resolve(SITE_CONFIG_FILENAME)
        );
    }

    /**
     * Reads the deployment settings from the plugin distribution.
     * They're read every time this method is called.
     *
     * @return The deployment settings bundled with the plugin distribution.
     * The JSON file is first searched at <code>$pluginPath/site-config.json</code> and then at <code>$pluginPath/extension/site-config.json</code>.
     * The first file found is read for the deployment settings.
     */
    static @Nullable AppMapDeploymentSettings readDeploymentSettings() {
        for (var filePath : deploymentSettingsFileSearchPath()) {
            var deploymentSettings = readDeploymentSettings(filePath);
            if (deploymentSettings != null) {
                return deploymentSettings;
            }
        }
        return null;
    }

    /**
     * Package-visible to support testing.
     *
     * @return The deployment settings from the given file, or {@code null} if the file does not exist or cannot be parsed as JSON.
     */
    static @Nullable AppMapDeploymentSettings readDeploymentSettings(@NotNull Path path) {
        if (Files.isReadable(path) && !Files.isDirectory(path)) {
            try {
                return GsonUtils.GSON.fromJson(Files.readString(path), AppMapDeploymentSettings.class);
            } catch (IOException e) {
                Logger.getInstance(AppMapPlugin.class).error("Failed to read deployment configuration file " + path, e);
            } catch (JsonParseException e) {
                Logger.getInstance(AppMapPlugin.class).error("Failed to parse deployment configuration file " + path, e);
            }
        }
        return null;
    }
}
