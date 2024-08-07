package appland.cli;

import appland.config.AppMapConfigFileListener;

/**
 * Listener to refresh the open projects, which is enabled in both production and test modes.
 * Service {@link DefaultCommandLineService} is not loaded by default in tests.
 */
public class AppMapCommandLineConfigListener implements AppMapConfigFileListener {
    @Override
    public void refreshAppMapConfigs() {
        AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
    }
}