package appland.config;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppMapConfigFileListenerTest extends AppMapBaseTest {
    @Test
    public void renameToAppMapYaml() throws InterruptedException {
        var file = myFixture.configureByText("appmap-new.yml", "content");

        var latch = createListenerCondition();
        myFixture.renameElement(file, "appmap.yml");
        assertTrue("Renames to appmap.yml must be detected", latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void renameFromAppMapYaml() throws InterruptedException {
        var file = myFixture.configureByText("appmap.yml", "content");

        var latch = createListenerCondition();
        myFixture.renameElement(file, "appmap-new.yml");
        assertTrue("Renames from appmap.yml to something else must be detected", latch.await(30, TimeUnit.SECONDS));
    }

    private CountDownLatch createListenerCondition() {
        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapConfigFileListener.TOPIC, latch::countDown);
        return latch;
    }
}