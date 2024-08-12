package appland;

import appland.utils.IndexTestUtils;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Test base class to execute tests which need files on the local disk and not in the in-memory TempFileSystem,
 * which is used by default.
 */
public abstract class AppMapLocalTempFilesTest extends AppMapBaseTest {
    private AtomicReference<ContentEntry> tempDirContentEntry;

    @Override
    protected final TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Before
    public void setupTempPathContentRoot() {
        var tempDir = LocalFileSystem.getInstance().findFileByPath(myFixture.getTempDirPath());
        assertNotNull("Temp directory must exist on disk", tempDir);

        // hack to use ModuleRootModificationUtil and to keep a reference to the new entry
        assertNull(tempDirContentEntry);
        tempDirContentEntry = new AtomicReference<>();
        ModuleRootModificationUtil.updateModel(getModule(), modifiableRootModel -> {
            tempDirContentEntry.set(modifiableRootModel.addContentEntry(tempDir));
        });

        IndexTestUtils.waitUntilIndexesAreReady(getProject());
    }

    @After
    public void removeTempPathContentRoot() {
        var contentRoot = tempDirContentEntry.getAndSet(null);
        assertNotNull("Content root of temp dir file must be available", contentRoot);

        ModuleRootModificationUtil.updateModel(getModule(), modifiableRootModel -> {
            modifiableRootModel.removeContentEntry(contentRoot);
        });
    }
}
