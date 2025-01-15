package appland.settings;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

public interface AppMapSecureApplicationSettings {
    @Nullable
    String getOpenAIKey();

    void setOpenAIKey(@Nullable String key);

    default boolean hasOpenAIKey() {
        return StringUtil.isNotEmpty(getOpenAIKey());
    }
}
