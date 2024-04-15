package appland.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableRunnable;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class ModuleTestUtils {
    public static void withContentRoot(@NotNull Module module, @NotNull VirtualFile contentRoot, @NotNull ThrowableRunnable<Exception> runnable) throws Exception {
        TestCase.assertTrue(contentRoot.isDirectory());

        var contentEntryRef = new AtomicReference<ContentEntry>();
        try {
            // hack to use ModuleRootModificationUtil and to keep a reference to the new entry
            ModuleRootModificationUtil.updateModel(module, modifiableRootModel -> {
                contentEntryRef.set(modifiableRootModel.addContentEntry(contentRoot));
            });

            runnable.run();
        } finally {
            // remove again to avoid breaking follow-up tests
            var newEntry = contentEntryRef.get();
            TestCase.assertNotNull(newEntry);

            ModuleRootModificationUtil.updateModel(module, modifiableRootModel -> {
                modifiableRootModel.removeContentEntry(newEntry);
            });
        }
    }
}
