package appland.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Service(Service.Level.APP)
public final class AppMapSecureApplicationSettingsService implements AppMapSecureApplicationSettings {
    public static @NotNull AppMapSecureApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppMapSecureApplicationSettingsService.class);
    }

    @Override
    public @Nullable String getOpenAIKey() {
        return PasswordSafe.getInstance().getPassword(createOpenAIKey());
    }

    @Override
    public void setOpenAIKey(@Nullable String key) {
        var oldValue = getOpenAIKey();
        try {
            PasswordSafe.getInstance().setPassword(createOpenAIKey(), key);
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
