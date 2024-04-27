package appland.settings;

import org.jetbrains.annotations.Nullable;

public interface AppMapSecureApplicationSettings {
    @Nullable
    String getOpenAIKey();

    void setOpenAIKey(@Nullable String key);
}
