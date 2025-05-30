package appland.javaAgent;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.HttpRequests;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

enum GitHubRelease implements Release {
    INSTANCE;
    private static final String DOWNLOAD_HOST = "github.com";
    private static final String URL = "https://api.github.com/repos/getappmap/appmap-java/releases/latest";

    @Override
    public String getDownloadHost() {
        return DOWNLOAD_HOST;
    }

    public List<Asset> getLatest(@NotNull ProgressIndicator progressIndicator) throws IOException {
        var response = HttpRequests
                .request(URL)
                .tuner(connection -> {
                    // Accessing api.github.com via GitHub actions often returns a 403,
                    // we're using the token to get a higher request rate limit.
                    // The system property is set by our Gradle build setup.
                    // https://docs.github.com/en/rest/overview/resources-in-the-rest-api?apiVersion=2022-11-28#rate-limits-for-requests-from-github-actions
                    var apiToken = System.getProperty("appland.github_token");
                    if (StringUtil.isNotEmpty(apiToken)) {
                        connection.setRequestProperty("authorization", "Bearer " + apiToken);
                    }
                })
                .readString(progressIndicator);
        return List.of(GsonUtils.GSON.fromJson(response, GitHubReleaseResponse.class).getAssets());
    }

    @Override
    public String toString() {
        return "{GitHubRelease: " + URL + "}";
    }
}

@Value
class GitHubReleaseResponse {
    @SerializedName("assets")
    Release.Asset[] assets;
}

