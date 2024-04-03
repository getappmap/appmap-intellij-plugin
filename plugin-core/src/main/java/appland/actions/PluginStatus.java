package appland.actions;

import appland.cli.AppLandDownloadService;
import appland.cli.CliTool;
import appland.javaAgent.JavaAgentStatus;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;


public class PluginStatus extends AnAction implements UpdateInBackground, DumbAware {
    private static final Logger LOG = Logger.getInstance(PluginStatus.class);
    private static final String STATUS_REPORT_FILENAME = "appmap_status_report.md";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        createStatusReport(Objects.requireNonNull(event.getProject()));
    }

    @RequiresBackgroundThread
    public static void createStatusReport(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) return;

        new Task.Backgroundable(project, "Generating AppMap Plugin status report...",  true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {

            private volatile VirtualFile statusReportFile;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                String reportText = statusReportText(indicator);

                statusReportFile = WriteAction.computeAndWait(() -> {
                    try {
                        var file = projectDir.createChildData(this, STATUS_REPORT_FILENAME);
                        VfsUtil.saveText(file, reportText);
                        return file;
                    } catch (IOException e) {
                        LOG.error(e);
                        return null;
                    }
                });
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
    private static String appmapBinaryStatusReport(CliTool type) {
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
        downloadedVersionText += downloadedVersion == null ? "" : downloadedVersion;
        downloadedVersionText += downloadPath == null ? "" : String.format("\n\nDownload location: %s", downloadPath);

        return getBinaryReportHeader(type) + downloadedVersionText + "\n\n\n" + latestVersionText + "\n\n";
    }

    @Nullable
    private static String getBinaryReportHeader(CliTool type) {
        if (type == CliTool.AppMap) {
            return "### AppMap CLI Status\n\n";
        } else if (type == CliTool.Scanner) {
            return "### AppMap Scanner Status\n\n";
        }

        return null;
    }
}
