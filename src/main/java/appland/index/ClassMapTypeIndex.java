package appland.index;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.InlineKeyDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * Index for classMap.json files, which maps "type -> (id,name)".
 */
public class ClassMapTypeIndex extends FileBasedIndexExtension<ClassMapItemType, List<ClassMapItem>> {
    private static final ID<ClassMapItemType, List<ClassMapItem>> INDEX_ID = ID.create("appmap.classMap");
    private static final FileBasedIndex.FileTypeSpecificInputFilter INPUT_FILTER = new NamedFileTypeFilter(JsonFileType.INSTANCE, "classMap.json"::equals);
    private static final DataExternalizer<List<ClassMapItem>> dataExternalizer = new DataExternalizer<>() {
        @Override
        public void save(@NotNull DataOutput out, @NotNull List<ClassMapItem> values) throws IOException {
            out.writeInt(values.size());

            for (var value : values) {
                IOUtil.writeUTF(out, value.id);
                IOUtil.writeUTF(out, value.name);
            }
        }

        @Override
        public List<ClassMapItem> read(@NotNull DataInput in) throws IOException {
            var size = in.readInt();
            var result = new ArrayList<ClassMapItem>(size);

            for (var i = 0; i < size; i++) {
                var id = IOUtil.readUTF(in);
                var name = IOUtil.readUTF(in);
                result.add(new ClassMapItem(id, name));
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

        var scope = GlobalSearchScope.everythingScope(project);

        var items = new HashMap<ClassMapItem, List<VirtualFile>>();
        FileBasedIndex.getInstance().processValues(INDEX_ID, type, null, (file, classMapItems) -> {
            for (var item : classMapItems) {
                var appMapFiles = items.computeIfAbsent(item, classMapItem -> new LinkedList<>());
                var appMapFile = ClassMapUtil.findAppMapSourceFile(file);
                if (appMapFile != null) {
                    appMapFiles.add(appMapFile);
                }
            }
            return true;
        }, scope);
        return items;
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
        return dataExternalizer;
    }

    @Override
    public int getVersion() {
        return 3;
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
                protected void onItem(int level, @NotNull String id, ClassMapItemType type, @NotNull String name) {
                    var list = result.computeIfAbsent(type, ignored -> new ArrayList<>());
                    list.add(new ClassMapItem(id, name));
                }
            }.parse(inputData.getContentAsText());

            return result;
        }
    }
}
