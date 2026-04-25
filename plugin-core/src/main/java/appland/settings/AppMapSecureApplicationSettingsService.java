package appland.settings;

import appland.AppMapBundle;
import appland.utils.GsonUtils;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.text.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service(Service.Level.APP)
public final class AppMapSecureApplicationSettingsService implements AppMapSecureApplicationSettings {
    public static @NotNull AppMapSecureApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppMapSecureApplicationSettingsService.class);
    }

    // Key to store the OpenAI API key in the model config. This must match the value used by the AppMap webview.
    static final String MODEL_CONFIG_OPENAI_API_KEY = "OPENAI_API_KEY";

    private volatile boolean isCached = false;
    private volatile @NotNull CachedSettings cachedSettings = new CachedSettings();

    @TestOnly
    public static void reset() {
        resetCache();

        var passwordSafe = PasswordSafe.getInstance();
        var service = (AppMapSecureApplicationSettingsService) getInstance();
        passwordSafe.setPassword(service.createOpenAIKeyCredentials(), null);
        passwordSafe.setPassword(service.createModelConfigCredentials(), GsonUtils.GSON.toJson(Map.of()));
    }

    @TestOnly
    static void resetCache() {
        var service = (AppMapSecureApplicationSettingsService) getInstance();
        service.isCached = false;
        service.cachedSettings = new CachedSettings();
    }

    @Override
    public synchronized @NotNull Map<String, String> getModelConfig() {
        updateCachedData();
        return cachedSettings.getModelConfig();
    }

    @Override
    public synchronized void setModelConfigItem(@NotNull String key, @Nullable String value) {
        var oldModelConfig = getModelConfig();
        var oldValue = oldModelConfig.get(key);

        var updatedModelConfig = new HashMap<>(oldModelConfig);
        if (value == null) {
            updatedModelConfig.remove(key);
        } else {
            updatedModelConfig.put(key, value);
        }

        try {
            PasswordSafe.getInstance().setPassword(createModelConfigCredentials(), GsonUtils.GSON.toJson(updatedModelConfig));
            cachedSettings.setModelConfig(updatedModelConfig);
        } finally {
            if (!Objects.equals(oldValue, value)) {
                // notify listeners on background thread, outside the synchronized block
                ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication()
                        .getMessageBus()
                        .syncPublisher(AppMapSettingsListener.TOPIC)
                        .modelConfigChange());
            }
        }
    }

    /**
     * @return The stored value of the OpenAI key. Because this method is called often
     * and is a slow operation in 2024.2+, we're caching the value to avoid too many slow calls.
     */
    @Override
    public synchronized @Nullable String getOpenAIKey() {
        updateCachedData();

        // The key is stored in the model config settings to sync with the webview.
        // But because it previously was stored as a separate value, we fall back to this key to migrate old settings.
        var modelConfigValue = cachedSettings.getModelConfig().get(MODEL_CONFIG_OPENAI_API_KEY);
        var fallbackValue = cachedSettings.openAIKey;
        return StringUtil.defaultIfEmpty(modelConfigValue, fallbackValue);
    }

    @Override
    public synchronized void setOpenAIKey(@Nullable String key) {
        updateCachedData();

        // We always store the OpenAI key in the model config settings.
        // The fallback to the old, deprecated value is done in getOpenAIKey.
        setModelConfigItem(MODEL_CONFIG_OPENAI_API_KEY, key);
    }

    private synchronized void updateCachedData() {
        if (isCached) {
            return;
        }

        try {
            if (ApplicationManager.getApplication().isDispatchThread()) {
                var title = AppMapBundle.get("applicationSettings.loadingSecureSettings");
                cachedSettings = ProgressManager.getInstance().run(new Task.WithResult<CachedSettings, Exception>(null, title, false) {
                    @Override
                    protected CachedSettings compute(@NotNull ProgressIndicator indicator) {
                        return loadSettingsInBackground();
                    }
                });
            } else {
                cachedSettings = loadSettingsInBackground();
            }

            isCached = true;
        } catch (Exception e) {
            isCached = false;
            cachedSettings = new CachedSettings();
        }
    }

    @SuppressWarnings("unchecked")
    private @NotNull CachedSettings loadSettingsInBackground() {
        var passwordSafe = PasswordSafe.getInstance();

        var openAiKey = passwordSafe.getPassword(createOpenAIKeyCredentials());
        var modelConfig = GsonUtils.GSON.fromJson(passwordSafe.getPassword(createModelConfigCredentials()), Map.class);
        return new CachedSettings(openAiKey, modelConfig);
    }

    private @NotNull CredentialAttributes createOpenAIKeyCredentials() {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("AppMap", "OpenAI"));
    }

    private @NotNull CredentialAttributes createModelConfigCredentials() {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("AppMap", "ModelConfig"));
    }

    @AllArgsConstructor
    @Data
    private static class CachedSettings {
        // fixme Drop this in a few months after most users updated to a version storing the value in modelConfig
        @Deprecated @Nullable String openAIKey;
        @Nullable Map<String, String> modelConfig;

        public CachedSettings() {
            this.openAIKey = null;
        }

        public @NotNull Map<String, String> getModelConfig() {
            return modelConfig == null ? Map.of() : Map.copyOf(modelConfig);
        }

        public void setModelConfig(@Nullable Map<String, String> modelConfig) {
            this.modelConfig = modelConfig == null ? Map.of() : Map.copyOf(modelConfig);
        }
    }
}
