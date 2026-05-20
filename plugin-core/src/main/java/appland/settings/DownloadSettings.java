package appland.settings;

import appland.cli.CliTool;
import appland.deployment.AppMapDeploymentSettingsService;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public final class DownloadSettings {
    public static final String DEFAULT_APPMAP_MANIFEST_URL = "https://raw.githubusercontent.com/getappmap/appmap-js/release-manifests/appmap-latest.json";
    public static final String DEFAULT_SCANNER_MANIFEST_URL = "https://raw.githubusercontent.com/getappmap/appmap-js/release-manifests/scanner-latest.json";

    private DownloadSettings() {
    }

    /**
     * Returns whether the download of AppMap assets is enabled.
     *
     * @return {@code true} if the download is enabled, {@code false} otherwise.
     */
    public static boolean isAssetDownloadEnabled() {
        var userOverride = AppMapApplicationSettingsService.getInstance().getAutoUpdateTools();
        return userOverride != null
                ? userOverride
                : AppMapDeploymentSettingsService.getCachedDeploymentSettings().isAutoUpdateTools();
    }

    public static boolean isAssetDownloadDisabled() {
        return !isAssetDownloadEnabled();
    }

    public static @NotNull String getManifestUrl(@NotNull CliTool type) {
        var appSettings = AppMapApplicationSettingsService.getInstance();
        var deploymentSettings = AppMapDeploymentSettingsService.getCachedDeploymentSettings();

        if (type == CliTool.AppMap) {
            var url = appSettings.getAppmapManifestUrl();
            if (!StringUtil.isEmptyOrSpaces(url)) return url;
            
            url = deploymentSettings.getAppmapManifestUrl();
            if (!StringUtil.isEmptyOrSpaces(url)) return url;
            
            return DEFAULT_APPMAP_MANIFEST_URL;
        } else if (type == CliTool.Scanner) {
            var url = appSettings.getScannerManifestUrl();
            if (!StringUtil.isEmptyOrSpaces(url)) return url;
            
            url = deploymentSettings.getScannerManifestUrl();
            if (!StringUtil.isEmptyOrSpaces(url)) return url;
            
            return DEFAULT_SCANNER_MANIFEST_URL;
        }

        throw new IllegalArgumentException("Unsupported CLI tool: " + type);
    }
}
