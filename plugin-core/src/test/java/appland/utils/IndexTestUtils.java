package appland.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.IndexingTestUtil;
import org.jetbrains.annotations.NotNull;

public class IndexTestUtils {
    @SuppressWarnings("UnstableApiUsage")
    public static void waitUntilIndexesAreReady(@NotNull Project project) {
        // according to JetBrains Slack, waitUntilIndexesAreReady must not be called inside a WriteAction or ReadAction
        var application = ApplicationManager.getApplication();
        if (application.isWriteAccessAllowed()) {
            throw new IllegalStateException("waitUntilIndexesAreReady must not be called in a WriteAction");
        }
        if (!application.isDispatchThread()) {
            application.assertReadAccessNotAllowed();
        }

        IndexingTestUtil.waitUntilIndexesAreReady(project);
    }
}
