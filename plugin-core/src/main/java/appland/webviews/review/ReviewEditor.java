
package appland.webviews.review;

import appland.rpcService.AppLandJsonRpcService;
import appland.webviews.WebviewEditor;
import appland.webviews.navie.NavieEditorProvider;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import org.jetbrains.annotations.Nls;

public class ReviewEditor extends WebviewEditor<Void> {
    public ReviewEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, AppMapWebview.Review, file, Set.of("open-location", "show-navie-thread"));
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "Review";
    }

    @Override
    protected void setupInitMessage(@Nullable Void initData, @NotNull JsonObject payload) {
        var port = AppLandJsonRpcService.getInstance(project).getServerPort();
        payload.addProperty("rpcPort", port);
        var baseRef = ReviewEditorProvider.KEY_BASE_REF.get(getFile());
        if (baseRef != null) {
            payload.addProperty("baseRef", baseRef);
        }
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
        switch (messageId) {
            case "open-location":
                if (message != null && message.has("args")) {
                    var args = message.getAsJsonArray("args");
                    if (args.isEmpty()) return;

                    var location = args.get(0).getAsString();
                    handleOpenLocation(location, null);
                }
                break;

            case "show-navie-thread":
                if (message != null && message.has("args")) {
                    var args = message.getAsJsonArray("args");
                    if (args.isEmpty()) return;
                    var threadId = args.get(0).getAsString();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NavieEditorProvider.openEditorWithThreadId(project, threadId);
                    }, ModalityState.defaultModalityState());
                }
                break;
        }
    }

    @Override
    protected void afterInit(@Nullable Void initData) {
    }

    @Override
    protected @Nullable Void createInitData() {
        return null;
    }
}
