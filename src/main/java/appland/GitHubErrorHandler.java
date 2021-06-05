package appland;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Urls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;

/**
 * Simple error handler to create new issues via GitHub URLs.
 * It opens a new browser tab with prefilled content.
 */
public class GitHubErrorHandler extends ErrorReportSubmitter {
    private static final String HOST = "github.com";
    private static final String URL_PATH = "/applandinc/appmap-intellij-plugin/issues/new";

    @Override
    public @NlsActions.ActionText @NotNull String getReportActionText() {
        return AppMapBundle.get("errorReporter.actionText");
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events,
                          @Nullable String additionalInfo,
                          @NotNull Component parentComponent,
                          @NotNull Consumer<? super SubmittedReportInfo> consumer) {

        var mgr = DataManager.getInstance();
        var context = mgr.getDataContext(parentComponent);
        var project = CommonDataKeys.PROJECT.getData(context);

        var stacktrace = "";
        if (events.length >= 1) {
            stacktrace = events[0].getThrowableText();
        }

        var lines = StringUtil.splitByLines(stacktrace);
        var body = createGitHubIssueBody(additionalInfo == null ? "" : additionalInfo, stacktrace);

        var params = new HashMap<String, String>();
        params.put("title", lines.length > 0 ? lines[0] : "Crash report: Fill in title");
        params.put("labels", "user report");
        params.put("body", body);

        var url = Urls.newHttpUrl(HOST, URL_PATH);
        url = url.addParameters(params);

        BrowserUtil.browse(url.toExternalForm());
        consumer.consume(new SubmittedReportInfo(null, "GitHub", SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
        return true;
    }

    private String createGitHubIssueBody(@NotNull String userNotes, @NotNull String stacktrace) {
        var lastAction = IdeaLogger.ourLastActionId;

        var out = new StringBuilder();
        out.append("Name | Value\n");
        out.append("----:|:-----\n");
        out.append("Plugin|").append(AppMapPlugin.getDescriptor().getPluginId()).append("\n");
        out.append("Version|").append(AppMapPlugin.getDescriptor().getVersion()).append("\n");
        out.append("IDE|").append(ApplicationInfo.getInstance().getBuild().asString()).append("\n");
        out.append("OS|").append(SystemInfoRt.OS_NAME).append(" ").append(SystemInfoRt.OS_VERSION).append("\n");
        out.append("Last action|").append(lastAction == null ? "" : "").append("\n");

        out.append("\n");
        out.append("### Description\n");
        out.append(userNotes).append("\n");

        if (!stacktrace.isEmpty()) {
            out.append("\n");
            out.append("### Stacktrace\n");
            out.append("```text\n");
            out.append(stacktrace).append("\n");
            out.append("```\n");
        }

        return out.toString();
    }
}
