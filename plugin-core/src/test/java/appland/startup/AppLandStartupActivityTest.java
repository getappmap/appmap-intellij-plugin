package appland.startup;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettingsService;
import org.junit.Test;

public class AppLandStartupActivityTest extends AppMapBaseTest {
    @Test
    public void instructionsAfterFirstSignIn() {
        var settings = AppMapApplicationSettingsService.getInstance();
        assertFalse("Flag must default to false", settings.isInstallInstructionsViewed());

        // updating the API key must trigger the listener to open the installation instructions page
        settings.setApiKeyNotifying("some-api-key");
        assertTrue("Flag must be true after user signed in", settings.isInstallInstructionsViewed());
    }
}