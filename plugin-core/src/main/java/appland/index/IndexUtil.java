package appland.index;

import appland.config.AppMapConfigFile;
import appland.files.AppMapFiles;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

final class IndexUtil {
    // base version for our indexes, increase when the data structures or the parsing logic change
    static final int BASE_VERSION = 57;

    // filenames covered by our own indexes
    static final Set<String> indexedFilenames = Collections.unmodifiableSet(findIndexedFilenames());

    /**
     * @param project Current project
     * @return Directories on the local filesystem, which contain indexable AppMap data.
     * The directories will be used for watched roots and for index decisions of the IDE.
     */
    static Set<VirtualFile> findAppMapIndexDirectories(@NotNull Project project) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        var appMapDirs = VfsUtilCore.createCompactVirtualFileSet();
        collectAppMapOutputDirectories(project, appMapDirs);
        return appMapDirs;
    }

    private static void collectAppMapOutputDirectories(@NotNull Project project, @NotNull Set<VirtualFile> target) {
        var configFiles = findConfigFilesByExcludedDirs(project);
        for (var configFile : configFiles) {
            var config = AppMapConfigFile.parseConfigFile(configFile);
            if (config != null && config.getAppMapDir() != null) {
                var appMapDir = VfsUtilCore.findRelativeFile(config.getAppMapDir(), configFile.getParent());
                if (appMapDir != null && appMapDir.isValid() && appMapDir.isInLocalFileSystem()) {
                    target.add(appMapDir);
                }
            }
        }
    }

    /**
     * Locate appmap.yml files without index access.
     */
    private static @NotNull Collection<VirtualFile> findConfigFilesByExcludedDirs(@NotNull Project project) {
        var configFiles = VfsUtilCore.createCompactVirtualFileSet();
        for (var module : ModuleManager.getInstance(project).getModules()) {
            for (var root : ModuleRootManager.getInstance(module).getExcludeRoots()) {
                // we're only checking two parents at most
                var start = root;
                for (var i = 0; i < 2 && start != null; i++) {
                    var parent = start.getParent();
                    if (parent != null && parent.isValid()) {
                        var config = parent.findChild(AppMapFiles.APPMAP_YML);
                        if (config != null) {
                            configFiles.add(config);
                            break;
                        }
                    }
                    start = parent;
                }
            }
        }
        return configFiles;
    }

    static boolean isIndexedFile(@NotNull VirtualFile file) {
        return ClassMapUtil.isClassMap(file)
                || AppMapFindingsUtil.isFindingFile(file)
                || indexedFilenames.contains(file.getName());
    }

    @RequiresReadLock
    static <T> @Nullable T getSingleEntryAppMapData(@NotNull ID<Integer, T> indexId,
                                                    @NotNull Project project,
                                                    @NotNull VirtualFile appMapMetadataDirectory,
                                                    @NotNull String filename) {

        var indexedFile = appMapMetadataDirectory.findChild(filename);
        if (indexedFile == null) {
            return null;
        }

        return FileBasedIndex.getInstance().getSingleEntryIndexData(indexId, indexedFile, project);
    }

    @NotNull
    private static Set<String> findIndexedFilenames() {
        try {
            return FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList().stream()
                    .filter(extension -> extension instanceof AbstractAppMapMetadataFileIndex<?>)
                    .map(extension -> ((AbstractAppMapMetadataFileIndex<?>) extension).getIndexedFileName())
                    .collect(Collectors.toSet());
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            // catching any exception, because we must not break indexing
            return Collections.emptySet();
        }
    }
}
