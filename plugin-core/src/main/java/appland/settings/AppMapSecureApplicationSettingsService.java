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
                        .secureModelConfigChange());
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
        return cachedSettings.getOpenAIKey();
    }

    @Override
    public synchronized void setOpenAIKey(@Nullable String key) {
        updateCachedData();

        var oldValue = cachedSettings.openAIKey;
        try {
            PasswordSafe.getInstance().setPassword(createOpenAIKeyCredentials(), key);
            cachedSettings.openAIKey = key;
        } finally {
            if (!Objects.equals(oldValue, key)) {
                // notify listeners on background thread, outside the synchronized block
                ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication()
                        .getMessageBus()
                        .syncPublisher(AppMapSettingsListener.TOPIC)
                        .openAIKeyChange());

            }
        }
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
        @Nullable String openAIKey;
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
