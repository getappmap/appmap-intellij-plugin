package appland.copilotChat.copilot;

import appland.notifications.AppMapNotifications;
import appland.utils.GsonUtils;
import appland.utils.SystemProperties;
import appland.utils.UserLog;
import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.EnvironmentUtil;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service(Service.Level.APP)
public final class GitHubCopilotService implements Disposable {
    GitHubCopilotService() {
        Disposer.register(this, userLog);
    }

    public static GitHubCopilotService getInstance() {
        return ApplicationManager.getApplication().getService(GitHubCopilotService.class);
    }

    static final Logger LOG = Logger.getInstance(GitHubCopilotService.class);

    public static PluginId CopilotPluginId = PluginId.getId("com.github.copilot");

    private static final @NotNull String machineId = UUID.randomUUID().toString();

    // random ID to protect requests to our endpoint accepting requests from Navie
    public static final @NotNull String RandomIdeSessionId = UUID.randomUUID().toString();

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    private final @NotNull UserLog userLog = new UserLog("appmap-copilot-proxy.log");

    @Override
    public void dispose() {
        // no-op, the disposer will take care of disposing the user log
    }

    public boolean isCopilotAuthenticated() {
        return loadGitHubOAuthToken() != null;
    }

    public @NotNull Encoding loadTokenizer(@NotNull String tokenizerName) {
        var encodingType = EncodingType.fromName(tokenizerName).orElseThrow();
        return registry.getEncoding(encodingType);
    }

    /**
     * Whether the content exclusions have been downloaded.
     */
    private boolean contentExclusionsDownloaded = false;

    /**
     * Downloads the content exclusions from the GitHub Copilot API and writes them to the global ignores file.
     */
    private synchronized void downloadContentExclusions(@NotNull String githubToken) throws IOException {
        if (contentExclusionsDownloaded) return;
        userLog.log("Downloading content exclusions");
        var exclusions = CopilotContentExclusion.fetchGlobal(githubToken);
        var paths = new ArrayList<String>();
        for (var exclusion : exclusions) {
            LOG.debug("Downloaded content exclusion: " + exclusion);
            for (var rule : exclusion.rules())
                paths.addAll(rule.paths());
        }
        var path = contentExclusionPath();
        Files.createDirectories(path.getParent());
        Files.write(path, paths);
        userLog.log("Downloaded content exclusions to " + path);
        contentExclusionsDownloaded = true;
    }

    /**
     * Ensures that the content exclusions have been downloaded.
     */
    public void ensureContentExclusionsDownloaded() throws IOException {
        if (contentExclusionsDownloaded) return;
        var githubToken = loadGitHubOAuthToken();
        if (githubToken != null) ensureContentExclusionsDownloaded(githubToken);
    }

    /**
     * Ensures that the content exclusions have been downloaded.
     */
    private void ensureContentExclusionsDownloaded(@NotNull String githubToken) throws IOException {
        try {
            downloadContentExclusions(githubToken);
        } catch (IOException e) {
            userLog.log("Failed to download content exclusions: " + e);
            LOG.warn("Failed to download content exclusions", e);
            throw e;
        }
    }

    /**
     * @return The path to the global ignores file.
     */
    private static @NotNull Path contentExclusionPath() {
        return Path.of(SystemProperties.getUserHome(), ".appmap", "navie", "global-ignore");
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

        try {
            ensureContentExclusionsDownloaded(githubToken);
        } catch (IOException e) {
            ApplicationManager.getApplication().invokeLater(() -> AppMapNotifications.showCopilotExclusionDownloadFailed(e));
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

        userLog.log("Creating chat session with GitHub Copilot API");
        return new CopilotChatSession(apiEndpoint, copilotToken, baseHeaders, userLog);
    }

    /**
     * @return The value of the "oauth_token" field in the GitHub Copilot configuration file, or {@code null} if it could not be found.
     */
    private @Nullable String loadGitHubOAuthToken() {
        var configPath = findGitHubCopilotConfig();
        if (configPath == null) {
            LOG.debug("Unable to find an existing GitHub Copilot configuration file.");
            userLog.log("Unable to find an existing GitHub Copilot configuration file.");
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
    private static @Nullable Path findGitHubCopilotConfig() {
        var basePath = findCopilotSettingsDirectory();
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
     * @return The {@link java.nio.file.Path} to the GitHub Copilot base directory
     */
    private static @NotNull Path findCopilotSettingsDirectory() {
        Path localAppDataPath;
        if (SystemInfo.isWindows) {
            // Windows: %LOCALAPPDATA%\AppData\Local\github-copilot
            var localAppData = EnvironmentUtil.getValue("LOCALAPPDATA");
            localAppDataPath = StringUtil.isNotEmpty(localAppData)
                    ? Path.of(localAppData)
                    : Path.of(SystemProperties.getUserHome(), "AppData", "Local");
        } else {
            // Linux and macOS: ~/.config/github-copilot
            localAppDataPath = Path.of(SystemProperties.getUserHome(), ".config");
        }

        return localAppDataPath.resolve("github-copilot");
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
