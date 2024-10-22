package appland.cli;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Service interface to allow testing of our Vfs refresh handing of AppMap data.
 * Requests for a filesystem refresh must be debounced by the service implementation.
 */
public interface VfsRefreshService {
    static @NotNull VfsRefreshService getInstance() {
        return ApplicationManager.getApplication().getService(VfsRefreshService.class);
    }

    /**
     * Request a refresh of the given path.
     *
     * @param path File or directory to refresh.
     */
    void requestVirtualFileRefresh(@NotNull Path path);
}
