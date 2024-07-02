package appland.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ModuleTestUtils {
    /**
     * Adds a content root to a module, runs the given runnable, and then removes the content root again.
     *
     * @param module      Module to add the content root to
     * @param contentRoot Content root to add
     * @param runnable    Runnable to run
     */
    public static void withContentRoot(@NotNull Module module,
                                       @NotNull VirtualFile contentRoot,
                                       @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        withContentRoots(module, List.of(contentRoot), runnable);
    }


    /**
     * Adds content roots at once to a module, runs the given runnable, and then removes the registered content roots again.
     *
     * @param module       Module to add the content roots to
     * @param contentRoots Content roots to add
     * @param runnable     Runnable to run
     */
    public static void withContentRoots(@NotNull Module module,
                                        @NotNull Collection<VirtualFile> contentRoots,
                                        @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        for (var contentRoot : contentRoots) {
            Assert.assertTrue(contentRoot.isDirectory());
        }

        var contentEntriesRef = new AtomicReference<Collection<ContentEntry>>();
        try {
            // hack to use ModuleRootModificationUtil and to keep a reference to the new entry
            ModuleRootModificationUtil.updateModel(module, modifiableRootModel -> {
                var contentEntries = new ArrayList<ContentEntry>();
                for (var contentRoot : contentRoots) {
                    contentEntries.add(modifiableRootModel.addContentEntry(contentRoot));
                }
                contentEntriesRef.set(contentEntries);
            });

            IndexTestUtils.waitUntilIndexesAreReady(module.getProject());

            runnable.run();
        } finally {
            // remove again to avoid breaking follow-up tests
            var newEntries = contentEntriesRef.get();
            Assert.assertNotNull(newEntries);

            ModuleRootModificationUtil.updateModel(module, modifiableRootModel -> {
                for (var contentEntry : newEntries) {
                    modifiableRootModel.removeContentEntry(contentEntry);
                }
            });
        }
    }
}
