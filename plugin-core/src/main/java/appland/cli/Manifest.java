package appland.cli;

import appland.utils.GsonUtils;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Manifest {
    private static final Logger LOG = Logger.getInstance(Manifest.class);

    // Captures the platform identifier suffix of a manifest asset name, e.g.
    // `scanner-linux-x64`, `scanner-macos-arm64`, `scanner-win-x64.exe`. Anchored
    // so an unrelated trailing token won't be silently bucketed alongside the canonical asset.
    // The capture group excludes any `.exe` suffix so Windows and non-Windows entries share a key shape.
    private static final Pattern PLATFORM_IN_ASSET_NAME = Pattern.compile("((?:linux|macos|win)-(?:x64|arm64))(?:\\.exe)?$");
    private static final Pattern TAG_VERSION_PATTERN = Pattern.compile("v(\\d+\\.\\d+\\.\\d+[\\w.+-]*)$");

    public final @NotNull String version;
    private final @NotNull Map<String, ManifestAsset> assetsByPlatform;

    private Manifest(@NotNull String version, @NotNull Map<String, ManifestAsset> assetsByPlatform) {
        this.version = version;
        this.assetsByPlatform = assetsByPlatform;
    }

    public static @Nullable Manifest parse(@NotNull String rawJson) {
        try {
            var element = GsonUtils.GSON.fromJson(rawJson, JsonObject.class);
            if (element == null || !element.isJsonObject()) {
                LOG.warn("Manifest is not a JSON object");
                return null;
            }
            var obj = element.getAsJsonObject();

            if (!obj.has("tag_name") || !obj.get("tag_name").isJsonPrimitive()) {
                LOG.warn("Manifest tag_name is missing or not a string");
                return null;
            }

            var tagName = obj.getAsJsonPrimitive("tag_name").getAsString();
            var match = TAG_VERSION_PATTERN.matcher(tagName);
            if (!match.find()) {
                LOG.warn("Could not parse manifest version from tag " + tagName);
                return null;
            }
            var version = match.group(1);

            if (!obj.has("assets") || !obj.get("assets").isJsonArray()) {
                LOG.warn("Manifest assets is missing or not an array");
                return null;
            }

            var assetsByPlatform = new HashMap<String, ManifestAsset>();
            for (var entryElement : obj.getAsJsonArray("assets")) {
                if (!entryElement.isJsonObject()) continue;
                var asset = entryElement.getAsJsonObject();

                if (!asset.has("name") || !asset.get("name").isJsonPrimitive()
                        || !asset.has("url") || !asset.get("url").isJsonPrimitive()) continue;
                var name = asset.getAsJsonPrimitive("name").getAsString();
                var url = asset.getAsJsonPrimitive("url").getAsString();
                
                var digest = asset.has("digest") && asset.get("digest").isJsonPrimitive() 
                        ? asset.getAsJsonPrimitive("digest").getAsString() 
                        : null;

                var platformMatch = PLATFORM_IN_ASSET_NAME.matcher(name);
                if (platformMatch.find()) {
                    assetsByPlatform.put(platformMatch.group(1), new ManifestAsset(url, digest));
                }
            }

            return new Manifest(version, Map.copyOf(assetsByPlatform));
        } catch (RuntimeException e) {
            LOG.warn("Failed to parse manifest JSON", e);
            return null;
        }
    }

    public @Nullable ManifestAsset getAsset(@NotNull String platformId) {
        return assetsByPlatform.get(platformId);
    }
}
