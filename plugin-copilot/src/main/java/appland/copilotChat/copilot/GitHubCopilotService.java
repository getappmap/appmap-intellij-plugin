package appland.copilotChat.copilot;

import appland.utils.SystemProperties;
import com.esotericsoftware.kryo.kryo5.util.Null;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private final @NotNull String machineId = UUID.randomUUID().toString();

    // random ID to protect requests to our endpoint accepting requests from Navie
    public static final @NotNull String RandomIdeSessionId = UUID.randomUUID().toString();

    public final static Gson gson = new GsonBuilder().create();

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    public @NotNull Encoding loadTokenizer(@NotNull String tokenizerName) {
        var encodingType = EncodingType.fromName(tokenizerName).orElseThrow();
        return registry.getEncoding(encodingType);
    }

    public @Nullable CopilotChatSession createChat() {
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

    private @Nullable String loadGitHubOAuthToken() {
        var configPath = findGitHubCopilotConfig();
        if (configPath == null) {
            LOG.debug("Unable to find an existing GitHub Copilot configuration file.");
            return null;
        }

        try {
            var config = gson.fromJson(Files.readString(configPath), JsonObject.class);
            for (var entry : config.asMap().entrySet()) {
                if (entry.getKey().contains("github.com")) {
                    return entry.getValue().getAsJsonObject().get("oauth_token").getAsString();
                }
            }

            return null;
        } catch (IOException e) {
            LOG.debug("Failed to read GitHub Copilot config: " + configPath, e);
            return null;
        }
    }

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

    private static @NotNull String getCopilotEditorVersion() {
        var applicationInfo = ApplicationInfo.getInstance();
        return "JetBrains-"
                + applicationInfo.getBuild().getProductCode()
                + "/"
                + applicationInfo.getBuild().withoutProductCode().asString();
    }
}
