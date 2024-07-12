package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.WriteAction;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

public class AppMapIndexedRootsSetContributorTest extends AppMapBaseTest {
    @Before
    public void verifyVersions() {
        Assume.assumeTrue(ApplicationInfo.getInstance().getBuild().getBaselineVersion() > 231);
    }

    @Test
    public void index() throws Exception {
        var excludedFolder = myFixture.getTempDirFixture().findOrCreateDir("excluded");
        WriteAction.runAndWait(() -> {
            excludedFolder.createChildDirectory(this, "appmap");
        });

        withContentRoot(excludedFolder, () -> {
            // AppMaps created in already excluded folders must still keep indexing enabled for them.
            // But we can only expect indexing if there's an appmap.yml file.
            withExcludedFolder(excludedFolder, () -> {
                createAppMapConfig(excludedFolder, Path.of("appmap"));

                myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json", "excluded/appmap/Create_Owner.appmap.json");
                myFixture.copyDirectoryToProject("appmap-files/Create_Owner", "excluded/appmap/Create_Owner");

                waitUntilIndexesAreReady();

                var foundMaps = AppMapMetadataService.getInstance(getProject()).findAppMaps("Create Owner");
                assertNotEmpty(foundMaps);
            });
        });
    }
}