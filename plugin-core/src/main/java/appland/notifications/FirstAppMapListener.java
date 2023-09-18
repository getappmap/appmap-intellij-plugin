package appland.notifications;

import appland.files.AppMapFileChangeListener;
import appland.files.AppMapFileEventType;
import appland.index.AppMapSearchScopes;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.ui.EdtInvocationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Displays a notification after the user created the first AppMap ever.
 * The notification is only displayed once for an IDE installation, i.e. it's an application setting.
 */
public class FirstAppMapListener implements AppMapFileChangeListener {
    private final @NotNull Project project;

    public FirstAppMapListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void refreshAppMaps(@NotNull Set<AppMapFileEventType> changeTypes) {
        if (!changeTypes.contains(AppMapFileEventType.Create)) {
            return;
        }

        if (AppMapApplicationSettingsService.getInstance().isShowFirstAppMapNotification()) {
            EdtInvocationManager.invokeLaterIfNeeded(() -> {
                DumbService.getInstance(project).runReadActionInSmartMode(this::showFirstAppMapNotification);
            });
        }
    }

    /**
     * Executed in a read action in smart mode.
     */
    @RequiresReadLock
    private void showFirstAppMapNotification() {
        if (project.isDisposed()) {
            return;
        }

        var settings = AppMapApplicationSettingsService.getInstance();
        if (settings.isShowFirstAppMapNotification()) {
            var chosenAppMap = chooseSuitableAppmap();
            if (chosenAppMap != null) {
                settings.setShowFirstAppMapNotification(false);
                AppMapNotifications.showFirstAppMapNotification(project, chosenAppMap);
            }
        }
    }

    /**
     * Choose a suitable AppMap. It must not use an AppMap index, because AppMaps indexing may be in progress.
     *
     * @return AppMap to open from the notification
     */
    @RequiresReadLock
    private @Nullable VirtualFile chooseSuitableAppmap() {
        var searchScope = AppMapSearchScopes.appMapsWithExcluded(project);
        var files = FilenameIndex.getAllFilesByExt(project, "appmap.json", searchScope);
        return files.isEmpty() ? null : files.stream().findFirst().orElse(null);
    }
}
