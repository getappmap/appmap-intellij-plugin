package appland.copilotChat;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettingsService;
import org.junit.Test;

import java.util.Map;

public class CopilotAppMapEnvProviderTest extends AppMapBaseTest {
    @Test
    public void openAiKeyOverride() {
        AppMapApplicationSettingsService.getInstance().setCliEnvironment(Map.of("OPENAI_API_KEY", "custom-key"));

        var copilotEnv = new CopilotAppMapEnvProvider().getEnvironment();
        assertTrue(CopilotAppMapEnvProvider.hasCustomAppMapModelSettings());
        assertTrue("Copilot environment should be empty if a custom OpenAI API key is set", copilotEnv.isEmpty());
    }

    @Test
    public void azureOpenAiKeyOverride() {
        AppMapApplicationSettingsService.getInstance().setCliEnvironment(Map.of("AZURE_OPENAI_API_KEY", "custom-key"));

        var copilotEnv = new CopilotAppMapEnvProvider().getEnvironment();
        assertTrue(CopilotAppMapEnvProvider.hasCustomAppMapModelSettings());
        assertTrue("Copilot environment should be empty if a custom Azure OpenAI API key is set", copilotEnv.isEmpty());
    }

    @Test
    public void enabledByDefault() {
        // Unfortunately, we can't add a dependency on the Copilot plugin in test mode because it throws an exception:
        // java.lang.ClassNotFoundException: com.github.copilot.lang.agent.CopilotAgentProcessTestService

        assertFalse("By default, custom settings must not be found", CopilotAppMapEnvProvider.hasCustomAppMapModelSettings());
    }
}