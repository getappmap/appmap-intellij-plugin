package appland.files;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppMapAsyncFileListenerTest extends AppMapBaseTest {
    @Test
    public void appMapCreated() throws InterruptedException {
        var condition = createCondition();

        myFixture.configureByText("a.appmap.json", createAppMapMetadataJSON("a"));

        assertTrue(condition.await(10, TimeUnit.SECONDS));
    }

    private CountDownLatch createCondition() {
        var latch = new CountDownLatch(1);

        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapFileChangeListener.TOPIC, latch::countDown);

        return latch;
    }
}