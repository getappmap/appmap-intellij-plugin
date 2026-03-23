package appland.settings;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class AppMapSecureApplicationSettingsServiceTest extends AppMapBaseTest {
    @Test
    public void openAiKey() throws Exception {
        ApplicationManager.getApplication().assertIsDispatchThread();

        var settings = AppMapSecureApplicationSettingsService.getInstance();

        // if executed on the EDT, it must not throw an exception about slow operations
        var oldValue = settings.getOpenAIKey();
        try {
            var condition = subscribeToModelConfig("OPENAI_API_KEY");
            settings.setOpenAIKey("my-new-key");

            condition.await(15, TimeUnit.SECONDS);
        } finally {
            settings.setOpenAIKey(oldValue);
        }
    }

    @Test
    public void modelConfig() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        var settings = AppMapSecureApplicationSettingsService.getInstance();
        settings.setModelConfigItem("first_key", "first_value");
        assertEquals(Map.of("first_key", "first_value"), settings.getModelConfig());

        // settings must be properly loaded into the cache
        AppMapSecureApplicationSettingsService.resetCache();
        assertEquals(Map.of("first_key", "first_value"), settings.getModelConfig());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void modelConfigMustBeImmutable() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        AppMapSecureApplicationSettingsService.getInstance().getModelConfig().put("first_key", "first_value");
    }

    @Test
    public void modelConfigListenerForNewKey() throws Exception {
        var condition = subscribeToModelConfig("first_key");
        AppMapSecureApplicationSettingsService.getInstance().setModelConfigItem("first_key", "first_value");
        assertTrue(condition.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void modelConfigListenerForUpdatedValue() throws Exception {
        AppMapSecureApplicationSettingsService.getInstance().setModelConfigItem("first_key", "first_value");

        var condition = subscribeToModelConfig("first_key");
        AppMapSecureApplicationSettingsService.getInstance().setModelConfigItem("first_key", "updated_value");
        assertTrue(condition.await(15, TimeUnit.SECONDS));
    }

    private @NotNull CountDownLatch subscribeToModelConfig(@NotNull String expectedKey) {
        var condition = new CountDownLatch(1);
        var listener = new AppMapSettingsListener() {
            @Override
            public void secureModelConfigChange(@NotNull String key) {
                ApplicationManager.getApplication().assertIsNonDispatchThread();
                condition.countDown();

                if (!expectedKey.equals(key)) {
                    LOG.error("Expected key: " + expectedKey + ", found: " + key);
                }
            }
        };

        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, listener);
        return condition;
    }
}