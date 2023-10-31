package appland.toolwindow.appmap;

import appland.AppMapBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DeleteProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.actions.VirtualFileDeleteProvider;
import org.jetbrains.annotations.NonNls;
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
        var enabled = deleteHandler.canDeleteElement(new CustomizedDataContext(e.getDataContext()));
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Pass a customized context to the delete handler to delete all Appand not just the array of selected AppMaps
        deleteHandler.deleteElement(new CustomizedDataContext(e.getDataContext()));
    }

    /**
     * Customized context to provide the value of {@link AppMapWindowPanel#KEY_ALL_APPMAPS}
     * when the delete implementation requests files to delete via {@link CommonDataKeys#VIRTUAL_FILE_ARRAY}.
     */
    private static class CustomizedDataContext extends DataContextWrapper {
        public CustomizedDataContext(@NotNull DataContext delegate) {
            super(delegate);
        }

        @Override
        public @Nullable Object getData(@NotNull @NonNls String dataId) {
            if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
                return super.getData(AppMapWindowPanel.KEY_ALL_APPMAPS);
            }
            return super.getData(dataId);
        }
    }
}
