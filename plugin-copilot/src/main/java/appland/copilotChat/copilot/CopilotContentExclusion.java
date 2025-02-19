package appland.copilotChat.copilot;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public record CopilotContentExclusion(@SerializedName("rules") @NotNull List<Rule> rules,
                                      @SerializedName("last_updated_at") @NotNull String lastUpdatedAt,
                                      @SerializedName("scope") @NotNull Scope scope) {
    /**
     * Fetches the global exclusion rules from the Copilot API.
     *
     * @param gitHubToken The GitHub token to use for authentication.
     * @return The content exclusion rules.
     * @throws IOException
     */
    static public @NotNull List<CopilotContentExclusion> fetchGlobal(@NotNull String gitHubToken) throws IOException {
        return fetch(gitHubToken, Scope.ALL, null);
    }

    /**
     * Fetches the content exclusion rules from the Copilot API.
     *
     * @param gitHubToken The GitHub token to use for authentication.
     * @param scope       The scope of the content exclusion rules to fetch.
     * @param repos       The specific repositories to fetch the content exclusion rules for
     * @return The content exclusion rules.
     * @throws IOException
     */
    static public @NotNull List<CopilotContentExclusion> fetch(@NotNull String gitHubToken, @NotNull Scope scope, @Nullable List<String> repos) throws IOException {
        try {
            var response = HttpRequests.request(contentExclusionUrl(scope, repos)).tuner(connection -> {
                connection.setRequestProperty("Authorization", "Bearer " + gitHubToken);
                connection.setRequestProperty("Accept", "application/json");
            }).isReadResponseOnError(true).readString();
            return List.of(GsonUtils.GSON.fromJson(response, CopilotContentExclusion[].class));
        } catch (HttpRequests.HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                return List.of();
            }
            throw e;
        }
    }


    /**
     * @param scope The scope of the content exclusion rules to fetch.
     * @param repos The specific repositories to fetch the content exclusion rules for
     */
    static private @NotNull Url contentExclusionUrl(@NotNull Scope scope, @Nullable List<String> repos) {
        var url = Urls.parse(GitHubCopilot.INTERNAL_API_URL + "/content_exclusion", false).addParameters(Map.of("scope", scope.name().toLowerCase()));
        if (repos != null) {
            return url.addParameters(Map.of("repos", String.join(",", repos)));
        }

        return url;
    }

    public enum Scope {
        @SerializedName("repo") REPO, @SerializedName("all") ALL,
    }

    public record Rule(@SerializedName("source") @NotNull Source source,
                       @SerializedName("paths") @NotNull List<String> paths) {
    }

    public record Source(@SerializedName("name") @NotNull String name, @SerializedName("type") @NotNull String type) {
    }
}