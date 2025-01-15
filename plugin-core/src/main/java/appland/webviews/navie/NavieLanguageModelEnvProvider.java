package appland.webviews.navie;

import appland.cli.AppLandCliEnvProvider;
import appland.rpcService.AppLandJsonRpcService;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSecureApplicationSettingsService;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Map;

/**
 * {@link AppLandCliEnvProvider} for Navie settings "Bring your own key" and "Use hosted AppMap LLM".
 * If keys for both settings are present, then "BYOK" is preferred.
 * <p>
 * See {@link appland.copilotChat.CopilotAppMapEnvProvider} for the Copilot integration.
 * The Copilot integration is disabled if the user has set custom model settings for Navie.
 */
public class NavieLanguageModelEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        var settings = AppMapApplicationSettingsService.getInstance();

        var openAIKey = AppMapSecureApplicationSettingsService.getInstance().getOpenAIKey();
        if (StringUtil.isNotEmpty(openAIKey)) {
            return Map.of(AppLandJsonRpcService.OPENAI_API_KEY, openAIKey);
        }

        var appMapKey = settings.getApiKey();
        if (StringUtil.isNotEmpty(appMapKey)) {
            return Map.of("APPMAP_API_KEY", appMapKey);
        }

        return Map.of();
    }
}
