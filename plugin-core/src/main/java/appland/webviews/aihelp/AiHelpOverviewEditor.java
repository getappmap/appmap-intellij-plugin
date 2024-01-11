package appland.webviews.aihelp;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.settings.AppMapApplicationSettingsService;
import appland.webviews.WebviewEditor;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Set;

public class AiHelpOverviewEditor extends WebviewEditor<Void> {
    public AiHelpOverviewEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file, Set.of());
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getAiHelpHTMLPath();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.aiHelp.title");
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        var apiKey = AppMapApplicationSettingsService.getInstance().getApiKey();
        payload.addProperty("apiKey", apiKey);
    }

    @Override
    protected void afterInit(@Nullable Void initData) {
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
    }

    protected @Nullable Void createInitData() {
        return null;
    }
}
