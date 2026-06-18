package appland.enterpriseConfig;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Local, non-roaming persistence for the cached organization-configuration JSON.
 *
 * <p>The cached configuration can contain a Splunk telemetry token. It is stored here, in a storage
 * with {@link RoamingType#DISABLED}, rather than in the roaming application settings, so the token is
 * not synced to JetBrains cloud / other machines via Settings Sync. It is still persisted locally so
 * the configuration survives restarts and serves as an offline fallback.
 */
@State(name = "appmap-enterprise-config-cache",
        storages = @Storage(value = "appmap-enterprise-config.xml", roamingType = RoamingType.DISABLED))
public final class EnterpriseConfigCacheService implements PersistentStateComponent<EnterpriseConfigCacheService.State> {

    public static final class State {
        // volatile: written under EnterpriseConfigService's apply lock but read off-lock (the status
        // report on the EDT, and startup), so a plain field could expose a stale value across threads.
        public volatile @Nullable String cacheJson = null;
    }

    private @NotNull State state = new State();

    public static @NotNull EnterpriseConfigCacheService getInstance() {
        return ApplicationManager.getApplication().getService(EnterpriseConfigCacheService.class);
    }

    public @Nullable String getCacheJson() {
        return state.cacheJson;
    }

    public void setCacheJson(@Nullable String cacheJson) {
        state.cacheJson = cacheJson;
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }
}
