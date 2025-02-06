package appland.copilotChat.copilot;

/**
 * Constants for our GitHub Copilot integration.
 * If the integration stops working with the remote API, we may have to update these values.
 * The values are fetched from chat completion requests send by the JetBrains Copilot plugin,
 * e.g. using a mitmweb proxy.
 */
public final class GitHubCopilot {
    public static final String LANGUAGE_SERVER_VERSION = "1.244.0";
    public static final String GITHUB_COPILOT_PLUGIN_VERSION = "copilot-intellij/1.5.29.7524";
    public static final String GITHUB_API_VERSION = "2023-07-07";
    public static final String USER_AGENT = "GithubCopilot/1.244.0";
    public static final CharSequence OPEN_AI_ORGANIZATION = "github-copilot";
    public static final CharSequence OPEN_AI_VERSION = "2020-10-01";

    public static final String CHAT_FALLBACK_MODEL_NAME = "gpt-4o";
    public static final double CHAT_DEFAULT_TEMPERATURE = 0.1;

    public static final String INTERNAL_API_URL = "https://api.github.com/copilot_internal";

    // HTTP header names used by GitHub Copilot
    public static final String HEADER_OPENAI_ORGANIZATION = "openai-organization";
    public static final String HEADER_OPENAI_VERSION = "openai-version";
    public static final String HEADER_REQUEST_ID = "x-request-id";
    public static final String HEADER_OPENAI_INTENT = "openai-intent";

    private GitHubCopilot() {
    }
}
