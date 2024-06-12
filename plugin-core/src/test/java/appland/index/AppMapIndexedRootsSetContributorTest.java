package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.WriteAction;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AppMapIndexedRootsSetContributorTest extends AppMapBaseTest {
    @Before
    public void verifyVersions(){
        Assume.assumeTrue(ApplicationInfo.getInstance().getBuild().getBaselineVersion() > 231);
    }

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

    private @NotNull PsiFile createFile(@NotNull String fileName) {
        return myFixture.configureByText(fileName, "");
    }
}