package appland.webviews.appMap;

import appland.AppMapBundle;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Navie is a split view of AI and AppMap rendering.
 * To support the AppMap view, we're extending from the base class supporting AppMap viewers.
 */
public class NavieEditor extends AbstractAppMapFileView<Void> {
    public NavieEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.Navie, file);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.navie.title");
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        var apiKey = AppMapApplicationSettingsService.getInstance().getApiKey();
        var filters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var question = NavieEditorProvider.KEY_QUESTION_TEXT.get(file);

        var port = NavieEditorProvider.KEY_INDEXER_RPC_PORT.get(file);
        assert port != null;

        payload.addProperty("appmapRpcPort", port);
        payload.addProperty("apiKey", StringUtil.defaultIfEmpty(apiKey, ""));
        payload.addProperty("question", StringUtil.defaultIfEmpty(question, ""));
        payload.add("savedFilters", gson.toJsonTree(filters));
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception {
        handleAppMapBaseMessage(messageId, message);
    }

    protected @Nullable Void createInitData() {
        return null;
    }
}