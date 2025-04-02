package appland.settings;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Application settings, which are securely stored in the {@link com.intellij.ide.passwordSafe.PasswordSafe}.
 */
public interface AppMapSecureApplicationSettings {
    /**
     * The key used to store the OpenAI key in the password safe.
     * If possible, this method should be called from a background thread.
     */
    @Nullable String getOpenAIKey();

    /**
     * Updates the value of the OpenAI key in the password safe.
     *
     * @param key New value of the OpenAI key.
     */
    void setOpenAIKey(@Nullable String key);

    default boolean hasOpenAIKey() {
        return StringUtil.isNotEmpty(getOpenAIKey());
    }
}
