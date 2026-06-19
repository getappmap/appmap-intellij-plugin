package appland.utils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Helpers for opening a file chooser without tripping the platform's "slow operations on EDT"
 * assertion.
 * <p>
 * {@link FileChooser#chooseFile} resolves a path via VFS on the EDT (its "restore last selection"
 * logic), which is a slow operation. We resolve a sensible initial directory ourselves on a background
 * thread and pass it as the pre-selected file, which both avoids the assertion and lets us keep the
 * "reopen where I last browsed" behavior. Because resolution happens off the EDT, the chooser is opened
 * asynchronously and the result is delivered to a callback.
 */
public final class AppMapFileChoosers {
    // Platform key used by FileChooserUtil to remember the last browsed location. It's a private
    // platform constant; we read it best-effort and fall back to the project/home directory, so a
    // future platform rename only loses the "last folder" nicety rather than breaking anything.
    private static final String LAST_OPENED_FILE_PATH = "last_opened_file_path";

    private AppMapFileChoosers() {
    }

    /**
     * Opens a single-file chooser and invokes {@code onChosen} on the EDT if a file is selected.
     * Must be called on the EDT.
     */
    public static void chooseFile(@NotNull FileChooserDescriptor descriptor,
                                  @Nullable Project project,
                                  @NotNull Consumer<@NotNull VirtualFile> onChosen) {
        var application = ApplicationManager.getApplication();
        var modality = ModalityState.current(); // captured on the EDT, so the chooser shows within the active modal
        application.executeOnPooledThread(() -> {
            var initialDirectory = resolveInitialDirectory(project);
            application.invokeLater(() -> {
                var file = FileChooser.chooseFile(descriptor, project, initialDirectory);
                if (file != null) {
                    onChosen.accept(file);
                }
            }, modality);
        });
    }

    /**
     * Opens a multi-file chooser and invokes {@code onChosen} on the EDT with the selected files
     * (only when at least one is selected). Must be called on the EDT.
     */
    public static void chooseFiles(@NotNull FileChooserDescriptor descriptor,
                                   @Nullable Project project,
                                   @NotNull Consumer<@NotNull List<VirtualFile>> onChosen) {
        var application = ApplicationManager.getApplication();
        var modality = ModalityState.current(); // captured on the EDT, so the chooser shows within the active modal
        application.executeOnPooledThread(() -> {
            var initialDirectory = resolveInitialDirectory(project);
            application.invokeLater(() -> {
                var files = FileChooser.chooseFiles(descriptor, project, initialDirectory);
                if (files.length > 0) {
                    onChosen.accept(List.of(files));
                }
            }, modality);
        });
    }

    /**
     * Resolves a starting directory for a file chooser: the last browsed location, else the project
     * root, else the user's home. Must be called off the EDT — {@link LocalFileSystem#findFileByPath}
     * is a slow operation.
     */
    static @Nullable VirtualFile resolveInitialDirectory(@Nullable Project project) {
        var localFileSystem = LocalFileSystem.getInstance();

        var properties = project != null ? PropertiesComponent.getInstance(project) : PropertiesComponent.getInstance();
        var lastOpenedPath = properties.getValue(LAST_OPENED_FILE_PATH);
        if (lastOpenedPath != null) {
            var lastOpened = localFileSystem.findFileByPath(lastOpenedPath);
            if (lastOpened != null) {
                // The stored path is the last selected file; point the chooser at its directory.
                var dir = lastOpened.isDirectory() ? lastOpened : lastOpened.getParent();
                if (dir != null) {
                    return dir;
                }
            }
        }

        if (project != null) {
            var basePath = project.getBasePath();
            if (basePath != null) {
                var projectDir = localFileSystem.findFileByPath(basePath);
                if (projectDir != null) {
                    return projectDir;
                }
            }
        }

        var userHome = System.getProperty("user.home");
        return userHome != null ? localFileSystem.findFileByPath(userHome) : null;
    }
}
