package appland.installGuide;

import appland.index.AppMapMetadata;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * Serializes AppMapMetaData in a format suitable for the Install-Guide web application.
 */
class AppMapMetadataWebAppSerializer implements JsonSerializer<AppMapMetadata> {
    @Override
    public JsonElement serialize(@NotNull AppMapMetadata map, Type type, JsonSerializationContext context) {
        var json = new JsonObject();
        json.addProperty("path", map.getSystemIndependentFilepath());
        json.addProperty("name", map.getName());
        json.addProperty("requests", map.getRequestCount());
        json.addProperty("sqlQueries", map.getQueryCount());
        json.addProperty("functions", map.getFunctionsCount());
        return json;
    }
}
