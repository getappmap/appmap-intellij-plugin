package appland.toolwindow.appmap;

import appland.AppMapBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DeleteProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileChooser.actions.VirtualFileDeleteProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Action to delete all AppMaps displayed in the AppMap panel.
 */
final class DeleteAllMapsAction extends AnAction {
    private final DeleteProvider deleteHandler = new VirtualFileDeleteProvider();

    public DeleteAllMapsAction() {
        super(AppMapBundle.get("toolwindow.appmap.actions.deleteAllAppMaps.title"), null, AllIcons.General.Remove);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var enabled = deleteHandler.canDeleteElement(createDataContext(e));
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Pass a customized context to the delete handler to delete all AppMaps, not just the array of selected AppMaps
        deleteHandler.deleteElement(createDataContext(e));
    }

    private static @NotNull DataContext createDataContext(@NotNull AnActionEvent e) {
        var parentContext = e.getDataContext();
        return new DataContext() {
            @Override
            public @Nullable Object getData(@NotNull String dataId) {
                if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
                    return parentContext.getData(AppMapWindowPanel.KEY_ALL_APPMAPS);
                }
                return parentContext.getData(dataId);
            }
        };
    }
}
