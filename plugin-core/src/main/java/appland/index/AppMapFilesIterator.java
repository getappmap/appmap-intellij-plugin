package appland.index;

import appland.AppMapBundle;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.util.indexing.roots.IndexableFilesIterationMethods;
import com.intellij.util.indexing.roots.IndexableFilesIterator;
import com.intellij.util.indexing.roots.kind.IndexableSetOrigin;
import com.intellij.util.indexing.roots.kind.ProjectFileOrDirOrigin;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
class AppMapFilesIterator implements IndexableFilesIterator {
    private final VirtualFile excludedRoot;

    public AppMapFilesIterator(@NotNull VirtualFile excludedRoot) {
        this.excludedRoot = excludedRoot;
    }

    @Override
    public @NotNull IndexableSetOrigin getOrigin() {
        return (ProjectFileOrDirOrigin) () -> excludedRoot;
    }

    @Override
    public @NonNls String getDebugName() {
        return "appland.indexableFiles";
    }

    @Override
    public @NlsContexts.ProgressText String getRootsScanningProgressText() {
        return AppMapBundle.get("fileWatcher.scanningAppMaps");
    }

    @Override
    public @NlsContexts.ProgressText String getIndexingProgressText() {
        return AppMapBundle.get("fileWatcher.indexingAppMaps");
    }

    @Override
    public boolean iterateFiles(@NotNull Project project,
                                @NotNull ContentIterator fileIterator,
                                @NotNull VirtualFileFilter fileFilter) {

        var roots = ReadAction.compute(() -> {
            return excludedRoot.isValid()
                    ? List.of(excludedRoot)
                    : Collections.<VirtualFile>emptyList();
        });

        return IndexableFilesIterationMethods.INSTANCE.iterateRoots(project, roots, fileIterator, fileFilter, false);
    }

    // needed for older SDKs
    public @NotNull Set<String> getRootUrls() {
        return Set.of(excludedRoot.getUrl());
    }

    // needed for 2022.2
    public @NotNull Set<String> getRootUrls(@NotNull Project project) {
        return getRootUrls();
    }
}
