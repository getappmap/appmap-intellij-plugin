package appland.settings;

import appland.AppMapBundle;
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

import java.util.Objects;

@Service(Service.Level.APP)
public final class AppMapSecureApplicationSettingsService implements AppMapSecureApplicationSettings {
    public static @NotNull AppMapSecureApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppMapSecureApplicationSettingsService.class);
    }

    private volatile boolean isCached = false;
    private volatile @NotNull CachedSettings cachedSettings = new CachedSettings();

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
            PasswordSafe.getInstance().setPassword(createOpenAIKey(), key);
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
        } catch (Exception e) {
            isCached = false;
            cachedSettings = new CachedSettings();
        }
    }

    private @NotNull CachedSettings loadSettingsInBackground() {
        var openAiKey = PasswordSafe.getInstance().getPassword(createOpenAIKey());
        return new CachedSettings(openAiKey);
    }

    private @NotNull CredentialAttributes createOpenAIKey() {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("AppMap", "OpenAI"));
    }

    @AllArgsConstructor
    @Data
    private static class CachedSettings implements AppMapSecureApplicationSettings {
        @Nullable String openAIKey;

        public CachedSettings() {
            this.openAIKey = null;
        }
    }
}
