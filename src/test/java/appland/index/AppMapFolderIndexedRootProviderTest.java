package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class AppMapFolderIndexedRootProviderTest extends AppMapBaseTest {
    @Test
    public void index() throws IOException {
        var excludedFolder = myFixture.getTempDirFixture().findOrCreateDir("excluded");
        WriteAction.runAndWait(() -> {
            excludedFolder.createChildDirectory(this, "appmap");
            excludedFolder.createChildDirectory(this, "appmap-not-indexed");
        });

        ModuleRootModificationUtil.updateExcludedFolders(getModule(), excludedFolder.getParent(),
                Collections.emptyList(),
                Collections.singletonList(excludedFolder.getUrl()));

        myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json", "excluded/appmap/Create_Owner.appmap.json");
        myFixture.copyFileToProject("appmap-files/Create_Owner.appmap.json", "excluded/appmap-not-indexed/Create_Owner.appmap.json");

        // 2020.2 EAP seems to always index excluded folders
        var foundMaps = AppMapMetadataIndex.findAppMaps(getProject(), "Create Owner");
        assertNotEmpty(foundMaps);
    }
}