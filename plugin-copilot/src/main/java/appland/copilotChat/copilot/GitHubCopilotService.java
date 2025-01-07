package appland.copilotChat.copilot;

import appland.utils.GsonUtils;
import appland.utils.SystemProperties;
import com.esotericsoftware.kryo.kryo5.util.Null;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service(Service.Level.APP)
public final class GitHubCopilotService {
    public static GitHubCopilotService getInstance() {
        return ApplicationManager.getApplication().getService(GitHubCopilotService.class);
    }

    static final Logger LOG = Logger.getInstance(GitHubCopilotService.class);

    public static PluginId CopilotPluginId = PluginId.getId("com.github.copilot");

    private static final @NotNull String machineId = UUID.randomUUID().toString();

    // random ID to protect requests to our endpoint accepting requests from Navie
    public static final @NotNull String RandomIdeSessionId = UUID.randomUUID().toString();

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    public @NotNull Encoding loadTokenizer(@NotNull String tokenizerName) {
        var encodingType = EncodingType.fromName(tokenizerName).orElseThrow();
        return registry.getEncoding(encodingType);
    }

    /**
     * Creates a new chat session with the GitHub Copilot API.
     * If the OAuth token or the Copilot token could not be retrieved, this method will return {@code null}.
     */
    public @Nullable CopilotChatSession createChatSession() {
        var githubToken = loadGitHubOAuthToken();
        if (githubToken == null) {
            return null;
        }

        var copilotToken = UpdatingCopilotToken.fetch(githubToken);
        if (copilotToken == null) {
            return null;
        }

        var apiEndpoint = copilotToken.getToken().endpoints().getOrDefault(CopilotToken.CopilotEndpoint.API, "https://api.githubcopilot.com");
        var baseHeaders = Map.of(
                "copilot-language-server-version", GitHubCopilot.LANGUAGE_SERVER_VERSION,
                "editor-plugin-version", GitHubCopilot.GITHUB_COPILOT_PLUGIN_VERSION,
                "editor-version", getCopilotEditorVersion(),
                "user-agent", GitHubCopilot.USER_AGENT,
                "x-github-api-version", GitHubCopilot.GITHUB_API_VERSION,
                "vscode-machineid", machineId,
                "vscode-sessionid", UUID.randomUUID().toString()
        );
        return new CopilotChatSession(apiEndpoint, copilotToken, baseHeaders);
    }

    /**
     * @return The value of the "oauth_token" field in the GitHub Copilot configuration file, or {@code null} if it could not be found.
     */
    private @Nullable String loadGitHubOAuthToken() {
        var configPath = findGitHubCopilotConfig();
        if (configPath == null) {
            LOG.debug("Unable to find an existing GitHub Copilot configuration file.");
            return null;
        }

        try {
            LOG.debug("Reading GitHub Copilot config: " + configPath);

            var config = GsonUtils.GSON.fromJson(Files.readString(configPath, StandardCharsets.UTF_8), JsonObject.class);
            for (var entry : config.asMap().entrySet()) {
                if (entry.getKey().startsWith("github.com") && entry.getValue().isJsonObject()) {
                    var oauthToken = entry.getValue().getAsJsonObject().getAsJsonPrimitive("oauth_token");
                    if (oauthToken != null) {
                        return oauthToken.getAsString();
                    }
                }
            }

            LOG.debug("No GitHub OAuth token found in GitHub Copilot config: " + configPath);
            return null;
        } catch (IOException e) {
            LOG.debug("Failed to read GitHub Copilot config: " + configPath, e);
            return null;
        }
    }

    /**
     * @return The {@link Path} to the GitHub Copilot plugin's configuration file, or {@code null} if it could not be found.
     */
    private static @Null Path findGitHubCopilotConfig() {
        var basePath = Path.of(SystemProperties.getUserHome(), ".config", "github-copilot");
        if (!Files.isDirectory(basePath)) {
            return null;
        }

        var hosts = basePath.resolve("hosts.json");
        if (Files.isRegularFile(hosts)) {
            return hosts;
        }

        var apps = basePath.resolve("apps.json");
        if (Files.isRegularFile(apps)) {
            return apps;
        }

        return null;
    }

    /**
     * @return The version of the Copilot editor in a format compatible with the GitHub Copilot plugin.
     * For example: <code>JetBrains-IC/231.8109.175</code>.
     */
    private static @NotNull String getCopilotEditorVersion() {
        var applicationInfo = ApplicationInfo.getInstance();
        return "JetBrains-"
                + applicationInfo.getBuild().getProductCode()
                + "/"
                + applicationInfo.getBuild().withoutProductCode().asString();
    }
}
