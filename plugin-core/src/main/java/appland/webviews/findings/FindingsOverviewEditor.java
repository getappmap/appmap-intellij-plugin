package appland.webviews.findings;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.problemsView.FindingsManager;
import appland.problemsView.FindingsViewTab;
import appland.problemsView.model.ScannerFinding;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.findingDetails.FindingDetailsEditorProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class FindingsOverviewEditor extends WebviewEditor {
    public FindingsOverviewEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file);
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getFindingsAppHTMLPath();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.findingsOverview.title");
    }

    @Override
    protected void setupInitMessage(@NotNull JsonObject payload) {
        payload.addProperty("page", "finding-overview");
        payload.add("data", GsonUtils.singlePropertyObject("findings", createFindingsArray()));
    }

    @Override
    public @Nullable JBCefJSQuery.Response handleWebviewMessage(@NotNull String messageId, @Nullable JsonObject message) {
        switch (messageId) {
            case "open-problems-tab":
                ApplicationManager.getApplication().invokeLater(() -> FindingsViewTab.activateFindingsTab(project));
                return new JBCefJSQuery.Response("success");

            case "open-finding-info":
                // locate finding by hash, then open it in a new AppMap webview
                if (message != null && message.has("hash")) {
                    openFindingsByHash(message.getAsJsonPrimitive("hash").getAsString());
                }
                return new JBCefJSQuery.Response("processed " + messageId);

            default:
                return null;
        }
    }

    private @NotNull JsonArray createFindingsArray() {
        var findingsJson = new JsonArray();
        for (var finding : FindingsManager.getInstance(project).getAllFindings()) {
            findingsJson.add(createFindingsJsonItem(finding));
        }
        return findingsJson;
    }

    private @NotNull JsonObject createFindingsJsonItem(ScannerFinding finding) {
        var jsonItem = new JsonObject();
        jsonItem.addProperty("ruleTitle", finding.ruleTitle);
        jsonItem.addProperty("hash_v2", finding.getAppMapHashWithFallback());
        var domain = finding.impactDomain;
        if (domain != null) {
            jsonItem.addProperty("impactDomain", domain.getJsonId());
        }

        var wrapper = new JsonObject();
        wrapper.add("finding", jsonItem);
        return wrapper;
    }

    private void openFindingsByHash(@NotNull String hash) {
        var findings = FindingsManager.getInstance(project).findProblemByHash(hash);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (findings.isEmpty()) {
                Messages.showErrorDialog("Unable to locate AppMap with ID", "Error Locating AppMap");
            } else {
                FindingDetailsEditorProvider.openEditor(project, findings);
            }
        });
    }
}
