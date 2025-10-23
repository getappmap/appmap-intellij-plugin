package appland.webviews.findings;

import appland.AppMapBundle;
import appland.problemsView.FindingsManager;
import appland.problemsView.FindingsViewTab;
import appland.problemsView.model.ScannerFinding;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.WebviewEditorException;
import appland.webviews.findingDetails.FindingDetailsEditorProvider;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class FindingsOverviewEditor extends WebviewEditor<List<ScannerFinding>> {
    public FindingsOverviewEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.Findings, file, Set.of("open-problems-tab", "open-finding-info"));
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.findingsOverview.title");
    }

    @Override
    protected void setupInitMessage(@Nullable List<ScannerFinding> initData, @NotNull JsonObject payload) throws WebviewEditorException {
        assert initData != null;

        payload.addProperty("page", "finding-overview");
        payload.add("data", GsonUtils.singlePropertyObject("findings", createFindingsArray(initData)));
    }

    @Override
    protected void afterInit(@Nullable List<ScannerFinding> initData) {
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
        switch (messageId) {
            case "open-problems-tab":
                ApplicationManager.getApplication().invokeLater(() -> FindingsViewTab.activateFindingsTab(project));
                break;

            case "open-finding-info":
                // locate finding by hash, then open it in a new AppMap webview
                if (message != null && message.has("hash")) {
                    openFindingsByHash(message.getAsJsonPrimitive("hash").getAsString());
                }
                break;
        }
    }

    protected @NotNull List<ScannerFinding> createInitData() throws WebviewEditorException {
        return FindingsManager.getInstance(project).getAllFindings();
    }

    private @NotNull JsonArray createFindingsArray(@NotNull List<ScannerFinding> initData) {
        var findingsJson = new JsonArray();
        for (var finding : initData) {
            findingsJson.add(createFindingsJsonItem(finding));
        }
        return findingsJson;
    }

    private void openFindingsByHash(@NotNull String hash) {
        var findings = FindingsManager.getInstance(project).findFindingsByHash(hash);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (findings.isEmpty()) {
                var message = AppMapBundle.get("findingsOverview.openFindingsByHashError.message", hash);
                var title = AppMapBundle.get("findingsOverview.openFindingsByHashError.title");
                Messages.showErrorDialog(message, title);
            } else {
                FindingDetailsEditorProvider.openEditor(project, hash, findings);
            }
        });
    }

    private static @NotNull JsonObject createFindingsJsonItem(@NotNull ScannerFinding finding) {
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
}
