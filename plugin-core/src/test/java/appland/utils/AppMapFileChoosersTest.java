package appland.utils;

import appland.AppMapBaseTest;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AppMapFileChoosersTest extends AppMapBaseTest {
    private static final String LAST_OPENED_FILE_PATH = "last_opened_file_path";

    private TempDirTestFixture tempDirFixture;

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // Use a real on-disk temp dir so LocalFileSystem.findFileByPath can resolve it.
        tempDirFixture = new TempDirTestFixtureImpl();
        return tempDirFixture;
    }

    @Test
    public void resolveInitialDirectory_prefersLastOpenedWhenValid() throws Exception {
        var lastOpenedPath = myFixture.getTempDirPath();
        PropertiesComponent.getInstance(getProject()).setValue(LAST_OPENED_FILE_PATH, lastOpenedPath);

        var resolvedPath = new AtomicReference<String>();
        var expectedPath = new AtomicReference<String>();
        var latch = new CountDownLatch(1);
        // Resolve both off the EDT (VFS lookups are slow operations).
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var resolved = AppMapFileChoosers.resolveInitialDirectory(getProject());
                var expected = LocalFileSystem.getInstance().findFileByPath(lastOpenedPath);
                resolvedPath.set(resolved != null ? resolved.getPath() : null);
                expectedPath.set(expected != null ? expected.getPath() : null);
            } finally {
                latch.countDown();
            }
        });
        assertTrue("resolution did not complete in time", latch.await(5, TimeUnit.SECONDS));

        assertNotNull("the last-opened temp dir must be resolvable by VFS", expectedPath.get());
        assertEquals("the last-opened path should take priority", expectedPath.get(), resolvedPath.get());
    }

    @Test
    public void resolveInitialDirectory_returnsParentWhenLastOpenedIsFile() throws Exception {
        // createFile registers the file in VFS on the test thread; doing that inside the pooled task
        // (which needs the EDT for the refresh) while the EDT waits on the latch would deadlock.
        var file = tempDirFixture.createFile("org-config.json", "{}");
        assertNotNull("the created file should have a parent", file.getParent());
        PropertiesComponent.getInstance(getProject()).setValue(LAST_OPENED_FILE_PATH, file.getPath());

        var resolved = resolveOffEdt();
        assertNotNull(resolved);
        assertEquals("a last-opened file should resolve to its parent directory",
                file.getParent().getPath(), resolved.getPath());
    }

    @Test
    public void resolveInitialDirectory_nonNullFallbackWhenNoLastOpened() throws Exception {
        PropertiesComponent.getInstance(getProject()).unsetValue(LAST_OPENED_FILE_PATH);

        assertNotNull("should fall back to the project root or user home", resolveOffEdt());
    }

    /** resolveInitialDirectory must run off the EDT (VFS lookups are slow operations). */
    private VirtualFile resolveOffEdt() throws InterruptedException {
        var ref = new AtomicReference<VirtualFile>();
        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ref.set(AppMapFileChoosers.resolveInitialDirectory(getProject()));
            } finally {
                latch.countDown();
            }
        });
        assertTrue("resolution did not complete in time", latch.await(5, TimeUnit.SECONDS));
        return ref.get();
    }
}
