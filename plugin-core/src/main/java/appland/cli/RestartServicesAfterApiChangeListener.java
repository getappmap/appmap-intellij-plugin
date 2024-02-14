package appland.cli;

import appland.settings.AppMapSettingsListener;

/**
 * The CLI processes are launched with the currently configured API key.
 * We need to restart them after a user signed in or signed out.
 */
public class RestartServicesAfterApiChangeListener implements AppMapSettingsListener {
    @Override
    public void apiKeyChanged() {
        AppLandCommandLineService.getInstance().restartProcessesInBackground();
    }
}
