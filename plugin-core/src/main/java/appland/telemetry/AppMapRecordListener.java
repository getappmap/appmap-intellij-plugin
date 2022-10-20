package appland.telemetry;

import appland.files.AppMapFileChangeListener;
import appland.files.AppMapFileEventType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppMapRecordListener implements AppMapFileChangeListener {
    @NotNull private final AtomicBoolean appMapRecordedThisSession = new AtomicBoolean(false);

    @Override
    public void refreshAppMaps(@NotNull Set<AppMapFileEventType> changeTypes) {
        if (!changeTypes.contains(AppMapFileEventType.Create) || !changeTypes.contains(AppMapFileEventType.Modify)) {
            return;
        }

        if (!appMapRecordedThisSession.compareAndSet(false, true)) {
            return;
        }

        TelemetryService.getInstance().sendEvent("appmap:create");
    }
}
