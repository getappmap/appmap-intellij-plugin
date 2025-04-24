package appland.actions;

import appland.AppMapBundle;
import appland.notifications.AppMapNotifications;
import appland.rpcService.AppLandJsonRpcService;
import appland.rpcService.NavieThreadQueryV1Params;
import appland.rpcService.NavieThreadQueryV1Response;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.text.StringUtil;
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
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        var port = AppLandJsonRpcService.getInstance(project).getServerPort();
        if (port == null) {
            AppMapNotifications.showNavieUnavailableNotification(project);
            return;
        }

        List<NavieThreadQueryV1Response.NavieThread> threads;
        try {
            threads = loadNavieThreadsInBackground(project);
        } catch (IOException ex) {
            AppMapNotifications.showNavieUnavailableNotification(project);
            return;
        }

        var popup = JBPopupFactory.getInstance().createListPopup(
                new BaseListPopupStep<>(AppMapBundle.get("action.appmap.navie.openThread.popupTitle"), threads) {
                    @Override
                    public PopupStep<?> onChosen(NavieThreadQueryV1Response.NavieThread thread, boolean finalChoice) {
                        NavieEditorProvider.openEditorWithThreadId(project, thread.id());
                        return FINAL_CHOICE;
                    }

                    @Override
                    public @NotNull String getTextFor(NavieThreadQueryV1Response.NavieThread thread) {
                        var title = StringUtil.defaultIfEmpty(thread.title(), AppMapBundle.get("action.appmap.navie.openThread.unknownHistoryItem"));
                        return String.format("%s – %s", title, thread.updatedAt());
                    }
                }
        );
        popup.showCenteredInCurrentWindow(project);
    }

    /**
     * Load Navie threads from the server under modal progress to avoid blocking the UI.
     *
     * @param project Project
     * @return List of Navie threads or {@code null} if the server is unavailable
     * @throws IOException Thrown by the JSON-RPC service
     */
    private static List<NavieThreadQueryV1Response.NavieThread> loadNavieThreadsInBackground(Project project) throws IOException {
        var title = AppMapBundle.get("action.appmap.navie.openThread.loadingThreads");
        return ProgressManager.getInstance().run(new Task.WithResult<List<NavieThreadQueryV1Response.NavieThread>, IOException>(project, title, true) {
            @Override
            protected List<NavieThreadQueryV1Response.NavieThread> compute(@NotNull ProgressIndicator progressIndicator) throws IOException {
                return AppLandJsonRpcService.getInstance(project).queryNavieThreads(
                        new NavieThreadQueryV1Params(null, null, "updated_at", THREAD_QUERY_LIMIT, null, null)
                );
            }
        });
    }
}