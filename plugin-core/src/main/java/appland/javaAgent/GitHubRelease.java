package appland.javaAgent;

import appland.github.GitHubHttpRequests;
import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.progress.ProgressIndicator;
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
                .tuner(GitHubHttpRequests.gitHubTokenTuner())
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

