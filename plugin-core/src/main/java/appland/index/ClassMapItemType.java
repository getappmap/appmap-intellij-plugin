package appland.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ClassMapItemType {
    ROOT(-1, "<root>", ""),
    // root item types
    HTTP(0, "http"),
    Folder(1, "folder"),
    Database(2, "database"),
    Package(3, "package", "/"),
    ExternalService(4, "external-service"),
    // children types
    Route(5, "route"),
    Query(6, "query"),
    Class(7, "class", "::"),
    Function(8, "function", "#");

    @Getter
    private final int id;
    @Getter
    private final @NotNull String name;
    @Getter
    private final @NotNull String separator;

    private static final Map<String, ClassMapItemType> nameToTypeMap;
    private static final Int2ObjectMap<ClassMapItemType> idToTypeMap;

    static {
        var values = values();
        var nameMapping = new HashMap<String, ClassMapItemType>(values.length);
        var idMapping = new Int2ObjectOpenHashMap<ClassMapItemType>(values.length);
        for (var itemType : values) {
            nameMapping.put(itemType.name, itemType);
            idMapping.put(itemType.id, itemType);
        }
        nameToTypeMap = Collections.unmodifiableMap(nameMapping);
        idToTypeMap = Int2ObjectMaps.unmodifiable(idMapping);
    }

    ClassMapItemType(int id, @NotNull String name) {
        this(id, name, "->");
    }

    ClassMapItemType(int id, @NotNull String name, @NotNull String separator) {
        this.id = id;
        this.name = name;
        this.separator = separator;
    }

    public static @NotNull ClassMapItemType findById(int id) {
        var itemType = idToTypeMap.get(id);
        if (itemType != null) {
            return itemType;
        }
        throw new IllegalStateException("Unexpected id: " + id);
    }

    public static @NotNull ClassMapItemType findByName(@NotNull String name) {
        var itemType = nameToTypeMap.get(name);
        if (itemType != null) {
            return itemType;
        }
        throw new IllegalStateException("Unexpected name: " + name);
    }
}
