package appland.problemsView.listener;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

class ScannerTestUtil {
    static CountDownLatch createFindingsCondition(@NotNull Project project, @NotNull Disposable disposable) {
        var latch = new CountDownLatch(1);
        project.getMessageBus().connect(disposable).subscribe(ScannerFindingsListener.TOPIC, new ScannerFindingsListener() {
            @Override
            public void afterFindingsReloaded() {
                latch.countDown();
            }

            @Override
            public void afterFindingsChanged() {
                latch.countDown();
            }
        });
        return latch;
    }

}
