package appland.settings;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Test;

public class AppMapSecureApplicationSettingsServiceTest extends AppMapBaseTest {
    @Test
    public void openAiKey() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        var settings = AppMapSecureApplicationSettingsService.getInstance();

        // if executed on the EDT, it must not throw an exception about slow operations
        var oldValue = settings.getOpenAIKey();
        try {
            settings.setOpenAIKey("my-new-key");
        } finally {
            settings.setOpenAIKey(oldValue);
        }
    }
}