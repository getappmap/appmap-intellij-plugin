package appland.webviews.navie;

import appland.AppMapBundle;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class NavieEditor extends WebviewEditor<Void> {
    public NavieEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.Navie, file, Set.of());
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
    protected void afterInit(@Nullable Void initData) {
        project.getMessageBus().connect(this).subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void appMapWebViewFiltersChanged() {
                applyWebViewFilters();
            }
        });
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
    }

    protected @Nullable Void createInitData() {
        return null;
    }

    /**
     * Update the AppMap filters in the AppLand JS application.
     */
    private void applyWebViewFilters() {
        var savedFilters = AppMapProjectSettingsService.getState(project).getAppMapFilters().values();
        var message = createMessageObject("updateSavedFilters");
        message.add("data", GsonUtils.GSON.toJsonTree(savedFilters));

        postMessage(message);
    }
}