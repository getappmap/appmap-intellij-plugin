package appland.copilotChat;

import appland.cli.AppLandCliEnvProvider;
import appland.copilotChat.copilot.GitHubCopilotService;
import appland.rpcService.AppLandJsonRpcService;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.ide.plugins.PluginManager;

import java.util.Map;

/**
 * Extends the environment setup of AppMap CLI commands with the values to use this plugin's GitHub Copilot integration.
 * <p>
 * We're not using an optional plugin dependency to avoid complicating our build setup.
 */
public class CopilotAppMapEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        if (hasCustomModelSettings() || isGitHubCopilotDisabled()) {
            return Map.of();
        }

        return Map.of(
                "OPENAI_BASE_URL", NavieCopilotChatRequestHandler.getBaseUrl(),
                "APPMAP_NAVIE_COMPLETION_BACKEND", "openai",
                "OPENAI_API_KEY", GitHubCopilotService.RandomIdeSessionId
        );
    }

    public static boolean isGitHubCopilotIntegrationEnabled() {
        return !hasCustomModelSettings() && !isGitHubCopilotDisabled();
    }

    /**
     * @return {@code true} if the user has set custom model settings for Navie for an OpenAI or Azure OpenAI API key.
     * Existing settings for a custom key override the Copilot integration.
     */
    public static boolean hasCustomModelSettings() {
        var environment = AppMapApplicationSettingsService.getInstance().getCliEnvironment();
        return environment.containsKey(AppLandJsonRpcService.OPENAI_API_KEY)
                || environment.containsKey(AppLandJsonRpcService.AZURE_OPENAI_API_KEY);
    }

    public static boolean isGitHubCopilotDisabled() {
        if (AppMapApplicationSettingsService.getInstance().isCopilotIntegrationDisabled()) {
            return true;
        }

        return PluginManager.getLoadedPlugins()
                .stream()
                .noneMatch(plugin -> plugin.isEnabled() && plugin.getPluginId().equals(GitHubCopilotService.CopilotPluginId));
    }
}
