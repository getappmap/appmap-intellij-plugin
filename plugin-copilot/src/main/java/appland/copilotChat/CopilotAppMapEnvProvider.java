package appland.copilotChat;

import appland.cli.AppLandCliEnvProvider;
import appland.copilotChat.copilot.GitHubCopilotService;
import appland.rpcService.AppLandJsonRpcService;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSecureApplicationSettingsService;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.extensions.PluginDescriptor;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Extends the environment setup of AppMap CLI commands with the values to use this plugin's GitHub Copilot integration.
 * <p>
 * We're not using an optional plugin dependency to avoid complicating our build setup.
 */
public class CopilotAppMapEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        if (isDisabled()) {
            return Map.of();
        }

        if (!GitHubCopilotService.getInstance().isCopilotAuthenticated()) {
            return Map.of();
        }

        var userCopilotModel = AppMapApplicationSettingsService.getInstance().getCopilotModelId();

        var env = new HashMap<String, String>();
        env.put("OPENAI_BASE_URL", NavieCopilotChatRequestHandler.getBaseUrl());
        env.put("APPMAP_NAVIE_COMPLETION_BACKEND", "openai");
        env.put("OPENAI_API_KEY", GitHubCopilotService.RandomIdeSessionId);
        if (StringUtil.isNotEmpty(userCopilotModel)) {
            env.put("APPMAP_NAVIE_MODEL", userCopilotModel);
        }
       return Map.copyOf(env);
    }

    /**
     * @return {@code true} if the integration with GitHub Copilot is unavailable,
     * either because the user chose a different LLM in Navie or because the GitHub Copilot plugin is not installed.
     * This method must not evaluate the state of Copilot authentication.
     */
    public static boolean isDisabled() {
        return hasCustomAppMapModelSettings() || isGitHubCopilotDisabled();
    }

    /**
     * @return {@code true} if the user has set custom model settings for Navie for an OpenAI or Azure OpenAI API key.
     * Existing settings for a custom key override the Copilot integration.
     */
    static boolean hasCustomAppMapModelSettings() {
        var environment = AppMapApplicationSettingsService.getInstance().getCliEnvironment();
        return AppMapSecureApplicationSettingsService.getInstance().hasOpenAIKey()
                || environment.containsKey(AppLandJsonRpcService.OPENAI_API_KEY)
                || environment.containsKey(AppLandJsonRpcService.OPENAI_BASE_URL)
                || environment.containsKey(AppLandJsonRpcService.AZURE_OPENAI_API_KEY);
    }

    /**
     * @return The GitHub Copilot plugin descriptor, or {@code null} if it's not installed.
     */
    public static @Nullable PluginDescriptor getCopilotPlugin() {
        return PluginManager.getLoadedPlugins()
                .stream()
                .filter(plugin -> plugin.isEnabled() && plugin.getPluginId().equals(GitHubCopilotService.CopilotPluginId))
                .findFirst()
                .orElse(null);
    }

    private static boolean isGitHubCopilotDisabled() {
        if (AppMapApplicationSettingsService.getInstance().isCopilotIntegrationDisabled()) {
            return true;
        }

        return getCopilotPlugin() == null;
    }
}
