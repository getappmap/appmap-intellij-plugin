package appland.settings;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Test;

import java.util.Map;

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

    @Test
    public void modelConfig() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        var settings = AppMapSecureApplicationSettingsService.getInstance();
        settings.setModelConfig(Map.of("first_key", "first_value"));
        assertEquals(Map.of("first_key", "first_value"), settings.getModelConfig());

        // settings must be properly loaded into the cache
        AppMapSecureApplicationSettingsService.reset();
        assertEquals(Map.of("first_key", "first_value"), settings.getModelConfig());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void modelConfigMustBeImmutable() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        AppMapSecureApplicationSettingsService.getInstance().getModelConfig().put("first_key", "first_value");
    }
}