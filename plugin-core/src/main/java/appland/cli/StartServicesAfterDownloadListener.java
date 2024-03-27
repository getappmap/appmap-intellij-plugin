package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class StartServicesAfterDownloadListener implements AppLandDownloadListener {
    @Override
    public void downloadFinished(@NotNull CliTool type, boolean success) {
        if (!success) {
            return;
        }

        // in tests, we don't want async process starts after the CLI download finished
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        var service = AppLandDownloadService.getInstance();
        if (service.isDownloaded(CliTool.AppMap) && service.isDownloaded(CliTool.Scanner)) {
            AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
        }
    }
}
