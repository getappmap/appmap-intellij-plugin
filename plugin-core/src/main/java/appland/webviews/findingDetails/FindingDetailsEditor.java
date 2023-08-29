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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

import static appland.utils.GsonUtils.singlePropertyObject;

public class FindingDetailsEditor extends WebviewEditor<Void> {
    public FindingDetailsEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file, Set.of("open-findings-overview", "open-in-source-code", "open-map"));
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
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception {
        switch (messageId) {
            case "open-findings-overview":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FindingsOverviewEditorProvider.openEditor(project);
                }, ModalityState.defaultModalityState());
                break;

            case "open-in-source-code":
                assert message != null;
                openEditorForLocation(gson.fromJson(message.getAsJsonObject("location"), ResolvedStackLocation.class));
                break;

            case "open-map":
                // file path to the AppMap is at uri > path
                assert message != null;
                var path = GsonUtils.getPath(message, "uri", "path");
                if (path != null) {
                    var fragment = GsonUtils.getPath(message, "uri", "fragment");
                    openAppMapUri(path.getAsString(), fragment != null ? fragment.getAsString() : "{}");
                }
                break;
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
        payload.add("data", singlePropertyObject("findings", FindingsUtil.createFindingsArray(gson, project, findings, "ruleInfo")));
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

    private void openAppMapUri(@NotNull String appMapUri, @NotNull String jsonState) {
        var file = LocalFileSystem.getInstance().findFileByPath(StringUtil.split(appMapUri, "#").get(0));
        if (file != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                var appMapEditors = FileEditorManager.getInstance(project).openFile(file, true);
                if (appMapEditors.length == 1 && appMapEditors[0] instanceof AppMapFileEditor) {
                    ((AppMapFileEditor) appMapEditors[0]).setWebViewState(AppMapFileEditorState.of(jsonState));
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
