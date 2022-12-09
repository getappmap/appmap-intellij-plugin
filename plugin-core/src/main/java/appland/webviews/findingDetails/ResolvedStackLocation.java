package appland.webviews.findingDetails;

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
class ResolvedStackLocation {
    // native path
    public @NotNull String absolutePath;
    // in native path format, may have a :line suffix
    public @NotNull String truncatedPath;
    // 0-based
    public @Nullable Integer line;

    static class TypeAdapter implements JsonSerializer<ResolvedStackLocation>, JsonDeserializer<ResolvedStackLocation> {
        @Override
        public ResolvedStackLocation deserialize(JsonElement jsonElement,
                                                 Type type,
                                                 JsonDeserializationContext context) throws JsonParseException {
            var json = jsonElement.getAsJsonObject();
            var absolutePath = json.getAsJsonObject("uri").getAsJsonPrimitive("path").getAsString();
            var truncatedPath = json.getAsJsonPrimitive("truncatedPath").getAsString();
            var line = json.has("range") ? json.getAsJsonObject("range").getAsJsonPrimitive("line") : null;
            var lineValue = line != null ? line.getAsInt() : null;
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
                json.add("range", GsonUtils.singlePropertyObject("line", stackLocation.line));
            }
            return json;
        }
    }
}
