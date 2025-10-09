package appland.deployment;

import appland.AppMapPlugin;
import appland.cli.CliTool;
import appland.utils.GsonUtils;
import com.google.gson.JsonParseException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service providing cached access to the deployment settings.
 * The settings are expected to never change during the lifetime of the plugin.
 * For tests, the settings can be reset.
 */
@Service(Service.Level.APP)
public final class AppMapDeploymentSettingsService {
    public static final String SITE_CONFIG_FILENAME = "site-config.json";
    private static final Logger LOG = Logger.getInstance(AppMapDeploymentSettingsService.class);

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
     * @return The directories where bundled binaries are searched.
     */
    public static @NotNull List<Path> bundledBinarySearchPath() {
        return List.of(AppMapPlugin.getPluginPath().resolve("resources"));
    }

    /**
     * Locates all bundled binaries of the given type at resources/ and extension/resources/ of the plugin distribution.
     * Results of all locations are combined.
     *
     * @param type     The type of the binary.
     * @param platform The platform to search for.
     * @param arch     The architecture to search for.
     * @return The bundled binaries of the given type. An empty list is returned if no binaries are found.
     * If more than one binary of the given type exists, the latest one is returned.
     */
    public @NotNull Stream<Path> findBundledBinaries(@NotNull CliTool type, String platform, String arch) {
        return bundledBinarySearchPath()
                .stream()
                .flatMap(searchPath -> findBundledBinaries(searchPath, type, platform, arch).stream());
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
        return List.of(AppMapPlugin.getPluginPath().resolve(SITE_CONFIG_FILENAME));
    }

    /**
     * Find the bundled binaries of the given type in the given directory.
     * Package-visible for tests.
     */
    static @NotNull List<Path> findBundledBinaries(@NotNull Path parentDirectory,
                                                   @NotNull CliTool type,
                                                   @NotNull String platform,
                                                   @NotNull String arch) {
        if (!Files.isDirectory(parentDirectory)) {
            return Collections.emptyList();
        }

        try (var fileStream = Files.list(parentDirectory)) {
            // e.g. appmap-linux-x64-0.9.0.exe -> appmap-linux-x64-0.9.0
            return fileStream.filter(file -> {
                if (!Files.isRegularFile(file)) {
                    return false;
                }

                // e.g. appmap-windows-x64-0.9.0.exe -> appmap-windows-x64-0.9.0
                var baseFilename = StringUtil.trimEnd(file.getFileName().toString(), ".exe");
                return baseFilename.startsWith(StringUtil.trimEnd(type.getBinaryName(platform, arch), ".exe"));
            }).toList();
        } catch (IOException e) {
            var logger = Logger.getInstance(AppMapDeploymentSettingsService.class);
            logger.debug("Error finding latest bundled binary for " + type + " in directory " + parentDirectory, e);
            return Collections.emptyList();
        }
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
        LOG.debug("Attempting to read deployment settings at " + path);

        if (Files.isReadable(path) && !Files.isDirectory(path)) {
            LOG.debug("Found deployment settings file at " + path);
            try {
                return GsonUtils.GSON.fromJson(Files.readString(path), AppMapDeploymentSettings.class);
            } catch (IOException e) {
                Logger.getInstance(AppMapPlugin.class).error("Failed to read deployment configuration file " + path, e);
            } catch (JsonParseException e) {
                Logger.getInstance(AppMapPlugin.class).error("Failed to parse deployment configuration file " + path, e);
            }
        } else {
            LOG.debug("Deployment settings file not found, not readable or not a regular file: " + path);
        }

        return null;
    }
}
