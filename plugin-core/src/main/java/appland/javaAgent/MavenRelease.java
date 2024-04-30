package appland.javaAgent;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

enum MavenRelease implements Release {
    INSTANCE;

    private final Pattern RELEASE_PATTERN = Pattern.compile("<release>(.*?)</release>");
    private final String DOWNLOAD_HOST = "repo.maven.apache.org";
    private final String MAVEN_PREFIX = "https://" + DOWNLOAD_HOST + "/maven2/com/appland/appmap-agent";
    private final String URL = MAVEN_PREFIX + "/maven-metadata.xml";

    @Override
    public String getDownloadHost() {
        return DOWNLOAD_HOST;
    }

    public List<Asset> getLatest(@NotNull ProgressIndicator progressIndicator) throws IOException {
        var response = HttpRequests
                .request(URL)
                .readString(progressIndicator);
        var m = RELEASE_PATTERN.matcher(response);
        if (!m.find()) {
            throw new IOException("Release not found in Maven metadata");
        }
        var version = m.group(1);
        var asset = new Asset(
                String.format("appmap-agent-%s.jar", version),
                "application/java-archive",
                String.format("%s/%s/appmap-agent-%s.jar", MAVEN_PREFIX, version, version)
        );
        return List.of(asset);
    }

    @Override
    public String toString() {
        return "{MavenRelease: " + URL + "}";
    }
}
