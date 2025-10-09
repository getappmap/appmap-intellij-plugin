package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class StartServicesAfterDownloadListener implements AppLandDownloadListener {
    @Override
    public void downloadFinished(@NotNull CliTool type, @NotNull AppMapDownloadStatus status) {
        if (!status.isSuccessful()) {
            return;
        }

        // in tests, we don't want async process starts after the CLI download finished
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        if (CliTools.isBinaryAvailable(CliTool.AppMap) && CliTools.isBinaryAvailable(CliTool.Scanner)) {
            AppLandCommandLineService.getInstance().refreshForOpenProjectsInBackground();
        }
    }
}
