package appland.actions;

import appland.AppMapBaseTest;
import appland.cli.TestAppLandDownloadService;
import appland.utils.DataContexts;
import appland.webviews.navie.NavieEditor;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class AddNavieContextFilesActionTest extends AppMapBaseTest {
    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Before
    public void ensureToolsDownloaded() {
        TestAppLandDownloadService.ensureDownloaded();

        // Unfortunately, JCEF is unsupported on headless systems, which includes CI.
        Assume.assumeTrue("JCEF must be supported to run tests of this class", JBCefApp.isSupported());
    }

    @Test
    public void enabledForNavieEditorAndContextFile() throws Exception {
        // must be created first because the Navie editor must be the selected editor
        var contextFile = myFixture.configureByText("test.txt", "");

        var tempDir = myFixture.createFile("test.txt", "").getParent();
        withContentRoot(tempDir, () -> {
            openNavieEditor(tempDir);

            var presentation = updateAction(createActionContext(contextFile.getVirtualFile()));
            Assert.assertTrue("Action must be enabled for a file", presentation.isEnabledAndVisible());
        });
    }

    @Test
    public void disabledWithoutNavieEditor() throws Exception {
        var contextFile = myFixture.configureByText("test.txt", "");

        var tempDir = myFixture.createFile("test.txt", "").getParent();
        withContentRoot(tempDir, () -> {
            var presentation = updateAction(createActionContext(contextFile.getVirtualFile()));
            Assert.assertFalse("Action must be unavailable without selected Navie editor", presentation.isEnabledAndVisible());
        });
    }

    @Test
    public void disabledWithoutContextFile() throws Exception {
        var contextDir = myFixture.configureByText("test.txt", "").getVirtualFile().getParent();

        var tempDir = myFixture.createFile("test.txt", "").getParent();
        withContentRoot(tempDir, () -> {
            openNavieEditor(tempDir);

            var presentation = updateAction(createActionContext(contextDir));
            Assert.assertFalse("Action must be unavailable for a selected directory", presentation.isEnabledAndVisible());
        });
    }

    private void openNavieEditor(@NotNull VirtualFile tempDir) throws Exception {
        createAppMapConfig(tempDir, Path.of("tmp", "appmap"));
        waitForJsonRpcServerPort();

        edt(() -> NavieEditorProvider.openEditor(getProject(), DataContext.EMPTY_CONTEXT));

        // wait until the Navie editor is the selected editor
        var deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (System.currentTimeMillis() <= deadline) {
            var editor = EdtTestUtil.runInEdtAndGet(() -> FileEditorManager.getInstance(getProject()).getSelectedEditor());
            if (editor instanceof NavieEditor) {
                break;
            }
            Thread.sleep(500);
        }

        var editor = EdtTestUtil.runInEdtAndGet(() -> FileEditorManager.getInstance(getProject()).getSelectedEditor());
        assertTrue("Navie editor must be open", editor instanceof NavieEditor);
    }

    private @NotNull DataContext createActionContext(@Nullable VirtualFile contextFile) {
        return DataContexts.createCustomContext(dataId -> {
            if (contextFile != null && PlatformDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) {
                return new VirtualFile[]{contextFile};
            }
            if (PlatformDataKeys.PROJECT.is(dataId)) {
                return myFixture.getProject();
            }
            return null;
        });
    }

    private static @NotNull Presentation updateAction(@NotNull DataContext context) {
        var action = ActionManager.getInstance().getAction("appmap.navie.pinContextFile");
        assertNotNull(action);

        var e = TestActionEvent.createFromAnAction(action, null, ActionPlaces.MAIN_MENU, context);
        ActionUtil.performDumbAwareUpdate(action, e, false);
        return e.getPresentation();
    }
}