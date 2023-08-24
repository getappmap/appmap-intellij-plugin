package appland.javaAgent;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.HttpRequests;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class GitHubRelease {
    @SuppressWarnings("SameParameterValue")
    static List<GitHubReleaseAsset> getLatestRelease(@NotNull ProgressIndicator progressIndicator,
                                                     @NotNull String repoOwner,
                                                     @NotNull String repoName) throws IOException {
        var url = String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName);
        var response = HttpRequests
                .request(url)
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
}

@Value
class GitHubReleaseResponse {
    @SerializedName("assets")
    GitHubReleaseAsset[] assets;
}

@Value
class GitHubReleaseAsset {
    @SerializedName("name")
    String fileName;
    @SerializedName("content_type")
    String contentType;
    @SerializedName("browser_download_url")
    String downloadUrl;

    void download(@NotNull ProgressIndicator progressIndicator, @NotNull Path targetFilePath) throws IOException {
        HttpRequests.request(this.downloadUrl).saveToFile(targetFilePath, progressIndicator);
    }
}