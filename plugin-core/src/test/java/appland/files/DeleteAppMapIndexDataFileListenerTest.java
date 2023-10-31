package appland.files;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DeleteAppMapIndexDataFileListenerTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        // we need to switch between EDT and non-EDT threads to make this test work
        return false;
    }

    @Test
    public void recursiveIndexDirectoryRemoval() throws InterruptedException {
        var root = WriteAction.computeAndWait(() -> myFixture.copyDirectoryToProject("projects/with_modification_date", "root"));
        var appMap = root.findFileByRelativePath("tmp/appmap/with_modification_date.appmap.json");
        assertNotNull(appMap);

        var indexDirectory = ReadAction.compute(() -> root.findFileByRelativePath("tmp/appmap/with_modification_date"));
        assertNotNull("Index directory must exist", indexDirectory);

        var deletedLatch = listenForDirectoryDeletion(indexDirectory);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            try {
                appMap.delete(this);
            } catch (IOException e) {
                addSuppressedException(e);
            }
        });

        var wasDeleted = deletedLatch.await(10, TimeUnit.SECONDS);
        assertTrue("The index directory must be deleted when an AppMap is deleted", wasDeleted);
        assertNull("Index directory must not exist after AppMap was deleted",
                root.findFileByRelativePath("tmp/appmap/with_modification_date"));
    }

    @NotNull
    private CountDownLatch listenForDirectoryDeletion(VirtualFile indexDirectory) {
        var indexDirLatch = new CountDownLatch(1);
        indexDirectory.getFileSystem().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void fileDeleted(@NotNull VirtualFileEvent event) {
                if (indexDirectory.equals(event.getFile())) {
                    indexDirLatch.countDown();
                }
            }
        }, getTestRootDisposable());
        return indexDirLatch;
    }
}