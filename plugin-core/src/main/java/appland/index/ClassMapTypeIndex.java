package appland.index;

import appland.files.AppMapFiles;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.InlineKeyDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * Index for classMap.json files, which maps "type -> (id,name)".
 */
public class ClassMapTypeIndex extends FileBasedIndexExtension<ClassMapItemType, List<ClassMapItem>> {
    private static final ID<ClassMapItemType, List<ClassMapItem>> INDEX_ID = ID.create("appmap.classMap");
    private static final FileBasedIndex.FileTypeSpecificInputFilter INPUT_FILTER = new NamedFileTypeFilter(JsonFileType.INSTANCE, ClassMapUtil.CLASS_MAP_FILE::equalsIgnoreCase);
    private static final DataExternalizer<List<ClassMapItem>> DATA_EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(@NotNull DataOutput out, @NotNull List<ClassMapItem> values) throws IOException {
            out.writeInt(values.size());

            for (var value : values) {
                IOUtil.writeUTF(out, StringUtil.defaultIfEmpty(value.parentId, ""));
                IOUtil.writeUTF(out, value.id);
                IOUtil.writeUTF(out, value.name);
            }
        }

        @Override
        public List<ClassMapItem> read(@NotNull DataInput in) throws IOException {
            var size = in.readInt();
            var result = new ArrayList<ClassMapItem>(size);

            for (var i = 0; i < size; i++) {
                var parentId = IOUtil.readUTF(in);
                var id = IOUtil.readUTF(in);
                var name = IOUtil.readUTF(in);
                result.add(new ClassMapItem(parentId.isEmpty() ? null : parentId, id, name));
            }

            return result;
        }
    };

    /**
     * @return Class map items of the given type, associated with the AppMap source files.
     */
    public static Map<ClassMapItem, List<VirtualFile>> findItems(@NotNull Project project, @NotNull ClassMapItemType type) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyMap();
        }

        var items = new HashMap<ClassMapItem, List<VirtualFile>>();
        processItems(project, type, (file, classMapItems) -> {
            for (var item : classMapItems) {
                var appMapFiles = items.computeIfAbsent(item, classMapItem -> new LinkedList<>());
                var appMapFile = AppMapFiles.findAppMapSourceFile(file);
                if (appMapFile != null) {
                    appMapFiles.add(appMapFile);
                }
            }
            return true;
        });
        return items;
    }

    public static void processItems(@NotNull Project project,
                                    @NotNull ClassMapItemType type,
                                    @NotNull FileBasedIndex.ValueProcessor<List<ClassMapItem>> processor) {
        if (DumbService.isDumb(project)) {
            return;
        }

        var scope = GlobalSearchScope.getScopeRestrictedByFileTypes(new EverythingExceptLibrariesScope(project), JsonFileType.INSTANCE);
        FileBasedIndex.getInstance().processValues(INDEX_ID, type, null, processor, scope);
    }

    @Override
    public @NotNull ID<ClassMapItemType, List<ClassMapItem>> getName() {
        return INDEX_ID;
    }

    @Override
    public @NotNull DataIndexer<ClassMapItemType, List<ClassMapItem>, FileContent> getIndexer() {
        return new ClassMapItemIndexer();
    }

    @Override
    public @NotNull KeyDescriptor<ClassMapItemType> getKeyDescriptor() {
        return new InlineKeyDescriptor<>() {
            @Override
            public @NotNull ClassMapItemType fromInt(int id) {
                return ClassMapItemType.findById(id);
            }

            @Override
            public int toInt(@NotNull ClassMapItemType type) {
                return type.getId();
            }
        };
    }

    @Override
    public @NotNull DataExternalizer<List<ClassMapItem>> getValueExternalizer() {
        return DATA_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return IndexUtil.BASE_VERSION;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    private static class ClassMapItemIndexer implements DataIndexer<ClassMapItemType, List<ClassMapItem>, FileContent> {
        @Override
        public @NotNull Map<ClassMapItemType, List<ClassMapItem>> map(@NotNull FileContent inputData) {
            var result = new EnumMap<ClassMapItemType, List<ClassMapItem>>(ClassMapItemType.class);

            new StreamingClassMapIterator() {
                @Override
                protected void onItem(@NotNull ClassMapItemType type, @Nullable String parentId, @NotNull String id, @NotNull String name, int level) {
                    var list = result.computeIfAbsent(type, ignored -> new LinkedList<>());
                    list.add(new ClassMapItem(parentId, id, name));
                }
            }.parse(inputData.getContentAsText());

            return result;
        }
    }
}
