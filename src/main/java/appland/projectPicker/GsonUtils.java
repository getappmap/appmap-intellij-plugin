package appland.projectPicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GsonUtils {
    static final Gson GSON = new GsonBuilder().create();

    static @Nullable JsonPrimitive getPath(@NotNull JsonObject json, @NotNull String... path) {
        if (path.length == 0) {
            return null;
        }

        JsonObject current = json;
        for (int i = 0; i < path.length - 1; i++) {
            var property = current.get(path[i]);
            if (!(property instanceof JsonObject)) {
                return null;
            }

            current = (JsonObject) property;
        }
        return current.getAsJsonPrimitive(path[path.length - 1]);
    }

    static boolean hasProperty(@NotNull JsonObject json, @NotNull String... path) {
        return getPath(json, path) != null;
    }
}
