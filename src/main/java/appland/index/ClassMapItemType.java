package appland.index;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum ClassMapItemType {
    ROOT(-1, "<root>", ""),
    // root item types
    HTTP(0, "http"),
    Folder(1, "folder"),
    Database(2, "database"),
    Package(3, "package", "/"),
    // children types
    Route(4, "route"),
    Query(5, "query"),
    Class(6, "class", "::"),
    Function(7, "function", "#");

    @Getter
    private final int id;
    @Getter
    private final @NotNull String name;
    @Getter
    private final @NotNull String separator;

    ClassMapItemType(int id, @NotNull String name) {
        this(id, name, "->");
    }

    ClassMapItemType(int id, @NotNull String name, @NotNull String separator) {
        this.id = id;
        this.name = name;
        this.separator = separator;
    }

    public static @NotNull ClassMapItemType findById(int id) {
        // slow mapping for now
        for (var value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        throw new IllegalStateException("Unexpected id: " + id);
    }

    public static @NotNull ClassMapItemType findByName(@NotNull String name) {
        // slow mapping for now
        for (var value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalStateException("Unexpected name: " + name);
    }
}
