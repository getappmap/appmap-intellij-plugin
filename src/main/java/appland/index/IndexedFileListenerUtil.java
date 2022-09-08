package appland.index;

import appland.files.AppMapFileChangeListener;
import appland.problemsView.listener.ScannerFindingsListener;
import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import org.jetbrains.annotations.NotNull;

public final class IndexedFileListenerUtil {
    private IndexedFileListenerUtil() {
    }

    public static void registerListeners(@NotNull Project project, @NotNull Disposable parent,
                                         boolean appMapFileListener, boolean findingsFileListener,
                                         @NotNull Runnable action) {

        var busConnection = project.getMessageBus().connect(parent);
        busConnection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void exitDumbMode() {
                if (!DumbService.isDumb(project)) {
                    ApplicationManager.getApplication().invokeLater(action);
                }
            }
        });

        busConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                ApplicationManager.getApplication().invokeLater(action);
            }
        });

        if (appMapFileListener) {
            busConnection.subscribe(AppMapFileChangeListener.TOPIC, () -> {
                ApplicationManager.getApplication().invokeLater(action);
            });
        }

        if (findingsFileListener) {
            busConnection.subscribe(ScannerFindingsListener.TOPIC, () -> {
                ApplicationManager.getApplication().invokeLater(action);
            });
        }
    }
}
