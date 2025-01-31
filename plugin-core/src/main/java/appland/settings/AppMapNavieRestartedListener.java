package appland.settings;

import appland.rpcService.AppLandJsonRpcListener;
import appland.webviews.navie.NavieEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Updates open Navie editors before and after the project-wide AppMap JSON-RPC server is restarted.
 */
public class AppMapNavieRestartedListener implements AppLandJsonRpcListener {
    private final @NotNull Project project;

    public AppMapNavieRestartedListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void beforeServerRestart() {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (editor instanceof NavieEditor) {
                ((NavieEditor) editor).notifyJsonRpcServerRestarting();
            }
        }
    }

    @Override
    public void serverConfigurationUpdated(@NotNull Collection<VirtualFile> contentRoots,
                                           @NotNull Collection<VirtualFile> appMapConfigFiles) {
        // The AppMap JSON-RPC server finished initializing,
        // all open Navie editors need to be notified about the restart,
        // either caused by configuration change or unexpected termination.
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (editor instanceof NavieEditor) {
                ((NavieEditor) editor).notifyJsonRpcServerRestarted();
            }
        }
    }
}
