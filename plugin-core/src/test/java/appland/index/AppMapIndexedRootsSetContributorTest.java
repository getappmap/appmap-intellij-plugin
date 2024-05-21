package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import org.junit.Test;

import java.io.IOException;

public class AppMapIndexedRootsSetContributorTest extends AppMapBaseTest {
    @Test
    public void index() throws IOException, InterruptedException {
        var excludedFolder = myFixture.getTempDirFixture().findOrCreateDir("excluded");
        WriteAction.runAndWait(() -> {
            excludedFolder.createChildDirectory(this, "appmap");
            excludedFolder.createChildDirectory(this, "appmap-not-indexed");
        });

        // AppMapIndexedRootsSetContributor only un-excludes directories in a content root with appmap.yml
        createAppMapYaml(excludedFolder.getParent(), excludedFolder.getName());

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
}