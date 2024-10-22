package appland.cli;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultVfsRefreshService implements VfsRefreshService, Disposable {
    // Cumulate requests for filesystem refreshes of the last 5s.
    // A new requests during the 5s delay restarts the delay.
    private final @NotNull SingleAlarm refreshAlarm = new SingleAlarm(
            this::debouncedRefresh,
            5_000,
            this,
            Alarm.ThreadToUse.POOLED_THREAD,
            null);
    // The Set<Path> value must be thread-safe.
    private final @NotNull AtomicReference<@NotNull Set<Path>> pendingRefresh = new AtomicReference<>(new CopyOnWriteArraySet<>());

    @Override
    public void dispose() {
        pendingRefresh.getAndSet(Set.of());
    }

    @Override
    public void requestVirtualFileRefresh(@NotNull Path path) {
        pendingRefresh.get().add(path);
        refreshAlarm.cancelAndRequest();
    }

    private void debouncedRefresh() {
        var pending = pendingRefresh.getAndSet(new CopyOnWriteArraySet<>());
        if (pending.isEmpty()) {
            return;
        }

        var fs = LocalFileSystem.getInstance();
        var pendingVirtualFiles = ContainerUtil.mapNotNull(pending, fs::refreshAndFindFileByNioFile);
        if (!pendingVirtualFiles.isEmpty()) {
            VfsUtil.markDirtyAndRefresh(true, false, false, pendingVirtualFiles.toArray(new VirtualFile[0]));
        }
    }
}
