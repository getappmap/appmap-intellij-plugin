package appland.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deletes corresponding AppMap index data after a *.appmap.json file was deleted.
 */
public class DeleteAppMapIndexDataFileListener implements AsyncFileListener {
    @Nullable
    @Override
    public ChangeApplier prepareChange(@NotNull List<? extends @NotNull VFileEvent> events) {
        var deletedAppMaps = events.stream()
                .map(DeleteAppMapIndexDataFileListener::findIndexDirectory)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return deletedAppMaps.isEmpty() ? null : new DeleteAppMapIndexDataChangeApplier(deletedAppMaps);
    }

    private static @Nullable VirtualFile findIndexDirectory(VFileEvent event) {
        var file = event.getFile();
        return file != null && event instanceof VFileDeleteEvent && AppMapFiles.isAppMap(file)
                ? AppMapFiles.findAppMapMetadataDirectory(file)
                : null;
    }

    private static class DeleteAppMapIndexDataChangeApplier implements ChangeApplier {
        private final @NotNull List<VirtualFile> appMapIndexDirectories;

        public DeleteAppMapIndexDataChangeApplier(@NotNull List<VirtualFile> appMapIndexDirectories) {
            this.appMapIndexDirectories = appMapIndexDirectories;
        }

        @Override
        public void afterVfsChange() {
            var application = ApplicationManager.getApplication();
            application.invokeLater(() -> {
                CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                    application.runWriteAction(() -> {
                        appMapIndexDirectories.forEach(DeleteAppMapIndexDataChangeApplier::safeDeleteDirectory);
                    });
                });
            }, ModalityState.defaultModalityState());
        }

        private static void safeDeleteDirectory(@NotNull VirtualFile indexDirectory) {
            if (!indexDirectory.isValid() || !indexDirectory.isDirectory()) {
                return;
            }

            try {
                indexDirectory.delete(DeleteAppMapIndexDataFileListener.class);
            } catch (Exception e) {
                var logger = Logger.getInstance(DeleteAppMapIndexDataChangeApplier.class);
                logger.warn("Exception deleting AppMap index directory " + indexDirectory.getPath());
            }
        }
    }
}
