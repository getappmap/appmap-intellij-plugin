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
public class TestCommandLineService extends DefaultCommandLineService {
    private static final Topic<Runnable> VFS_REFRESH_TOPIC = Topic.create("appmap.test.vfsRefresh", Runnable.class);

    private final Set<Path> refreshedFiles = ContainerUtil.newConcurrentSet();

    /**
     * Condition to wait for the request to refresh a file path based on STDOUT of the AppMap indexer.
     */
    public static @NotNull CountDownLatch newVfsRefreshCondition(@NotNull Project project,
                                                                 @NotNull Disposable parentDisposable) {
        var latch = new CountDownLatch(1);
        project.getMessageBus().connect(parentDisposable).subscribe(VFS_REFRESH_TOPIC, (Runnable) latch::countDown);
        return latch;
    }

    public static @NotNull TestCommandLineService getInstance() {
        return (TestCommandLineService) AppLandCommandLineService.getInstance();
    }

    public Set<Path> getRefreshedFiles() {
        return Collections.unmodifiableSet(refreshedFiles);
    }

    public void reset() {
        refreshedFiles.clear();
    }

    @Override
    protected void requestVirtualFileRefresh(@NotNull Path filePath) {
        try {
            refreshedFiles.add(filePath);
            super.requestVirtualFileRefresh(filePath);
        } finally {
            ApplicationManager.getApplication().getMessageBus().syncPublisher(VFS_REFRESH_TOPIC).run();
        }
    }
}
