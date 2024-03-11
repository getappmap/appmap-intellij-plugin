package appland.utils;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GsonUtils {
    public static final Gson GSON = new GsonBuilder().create();

    public static @Nullable JsonPrimitive getPath(@NotNull JsonObject json, @NotNull String... path) {
        if (path.length == 0) {
            return null;
        }

        var current = json;
        for (var i = 0; i < path.length - 1; i++) {
            var property = current.get(path[i]);
            if (!(property instanceof JsonObject)) {
                return null;
            }

            current = (JsonObject) property;
        }
        return current.getAsJsonPrimitive(path[path.length - 1]);
    }

    public static boolean hasProperty(@NotNull JsonObject json, @NotNull String... path) {
        return getPath(json, path) != null;
    }

    public static @NotNull JsonObject singlePropertyObject(@NotNull String name, @NotNull JsonElement value) {
        var json = new JsonObject();
        json.add(name, value);
        return json;
    }

    public static @NotNull JsonObject singlePropertyObject(@NotNull String name, @NotNull String value) {
        var json = new JsonObject();
        json.addProperty(name, value);
        return json;
    }

    public static @NotNull JsonObject singlePropertyObject(@NotNull String name, @NotNull Number value) {
        var json = new JsonObject();
        json.addProperty(name, value);
        return json;
    }

    public static @NotNull JsonObject singlePropertyObject(@NotNull String name, boolean value) {
        var json = new JsonObject();
        json.addProperty(name, value);
        return json;
    }

    public static @NotNull JsonArray createArray(@NotNull JsonObject... values) {
        var array = new JsonArray();
        for (var value : values) {
            array.add(value);
        }
        return array;
    }
}
