package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;

public class AppMapIndexableFilesContributorTest extends AppMapBaseTest {
    @Test
    public void index() throws IOException {
        var excludedFolder = myFixture.getTempDirFixture().findOrCreateDir("excluded");
        WriteAction.runAndWait(() -> {
            excludedFolder.createChildDirectory(this, "appmap");
            excludedFolder.createChildDirectory(this, "appmap-not-indexed");
        });

        withExcludedFolder(excludedFolder, () -> {
            myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json", "excluded/appmap/Create_Owner.appmap.json");
            myFixture.copyDirectoryToProject("appmap-files/Create_Owner", "excluded/appmap/Create_Owner");

            myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json", "excluded/appmap-not-indexed/Create_Owner.appmap.json");
            myFixture.copyDirectoryToProject("appmap-files/Create_Owner", "excluded/appmap-not-indexed/Create_Owner");

            // 2020.2 EAP seems to always index excluded folders
            var foundMaps = AppMapMetadataService.getInstance(getProject()).findAppMaps("Create Owner");
            assertNotEmpty(foundMaps);
        });
    }

    @Test
    public void files() throws IOException {
        assertAccepted(createFile("sample.appmap.json"));
        assertAccepted(createFile("classMap.json"));
        assertAccepted(createFile("appmap-findings.json"));

        assertRejected(createFile("sample.json"));
        assertRejected(createFile("sample.txt"));

        // any directory must be accepted, because it may contain an "appmap" directory
        assertAccepted(myFixture.getTempDirFixture().findOrCreateDir("some-directory"));
        assertAccepted(myFixture.getTempDirFixture().findOrCreateDir("target"));
        assertAccepted(myFixture.getTempDirFixture().findOrCreateDir("appmap"));
    }

    private void assertAccepted(PsiFile file) {
        assertAccepted(file.getVirtualFile());
    }

    private void assertRejected(PsiFile file) {
        assertRejected(file.getVirtualFile());
    }

    private void assertAccepted(@NotNull VirtualFile virtualFile) {
        var contributor = new AppMapIndexableFilesContributor();
        var predicate = contributor.getOwnFilePredicate(getProject());
        assertNotNull(virtualFile);
        assertTrue("File must be accepted: " + virtualFile.getName(), predicate.test(virtualFile));
    }

    private void assertRejected(@NotNull VirtualFile virtualFile) {
        var contributor = new AppMapIndexableFilesContributor();
        var predicate = contributor.getOwnFilePredicate(getProject());
        assertNotNull(virtualFile);
        assertFalse("File must be rejected: " + virtualFile.getName(), predicate.test(virtualFile));
    }

    private @NotNull PsiFile createFile(@NotNull String fileName) {
        return myFixture.configureByText(fileName, "");
    }
}