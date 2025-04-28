package appland.settings;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Application settings, which are securely stored in the {@link com.intellij.ide.passwordSafe.PasswordSafe}.
 */
public interface AppMapSecureApplicationSettings {
    /**
     * The key used to store the OpenAI key in the password safe.
     * If possible, this method should be called from a background thread.
     */
    @Deprecated
    @Nullable String getOpenAIKey();

    /**
     * Updates the value of the OpenAI key in the password safe.
     *
     * @param key New value of the OpenAI key.
     */
    @Deprecated
    void setOpenAIKey(@Nullable String key);

    @Deprecated
    default boolean hasOpenAIKey() {
        return StringUtil.isNotEmpty(getOpenAIKey());
    }

    /**
     * @return The complete data of the currently stored model configuration settings.
     */
    @NotNull Map<String, String> getModelConfig();

    /**
     * Updates one item of the model configuration settings.
     *
     * @param key   The key of the item to update.
     * @param value The new value of the item. If null, the item will be removed.
     */
    void setModelConfigItem(@NotNull String key, @Nullable String value);
}
