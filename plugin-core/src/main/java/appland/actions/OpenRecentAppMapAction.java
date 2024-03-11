package appland.actions;

import appland.AppMapBundle;
import appland.index.AppMapSearchScopes;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import org.jetbrains.annotations.NotNull;

/**
 * Locates the most recently modified .appmap.json file in the project and opens it.
 * This needs an index and thus can't run when DumbMode is active.
 * <p>
 * If two files have the same modification timestamp, then the returned file is randomly chosen,
 * depending on the order in the index.
 */
public class OpenRecentAppMapAction extends AnAction implements UpdateInBackground {
    private static final Logger LOG = Logger.getInstance("#appmap.action");

    static VirtualFile findMostRecentlyModifiedAppMap(com.intellij.openapi.project.Project project) {
        LOG.debug("Query .appmap.json files...");
        var files = FilenameIndex.getAllFilesByExt(project, "appmap.json", AppMapSearchScopes.appMapsWithExcluded(project));
        LOG.debug("Found .appmap.json files: " + files.size());

        return files.stream().max((a, b) -> {
            var delta = a.getModificationStamp() - b.getModificationStamp();
            if (delta < 0) return -1;
            if (delta > 0) return 1;
            return 0;
        }).orElse(null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        var mostRecent = findMostRecentlyModifiedAppMap(project);
        if (mostRecent == null) {
            var message = AppMapBundle.get("action.showRecentAppmap.notFoundErrorMessage");
            var title = AppMapBundle.get("action.showRecentAppmap.notFoundErrorTitle");
            Messages.showErrorDialog(project, message, title);
            return;
        }

        LOG.debug("Opening most recently modified .appmap.json file: " + mostRecent.getName());
        FileEditorManager.getInstance(project).openFile(mostRecent, true, true);
    }
}
