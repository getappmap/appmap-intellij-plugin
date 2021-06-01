package appland.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "appmap-application", storages = @Storage("appmap-application.xml"))
public class AppMapApplicationSettingsService implements PersistentStateComponent<AppMapApplicationSettings> {
    private AppMapApplicationSettings state = new AppMapApplicationSettings();

    @NotNull
    public static AppMapApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppMapApplicationSettingsService.class).state;
    }

    @Override
    public @Nullable AppMapApplicationSettings getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull AppMapApplicationSettings state) {
        this.state = state;
    }
}
