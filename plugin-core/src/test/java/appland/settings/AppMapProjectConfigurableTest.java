package appland.settings;

import appland.AppMapBaseTest;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class AppMapProjectConfigurableTest extends AppMapBaseTest {
    @Test
    public void isModified() {
        AppMapSecureApplicationSettings secureService = AppMapSecureApplicationSettingsService.getInstance();
        var oldKeyValue = secureService.getOpenAIKey();

        try {
            secureService.setOpenAIKey("secure-key-value");

            var inlineCopy = new AppMapProjectConfigurable.InlineSecureApplicationSettings(secureService);

            var newInlineSettings = new AppMapProjectConfigurable.InlineSecureApplicationSettings();
            newInlineSettings.setOpenAIKey("secure-key-value");
            assertEquals("Inline settings must be equal for the same OpenAI key", inlineCopy, newInlineSettings);

            newInlineSettings.setOpenAIKey("update-value");
            assertNotEquals("Inline settings must not be equal after a change to the OpenAI key", inlineCopy, newInlineSettings);
        } finally {
            secureService.setOpenAIKey(oldKeyValue);
        }
    }
}