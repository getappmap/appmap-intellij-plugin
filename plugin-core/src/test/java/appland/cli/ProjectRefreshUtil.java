package appland.cli;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

public final class ProjectRefreshUtil {
    private ProjectRefreshUtil() {
    }

    public static CountDownLatch newProjectRefreshCondition(@NotNull Disposable disposable) {
        var latch = new CountDownLatch(1);
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(disposable)
                .subscribe(AppLandCommandLineListener.TOPIC, latch::countDown);
        return latch;
    }
}
