package appland.files;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppMapAsyncFileListenerTest extends AppMapBaseTest {
    @Test
    public void appMapCreated() throws Throwable {
        var condition = createCondition();

        createAppMapWithIndexes("a");

        assertTrue(condition.await(10, TimeUnit.SECONDS));
    }

    private CountDownLatch createCondition() {
        var latch = new CountDownLatch(1);

        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapFileChangeListener.TOPIC, (changes, isGenericRefresh) -> {
                    latch.countDown();
                });

        return latch;
    }
}