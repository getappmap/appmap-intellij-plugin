package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.changes.committed.MockAbstractVcs;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Objects;

public class LanguageResolverIgnoredFilesTest extends AppMapBaseTest {
    private MockAbstractVcs myVcs;
    private ProjectLevelVcsManagerImpl myVcsManager;
    private ChangeListManagerImpl myChangeListManager;
    private File ignoredClientRoot;
    private LocalFileSystem myLFS;

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected String getBasePath() {
        return "installGuide/language-resolver";
    }

    @Before
    public void setUpTest() {
        EdtTestUtil.runInEdtAndWait(() -> {
            ignoredClientRoot = new File(myFixture.getTempDirPath(), "ignored_root");
            ignoredClientRoot.mkdir();

            myVcs = new MockAbstractVcs(getProject());
            myVcs.setChangeProvider(new MyMockChangeProvider());

            myVcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(getProject());
            myVcsManager.registerVcs(myVcs);
            myVcsManager.setDirectoryMapping(ignoredClientRoot.getPath(), myVcs.getName());

            myLFS = LocalFileSystem.getInstance();
            myChangeListManager = ChangeListManagerImpl.getInstanceImpl(getProject());
        });
    }

    @After
    public void tearDownTest() {
        EdtTestUtil.runInEdtAndWait(() -> {
            myVcsManager.unregisterVcs(myVcs);
            myVcs = null;
            myVcsManager = null;
            myChangeListManager = null;
            FileUtil.delete(ignoredClientRoot);
        });
    }

    @Test
    public void ignoredFilesDetection() {
        var ignoredRoot = myLFS.refreshAndFindFileByIoFile(ignoredClientRoot);
        assertNotNull(ignoredRoot);
        var root = copySourceDirIntoIgnoredRoot("python-java");

        var language = new LanguageResolver(getProject()).getLanguage(root);
        assertNull("Language detection must ignore VCS ignored files", language);
    }

    @NotNull
    private VirtualFile copySourceDirIntoIgnoredRoot(@NotNull String sourceDirectory) {
        var file = myFixture.copyDirectoryToProject(sourceDirectory, ignoredClientRoot.getName());
        myChangeListManager.ensureUpToDate();
        return file;
    }

    private static class MyMockChangeProvider implements ChangeProvider {
        @Override
        public void getChanges(@NotNull VcsDirtyScope dirtyScope,
                               @NotNull final ChangelistBuilder builder,
                               @NotNull ProgressIndicator progress,
                               @NotNull ChangeListManagerGate addGate) {
            for (var path : dirtyScope.getDirtyFiles()) {
                builder.processIgnoredFile(path);
            }

            for (var dir : dirtyScope.getRecursivelyDirtyDirectories()) {
                VfsUtilCore.processFilesRecursively(Objects.requireNonNull(dir.getVirtualFile()), vf -> {
                    builder.processIgnoredFile(VcsUtil.getFilePath(vf));
                    return true;
                });
            }
        }

        @Override
        public boolean isModifiedDocumentTrackingRequired() {
            return false;
        }
    }
}