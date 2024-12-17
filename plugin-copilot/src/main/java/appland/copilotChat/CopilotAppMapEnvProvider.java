package appland.copilotChat;

import appland.cli.AppLandCliEnvProvider;
import appland.copilotChat.copilot.GitHubCopilotService;
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
        if (!isGitHubCopilotEnabled()) {
            return Map.of();
        }

        return Map.of(
                "OPENAI_BASE_URL", NavieCopilotChatRequestHandler.getBaseUrl(),
                "APPMAP_NAVIE_COMPLETION_BACKEND", "openai",
                "OPENAI_API_KEY", GitHubCopilotService.RandomIdeSessionId
        );
    }

    public static boolean isGitHubCopilotEnabled() {
        return PluginManager.getLoadedPlugins()
                .stream()
                .anyMatch(plugin -> plugin.isEnabled() && plugin.getPluginId().equals(GitHubCopilotService.CopilotPluginId));
    }
}
