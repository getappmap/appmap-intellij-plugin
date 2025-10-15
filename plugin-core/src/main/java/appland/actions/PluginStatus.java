package appland.actions;

import appland.AppMapBundle;
import appland.cli.AppLandDownloadService;
import appland.cli.CliTool;
import appland.deployment.AppMapDeploymentSettingsService;
import appland.javaAgent.JavaAgentStatus;
import appland.telemetry.SplunkTelemetryUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class PluginStatus extends AnAction implements DumbAware {
    private static final String STATUS_REPORT_FILENAME = "appmap_status_report.md";

    @Override
    @RequiresBackgroundThread
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = Objects.requireNonNull(event.getProject());

        new Task.Backgroundable(project, AppMapBundle.get("action.appmap.pluginStatus.generatingReport"), true) {

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

    /**
     * Generates a detailed status report for the AppMap plugin.
     * <p>
     * This method is package-visible for testing.
     *
     * @param indicator the {@link ProgressIndicator} used to monitor the progress of the status report generation.
     * @return Markdown-formatted status report.
     */
    @NotNull
    static String statusReportText(ProgressIndicator indicator) {
        String header = "# AppMap Plugin Status Report\n\n";
        String deploymentReport = deploymentStatusReport();
        String cliReport = appmapBinaryStatusReport(CliTool.AppMap);
        String scannerReport = appmapBinaryStatusReport(CliTool.Scanner);
        return header + JavaAgentStatus.generateStatusReport(indicator) + deploymentReport + cliReport + scannerReport;
    }

    private static @NotNull String deploymentStatusReport() {
        var output = new StringBuilder();
        output.append("### Deployment Settings\n");

        output.append("Deployment settings search path:\n");
        for (var filePath : AppMapDeploymentSettingsService.deploymentSettingsFileSearchPath()) {
            output.append("- ").append(filePath.toString()).append("\n");
        }
        output.append("\n");

        var settings = AppMapDeploymentSettingsService.getCachedDeploymentSettings();
        if (settings.isEmpty()) {
            output.append("No deployment settings were found.\n");
        } else if (SplunkTelemetryUtils.isSplunkTelemetryEnabled(settings)) {
            var telemetrySettings = Objects.requireNonNull(settings.getTelemetry());
            if (SplunkTelemetryUtils.isSplunkTelemetryActive(settings)) {
                output.append("Splunk telemetry is active.\n");
            } else {
                output.append("Splunk telemetry is configured, but inactive.\n");
            }

            output.append("Backend: %s\n".formatted(StringUtil.notNullize(telemetrySettings.getBackend())));
            output.append("URL: %s\n".formatted(telemetrySettings.getUrl()));
            output.append("Token: %s\n".formatted(StringUtil.trimMiddle(StringUtil.notNullize(telemetrySettings.getToken()), 3)));
        } else {
            output.append("Deployment settings were found, but no valid telemetry settings were found.\n");
        }
        output.append("\n\n");
        return output.toString();
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

    private static @NotNull String getBinaryReportHeader(CliTool type) {
        return switch (type) {
            case AppMap -> "### AppMap CLI Status\n\n";
            case Scanner -> "### AppMap Scanner Status\n\n";
        };
    }
}
