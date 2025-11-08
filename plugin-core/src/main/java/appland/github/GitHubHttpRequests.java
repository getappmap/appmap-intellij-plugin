package appland.github;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

public final class GitHubHttpRequests {
    private GitHubHttpRequests() {
    }

    private static final HttpRequests.ConnectionTuner NO_OP_TUNER = connection -> {
    };

    /**
     * Accessing api.github.com via GitHub actions often returns a 403,
     * we're using the token to get a higher request rate limit.
     */
    public static @NotNull HttpRequests.ConnectionTuner gitHubTokenTuner() {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            return NO_OP_TUNER;
        }

        // The system property is set by our Gradle build setup.
        // https://docs.github.com/en/rest/overview/resources-in-the-rest-api?apiVersion=2022-11-28#rate-limits-for-requests-from-github-actions
        var apiToken = System.getProperty("appland.github_token");
        if (StringUtil.isEmpty(apiToken)) {
            return NO_OP_TUNER;
        }

        return connection -> connection.setRequestProperty("authorization", "Bearer " + apiToken);
    }
}
