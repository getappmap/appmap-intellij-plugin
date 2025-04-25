package appland.settings;

import appland.AppMapBaseTest;
import org.junit.Test;

public class AppMapSettingsMigrationActivityTest extends AppMapBaseTest {
    @Test
    public void noMigrationIfSecureValueExists() {
        var settings = AppMapApplicationSettingsService.getInstance();
        var secureSettings = AppMapSecureApplicationSettingsService.getInstance();
        secureSettings.setModelConfigItem("OPENAI_API_KEY", "secure-openai-model-config-key");

        assertNull(settings.getCliEnvironment().get("OPENAI_API_KEY"));
        secureSettings.setOpenAIKey("migrated-key");

        AppMapSettingsMigrationActivity.migrateApplicationSettings();

        assertNull(settings.getCliEnvironment().get("OPENAI_API_KEY"));
        assertEquals("migrated-key", secureSettings.getOpenAIKey());
        assertEquals("secure-openai-model-config-key", secureSettings.getModelConfig().get("OPENAI_API_KEY"));
    }

    @Test
    public void migrateEnvironmentOpenAIKey() {
        var settings = AppMapApplicationSettingsService.getInstance();
        var secureSettings = AppMapSecureApplicationSettingsService.getInstance();

        assertNull(settings.getCliEnvironment().get("OPENAI_API_KEY"));
        secureSettings.setOpenAIKey("migrated-key");

        AppMapSettingsMigrationActivity.migrateApplicationSettings();

        assertNull(settings.getCliEnvironment().get("OPENAI_API_KEY"));
        assertNull(secureSettings.getOpenAIKey());
        assertEquals("migrated-key", secureSettings.getModelConfig().get("OPENAI_API_KEY"));
    }

    @Test
    public void migrateSecureOpenAIKey() {
        var secureSettings = AppMapSecureApplicationSettingsService.getInstance();

        assertNull(secureSettings.getModelConfig().get("OPENAI_API_KEY"));
        secureSettings.setOpenAIKey("migrated-key");

        AppMapSettingsMigrationActivity.migrateApplicationSettings();

        assertFalse(secureSettings.hasOpenAIKey());
        assertNull(secureSettings.getOpenAIKey());
        assertEquals("migrated-key", secureSettings.getModelConfig().get("OPENAI_API_KEY"));
    }
}