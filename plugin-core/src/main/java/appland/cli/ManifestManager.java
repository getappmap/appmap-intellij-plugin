package appland.cli;

import appland.settings.DownloadSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ManifestManager {
    private static final Logger LOG = Logger.getInstance(ManifestManager.class);
    private static final Map<String, Manifest> CACHE = new ConcurrentHashMap<>();

    private ManifestManager() {
    }

    public static @Nullable Manifest fetch(@NotNull CliTool type) {
        return fetch(DownloadSettings.getManifestUrl(type));
    }

    public static @Nullable Manifest fetch(@NotNull String urlString) {
        var cached = CACHE.get(urlString);
        if (cached != null) return cached;

        try {
            var url = Urls.newFromEncoded(urlString);
            var jsonString = HttpRequests.request(url).readString(null);
            var manifest = Manifest.parse(jsonString);
            
            if (manifest != null) {
                LOG.info("Fetched manifest from " + urlString + " (version " + manifest.version + ")");
                CACHE.put(urlString, manifest);
            }
            
            return manifest;
        } catch (IOException e) {
            LOG.warn("Failed to fetch manifest from " + urlString + ": " + e.toString());
            // Don't cache failures so a transient outage doesn't disable discovery until restart
            return null;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
