package appland.index;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.roots.IndexableFilesContributor;
import com.intellij.util.indexing.roots.IndexableFilesIterator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Only used in 2023.1 and earlier.
 * <p>
 * Adds excluded "appmap" folders and excluded AppMap files for indexing.
 * <p>
 * We can't use an {@link com.intellij.util.indexing.AdditionalIndexedRootsScope}, because it's only called at startup.
 * We also have to update indexed files after the "Install Guide" wizard steps,
 * which add new roots after the project was opened.
 */
@SuppressWarnings("UnstableApiUsage")
public class AppMapIndexableFilesContributor implements IndexableFilesContributor {
    @Override
    public @NotNull List<IndexableFilesIterator> getIndexableFiles(@NotNull Project project) {
        var iterators = new ArrayList<IndexableFilesIterator>();
        for (var module : ModuleManager.getInstance(project).getModules()) {
            for (var excludedRoot : ModuleRootManager.getInstance(module).getExcludeRoots()) {
                iterators.add(new AppMapFilesIterator(excludedRoot));
            }
        }
        return iterators;
    }

    @Override
    public @NotNull Predicate<VirtualFile> getOwnFilePredicate(@NotNull Project project) {
        // we have to include directories, because that controls if the sub-entries are processed
        return file -> file.isDirectory() || IndexUtil.isIndexedFile(file);
    }
}
