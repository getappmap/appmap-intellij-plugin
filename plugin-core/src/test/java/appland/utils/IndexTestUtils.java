package appland.utils;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class IndexTestUtils {
    public static void waitUntilIndexesAreReady(@NotNull Project project) {
        // IndexingTestUtil.waitUntilIndexesAreReady(project);
        // 2024.2 made indexing in tests async and added IndexingTestUtil.waitUntilIndexesAreReady(project)
        // to allow tests to wait for indexing to finish.
        // But because this class and method is unavailable in < 2024.1, we need to use reflection to call this method.

        if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 241) {
            // according to JetBrains Slack, waitUntilIndexesAreReady must not be called inside a WriteAction or ReadAction
            var application = ApplicationManager.getApplication();

            if (application.isWriteAccessAllowed()) {
                throw new IllegalStateException("waitUntilIndexesAreReady must not be called in a WriteAction");
            }

            if (!application.isDispatchThread()) {
                application.assertReadAccessNotAllowed();
            }

            reflectiveWaitUntilIndexesAreReady(project);
        }
    }

    private static void reflectiveWaitUntilIndexesAreReady(@NotNull Project project) {
        try {
            var indexingTestUtil = IndexTestUtils.class.getClassLoader().loadClass("com.intellij.testFramework.IndexingTestUtil");
            var waitUntilIndexesAreReady = indexingTestUtil.getDeclaredMethod("waitUntilIndexesAreReady", Project.class);
            waitUntilIndexesAreReady.invoke(null, project);
        } catch (Exception e) {
            Logger.getInstance(IndexTestUtils.class).error("Error calling waitUntilIndexesAreReady via reflection");
        }
    }
}
