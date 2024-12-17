package appland.copilotChat.copilot;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Refreshed the GitHub Copilot token when it's near its expiration.
 */
class UpdatingCopilotToken {
    private static final @NotNull Logger LOG = Logger.getInstance(UpdatingCopilotToken.class);

    private static @Nullable CopilotToken fetchRawCopilotToken(@NotNull String githubToken) {
        try {
            var response = HttpRequests.request("https://api.github.com/copilot_internal/v2/token")
                    .accept("application/json")
                    .isReadResponseOnError(true)
                    .gzip(true)
                    .tuner(connection -> {
                        connection.addRequestProperty("Authorization", "Bearer " + githubToken);
                    }).readString();

            return GitHubCopilotService.gson.fromJson(response, CopilotToken.class);
        } catch (Exception e) {
            LOG.warn("Failed to fetch a new Copilot token", e);
            return null;
        }
    }

    static @Nullable UpdatingCopilotToken fetch(@NotNull String githubToken) {
        var copilotToken = fetchRawCopilotToken(githubToken);
        if (copilotToken == null) {
            return null;
        }

        return new UpdatingCopilotToken(githubToken, copilotToken);
    }

    private final @NotNull String githubToken;
    private volatile @NotNull CopilotToken token;

    UpdatingCopilotToken(@NotNull String githubToken, @NotNull CopilotToken token) {
        this.githubToken = githubToken;
        this.token = token;
    }

    @NotNull CopilotToken getToken() {
        return token;
    }

    /**
     * The HTTP authorization header to use for Copilot requests.
     * If needed, the Copilot token is updated before returning the header.
     */
    @NotNull String getAuthorizationHeader() {
        // GitHub Copilot's default seems to be 30 minutes (1800 seconds)
        var remainingSeconds = token.expiresAt() - System.currentTimeMillis() / 1000;
        if (remainingSeconds < 180) {
            LOG.debug("Copilot token is about to expire, refreshing it");
            var newToken = fetchRawCopilotToken(githubToken);
            if (newToken != null) {
                this.token = newToken;
            }
        }
        return "Bearer " + token.token();
    }
}
