package appland.settings;

import appland.deployment.AppMapDeploymentSettingsService;

public final class DownloadSettings {
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
}
