package appland.webviews.findingDetails;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.files.FileLocation;
import appland.problemsView.FindingsUtil;
import appland.problemsView.ResolvedStackLocation;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.appMap.AppMapFileEditor;
import appland.webviews.appMap.AppMapFileEditorState;
import appland.webviews.findings.FindingsOverviewEditorProvider;
import com.google.gson.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

import static appland.utils.GsonUtils.singlePropertyObject;

public class FindingDetailsEditor extends WebviewEditor<Void> {

    public FindingDetailsEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.findingDetails.title");
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getFindingsAppHTMLPath();
    }

    @Override
    public @Nullable JBCefJSQuery.Response handleWebviewMessage(@NotNull String messageId, @Nullable JsonObject message) {
        switch (messageId) {
            case "open-findings-overview":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FindingsOverviewEditorProvider.openEditor(project);
                }, ModalityState.defaultModalityState());
                return new JBCefJSQuery.Response("success");

            case "open-in-source-code":
                assert message != null;
                openEditorForLocation(gson.fromJson(message.getAsJsonObject("location"), ResolvedStackLocation.class));
                return new JBCefJSQuery.Response("success");

            case "open-map":
                // file path to the AppMap is at uri > path
                assert message != null;
                var path = GsonUtils.getPath(message, "uri", "path");
                if (path != null) {
                    openAppMapUri(path.getAsString());
                }
                return new JBCefJSQuery.Response("success");

            default:
                return null;
        }
    }

    @Override
    protected @Nullable Gson createCustomizedGson() {
        return new GsonBuilder()
                .registerTypeAdapter(FileLocation.class, new FileLocation.TypeAdapter())
                .registerTypeAdapter(ResolvedStackLocation.class, new ResolvedStackLocation.TypeAdapter())
                .create();
    }

    @Override
    protected @Nullable Void createInitData() {
        return null;
    }

    @Override
    protected void afterInit(@Nullable Void initData) {
        // no-op, the editor provider is handling telemetry
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        var findings = FindingDetailsEditorProvider.KEY_FINDINGS.get(file);
        assert findings != null;

        payload.addProperty("page", "finding-details");
        payload.add("data", singlePropertyObject("findings", FindingsUtil.createFindingsArray(gson, project, findings)));
    }

    private void openEditorForLocation(@NotNull ResolvedStackLocation location) {
        var file = LocalFileSystem.getInstance().findFileByPath(location.absolutePath);
        if (file != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                int line = location.line != null ? location.line : 0;
                new OpenFileDescriptor(project, file, line, 0).navigate(true);
            }, ModalityState.defaultModalityState());
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(
                        AppMapBundle.get("webview.findingDetails.locationNotFound.message"),
                        AppMapBundle.get("webview.findingDetails.locationNotFound.title"));
            });
        }
    }

    private void openAppMapUri(@NotNull String appMapUri) {
        // split into file path and state (after the "#)
        var parts = StringUtil.split(appMapUri, "#");
        var file = LocalFileSystem.getInstance().findFileByPath(parts.get(0));
        if (file != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                var appMapEditors = FileEditorManager.getInstance(project).openFile(file, true);
                if (appMapEditors.length == 1 && appMapEditors[0] instanceof AppMapFileEditor) {
                    appMapEditors[0].setState(new AppMapFileEditorState(parts.get(1)));
                }
            }, ModalityState.defaultModalityState());
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(
                        AppMapBundle.get("webview.findingDetails.appMapNotFound.message"),
                        AppMapBundle.get("webview.findingDetails.appMapNotFound.title"));
            });
        }
    }
}
