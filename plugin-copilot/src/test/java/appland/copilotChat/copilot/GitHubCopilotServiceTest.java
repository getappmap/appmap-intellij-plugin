package appland.copilotChat.copilot;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettingsService;
import org.junit.Test;

public class GitHubCopilotServiceTest extends AppMapBaseTest {
    @Test
    public void integrationDisabledBySettings() {
        AppMapApplicationSettingsService.getInstance().setCopilotIntegrationDisabled(true);

        assertTrue(GitHubCopilotService.getInstance().isIntegrationDisabled());
    }

    @Test
    public void integrationDisabledWithoutCopilotPlugin() {
        // Unfortunately, we can't add a dependency on the Copilot plugin in test mode because it throws an exception:
        // java.lang.ClassNotFoundException: com.github.copilot.lang.agent.CopilotAgentProcessTestService

        assertTrue("Without the GitHub Copilot plugin loaded, the integration must be reported as disabled",
                GitHubCopilotService.getInstance().isIntegrationDisabled());
    }
}
