package appland.actions;

import appland.AppMapBundle;
import appland.cli.AppLandDownloadService;
import appland.cli.CliTool;
import appland.javaAgent.JavaAgentStatus;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public class PluginStatus extends AnAction implements DumbAware {
    private static final String STATUS_REPORT_FILENAME = "appmap_status_report.md";

    @Override
    @RequiresBackgroundThread
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = Objects.requireNonNull(event.getProject());

        new Task.Backgroundable(project,
                AppMapBundle.get("action.appmap.pluginStatus.generatingReport"),
                true,
                PerformInBackgroundOption.ALWAYS_BACKGROUND) {

            private volatile VirtualFile statusReportFile;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                statusReportFile = new LightVirtualFile(STATUS_REPORT_FILENAME, statusReportText(indicator).trim());
            }

            @Override
            public void onSuccess() {
                if (statusReportFile != null) {
                    var editorManager = FileEditorManager.getInstance(project);
                    editorManager.openFile(statusReportFile, true);
                }
            }
        }.queue();
    }

    @NotNull
    private static String statusReportText(ProgressIndicator indicator) {
        String header = "# AppMap Plugin Status Report\n\n";
        String cliReport = appmapBinaryStatusReport(CliTool.AppMap);
        String scannerReport = appmapBinaryStatusReport(CliTool.Scanner);
        return header + JavaAgentStatus.generateStatusReport(indicator) + cliReport + scannerReport;
    }

    @NotNull
    private static String appmapBinaryStatusReport(@NotNull CliTool type) {
        var service = AppLandDownloadService.getInstance();

        String latestVersion;
        try {
            latestVersion = service.fetchLatestRemoteVersion(type);
        } catch (IOException e) {
            latestVersion = null;
        }

        String latestVersionText = String.format("Latest version: %s",
                latestVersion == null ? "Failed to check for the latest version" : latestVersion);

        var downloadedVersion = service.findLatestDownloadedVersion(type);
        var downloadPath = service.getDownloadFilePath(type);

        String downloadedVersionText = "Your version: ";
        downloadedVersionText += downloadedVersion == null ? "<unavailable>" : downloadedVersion;
        downloadedVersionText += downloadPath == null ? "<unavailable>" : String.format("\n\nDownload location: %s", downloadPath);

        return getBinaryReportHeader(type) + latestVersionText + "\n\n" + downloadedVersionText + "\n\n";
    }

    @Nullable
    private static String getBinaryReportHeader(CliTool type) {
        switch (type) {
            case AppMap:
                return "### AppMap CLI Status\n\n";
            case Scanner:
                return "### AppMap Scanner Status\n\n";
            default:
                return null;
        }
    }
}
