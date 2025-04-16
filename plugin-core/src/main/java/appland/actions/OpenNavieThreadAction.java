package appland.actions;

import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import appland.rpcService.NavieThreadQueryV1Params;
import appland.rpcService.NavieThreadQueryV1Response;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Action to list existing Navie threads and open one.
 */
public class OpenNavieThreadAction extends AnAction implements DumbAware {
    // maximum number of historical threads to retrieve
    private static final int THREAD_QUERY_LIMIT = 64;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        var service = AppLandJsonRpcService.getInstance(project);
        Integer port = service.getServerPort();
        if (port == null) {
            AppMapNotifications.showNavieUnavailableNotification(project);
            return;
        }

        // Query existing threads via JSON-RPC service
        List<NavieThreadQueryV1Response.NavieThread> threads;
        try {
            threads = AppLandJsonRpcService.getInstance(project)
                    .queryNavieThreads(
                            new NavieThreadQueryV1Params(
                                    null,
                                    null,
                                    "updated_at",
                                    THREAD_QUERY_LIMIT,
                                    null,
                                    null
                            )
                    );
        } catch (IOException ex) {
            AppMapNotifications.showNavieUnavailableNotification(project);
            return;
        }

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(
            new BaseListPopupStep<NavieThreadQueryV1Response.NavieThread>("Select Navie Thread", threads) {
                @Override
                public PopupStep<?> onChosen(NavieThreadQueryV1Response.NavieThread thread, boolean finalChoice) {
                    NavieEditorProvider.openEditorWithThreadId(project, thread.id());
                    return FINAL_CHOICE;
                }
                @Override
                public String getTextFor(NavieThreadQueryV1Response.NavieThread thread) {
                    var title = thread.title() == null ? "Untitled" : thread.title();
                    return String.format("%s – %s", title, thread.updatedAt());
                }
            }
        );
        popup.showInBestPositionFor(e.getDataContext());
    }
}