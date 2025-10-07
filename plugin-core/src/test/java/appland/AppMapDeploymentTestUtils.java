package appland;

import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.utils.GsonUtils;
import com.intellij.util.ThrowableConsumer;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities to test with a specific AppMap deployment setup.
 */
public final class AppMapDeploymentTestUtils {
    private AppMapDeploymentTestUtils() {
    }

    /**
     * Runs the given runnable with the specific AppMap deployment settings stored at the default location.
     *
     * @param newSettings The new settings to apply.
     * @param runnable    The runnable to run with the new deployment settings.
     */
    public static void withSiteConfigFile(@NotNull AppMapDeploymentSettings newSettings,
                                          @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        withSiteConfigFile(AppMapPlugin.getPluginPath(), GsonUtils.GSON.toJson(newSettings), path -> runnable.run());
    }

    public static void withSiteConfigFile(@NotNull Path parentDirectory,
                                          @NotNull String fileContent,
                                          @NotNull ThrowableConsumer<Path, Exception> runnable) throws Exception {
        Files.createDirectories(parentDirectory);

        var jsonFile = parentDirectory.resolve(AppMapDeploymentSettingsService.SITE_CONFIG_FILENAME);
        try {
            AppMapDeploymentSettingsService.reset();

            Files.writeString(jsonFile, fileContent);
            runnable.consume(jsonFile);
        } finally {
            // we have to delete it because it would have side-effects on other tests
            Files.deleteIfExists(jsonFile);

            AppMapDeploymentSettingsService.reset();
        }
    }
}
