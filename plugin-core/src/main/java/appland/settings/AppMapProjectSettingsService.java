package appland.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(name = "appmap-project", storages = @Storage("appmap.xml"))
public final class AppMapProjectSettingsService implements PersistentStateComponent<AppMapProjectSettings> {
    @NotNull
    private volatile AppMapProjectSettings state = new AppMapProjectSettings();

    public static AppMapProjectSettings getState(@NotNull Project project) {
        return project.getService(AppMapProjectSettingsService.class).getState();
    }

    @Override
    public @NotNull AppMapProjectSettings getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull AppMapProjectSettings state) {
        this.state = state;
    }
}
