package appland.copilotChat.copilot;

import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public record CopilotContentExclusion(@SerializedName("rules") List<Rule> rules,
                                      @SerializedName("last_updated_at") String lastUpdatedAt,
                                      @SerializedName("scope") Scope scope) {
    /**
     * Fetches the global exclusion rules from the Copilot API.
     *
     * @param gitHubToken The GitHub token to use for authentication.
     * @return The content exclusion rules.
     * @throws IOException
     */
    static public List<CopilotContentExclusion> fetchGlobal(String gitHubToken) throws IOException {
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
    static public List<CopilotContentExclusion> fetch(String gitHubToken, Scope scope, @Nullable List<String> repos) throws IOException {
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
    static private Url contentExclusionUrl(Scope scope, @Nullable List<String> repos) {
        var url = Urls.parse("https://api.github.com/copilot_internal/content_exclusion", false).addParameters(Map.of("scope", scope.name().toLowerCase()));
        if (repos != null) {
            return url.addParameters(Map.of("repos", String.join(",", repos)));
        }

        return url;
    }

    public enum Scope {
        @SerializedName("repo") REPO, @SerializedName("all") ALL,
    }

    public record Rule(@SerializedName("source") Source source, @SerializedName("paths") List<String> paths) {
    }

    public record Source(@SerializedName("name") String name, @SerializedName("type") String type) {
    }
}