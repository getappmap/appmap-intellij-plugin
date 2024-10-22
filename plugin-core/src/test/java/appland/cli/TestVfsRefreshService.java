package appland.cli;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

@TestOnly
public class TestVfsRefreshService extends DefaultVfsRefreshService {
    private static final Topic<Runnable> VFS_REFRESH_TOPIC = Topic.create("appmap.test.vfsRefresh", Runnable.class);

    private final Set<Path> refreshedFiles = ContainerUtil.newConcurrentSet();

    public static @NotNull TestVfsRefreshService getInstance() {
        return (TestVfsRefreshService) VfsRefreshService.getInstance();
    }

    /**
     * Condition to wait for the request to refresh a file path based on STDOUT of the AppMap indexer.
     */
    public static @NotNull CountDownLatch newVfsRefreshCondition(@NotNull Project project,
                                                                 @NotNull Disposable parentDisposable) {
        var latch = new CountDownLatch(1);
        project.getMessageBus().connect(parentDisposable).subscribe(VFS_REFRESH_TOPIC, (Runnable) latch::countDown);
        return latch;
    }

    public @NotNull Set<Path> getRefreshedFiles() {
        return Collections.unmodifiableSet(refreshedFiles);
    }

    @Override
    public void dispose() {
        reset();
        super.dispose();
    }

    public void reset() {
        refreshedFiles.clear();
    }

    @Override
    public void requestVirtualFileRefresh(@NotNull Path path) {
        try {
            refreshedFiles.add(path);
            super.requestVirtualFileRefresh(path);
        } finally {
            ApplicationManager.getApplication().getMessageBus().syncPublisher(VFS_REFRESH_TOPIC).run();
        }
    }
}