package appland.javaAgent;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.progress.ProgressIndicator;
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
        var response = HttpRequests.request(url).readString(progressIndicator);
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