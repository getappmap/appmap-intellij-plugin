package appland.index;

import com.intellij.icons.AllIcons;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
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
    Function(8, "function", "#"),
    ExternalRoute(9, "external-route");

    private final int id;
    private final @NotNull String name;
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

    public @Nullable Icon getIcon() {
        switch (this) {
            case Folder:
                return AllIcons.Nodes.Folder;
            case Package:
                return AllIcons.Nodes.Package;
            case ExternalService:
                return AllIcons.Nodes.Services;
            case HTTP: // fall-through
            case Route:
                return AllIcons.Nodes.WebFolder;
            case Database:
                return AllIcons.Nodes.DataSchema;
            case Query:
                return AllIcons.Nodes.DataTables;
            case Class:
                return AllIcons.Nodes.Class;
            case Function:
                return AllIcons.Nodes.Function;
            default:
                return null;
        }
    }

    public static @NotNull ClassMapItemType findById(int id) {
        var itemType = idToTypeMap.get(id);
        if (itemType != null) {
            return itemType;
        }
        throw new IllegalStateException("Unexpected id: " + id);
    }

    public static @Nullable ClassMapItemType findByName(@NotNull String name) {
        return nameToTypeMap.get(name);
    }
}
