package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class StartServicesAfterDownloadListener implements AppLandDownloadListener {
    @Override
    public void downloadFinished(@NotNull CliTool type, boolean success) {
        if (!success) {
            return;
        }

        var service = AppLandDownloadService.getInstance();
        if (service.isDownloaded(CliTool.AppMap) && service.isDownloaded(CliTool.Scanner)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                AppLandCommandLineService.getInstance().refreshForOpenProjects();
            });
        }
    }
}
