package appland.actions;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

import java.io.IOException;

public class OpenRecentAppMapActionTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void action() throws InterruptedException, IOException {
        var oldest = myFixture.configureByText("old.appmap.json", "{}");
        Thread.sleep(100);
        myFixture.configureByText("newer.appmap.json", "{}");
        Thread.sleep(100);
        myFixture.configureByText("newest.appmap.json", "{}");

        var found = OpenRecentAppMapAction.findMostRecentlyModifiedAppMap(getProject());
        assertNotNull(found);
        assertEquals("newest.appmap.json", found.getName());

        // modify "old.appmap.json" and make sure that it's returned
        Thread.sleep(100);
        WriteAction.runAndWait(() -> {
            VfsUtil.saveText(oldest.getVirtualFile(), "{\"name\": \"key\"}");
        });

        found = OpenRecentAppMapAction.findMostRecentlyModifiedAppMap(getProject());
        assertNotNull(found);
        assertEquals("old.appmap.json", found.getName());
    }
}