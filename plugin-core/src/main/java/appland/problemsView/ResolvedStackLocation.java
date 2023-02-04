package appland.problemsView;

import appland.utils.GsonUtils;
import com.google.gson.*;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * References a file on disk, optionally specifying a line inside that file.
 * The JSON follows VSCode's JSON structure `of "Location".
 */
@Value
public class ResolvedStackLocation {
    // native path
    public @NotNull String absolutePath;
    // in native path format, may have a :line suffix
    public @NotNull String truncatedPath;
    // 0-based
    public @Nullable Integer line;

    public static class TypeAdapter implements JsonSerializer<ResolvedStackLocation>, JsonDeserializer<ResolvedStackLocation> {
        @Override
        public ResolvedStackLocation deserialize(JsonElement jsonElement,
                                                 Type type,
                                                 JsonDeserializationContext context) throws JsonParseException {
            var json = jsonElement.getAsJsonObject();
            var absolutePath = json.getAsJsonObject("uri").getAsJsonPrimitive("path").getAsString();
            var truncatedPath = json.getAsJsonPrimitive("truncatedPath").getAsString();

            Integer lineValue = null;
            var range = json.getAsJsonArray("range");
            if (range != null && !range.isEmpty()) {
                var line = range.get(0).getAsJsonObject().getAsJsonPrimitive("line");
                if (line != null) {
                    lineValue = line.getAsInt();
                }
            }
            return new ResolvedStackLocation(absolutePath, truncatedPath, lineValue);
        }

        @Override
        public JsonElement serialize(ResolvedStackLocation stackLocation,
                                     Type type,
                                     JsonSerializationContext jsonSerializationContext) {
            var json = new JsonObject();
            json.addProperty("truncatedPath", stackLocation.truncatedPath);
            json.add("uri", GsonUtils.singlePropertyObject("path", stackLocation.absolutePath));
            if (stackLocation.line != null) {
                var range = new JsonObject();
                range.addProperty("line", stackLocation.line);
                range.addProperty("character", 0);
                json.add("range", GsonUtils.createArray(range));
            }
            return json;
        }
    }
}
