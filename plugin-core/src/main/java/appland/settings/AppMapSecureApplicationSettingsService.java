package appland.settings;

import appland.AppMapBundle;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Service(Service.Level.APP)
public final class AppMapSecureApplicationSettingsService implements AppMapSecureApplicationSettings {
    public static @NotNull AppMapSecureApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppMapSecureApplicationSettingsService.class);
    }

    private volatile boolean isCached = false;
    private volatile @Nullable String cachedOpenAIKey;

    /**
     * @return The stored value of the OpenAI key. Because this method is called often
     * and is a slow operation in 2024.2+, we're caching the value to avoid too many slow calls.
     */
    @Override
    public @Nullable String getOpenAIKey() {
        if (!isCached) {
            // load the key in a background task to avoid the warning about the slow operation
            var title = AppMapBundle.get("applicationSettings.openAI.loadingKey");
            var task = new Task.WithResult<String, Exception>(null, title, false) {
                @Override
                protected String compute(@NotNull ProgressIndicator indicator) {
                    return PasswordSafe.getInstance().getPassword(createOpenAIKey());
                }
            };

            // getPassword is a slow operation in 2024.2+
            task.queue();

            try {
                cachedOpenAIKey = task.getResult();
                // we're only caching successfully retrieved key values
                isCached = true;
            } catch (Exception e) {
                isCached = false;
                Logger.getInstance(this.getClass()).warn("Failed to fetch OpenAI key", e);
                return null;
            }
        }

        return cachedOpenAIKey;
    }

    @Override
    public void setOpenAIKey(@Nullable String key) {
        var oldValue = getOpenAIKey();
        try {
            PasswordSafe.getInstance().setPassword(createOpenAIKey(), key);

            cachedOpenAIKey = key;
            isCached = true;
        } finally {
            if (!Objects.equals(oldValue, key)) {
                ApplicationManager.getApplication().getMessageBus()
                        .syncPublisher(AppMapSettingsListener.TOPIC)
                        .openAIKeyChange();
            }
        }
    }

    private @NotNull CredentialAttributes createOpenAIKey() {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("AppMap", "OpenAI"));
    }
}
