package appland.telemetry;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import appland.files.AppMapFileChangeListener;
import appland.files.AppMapFileEventType;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class AppMapRecordListener implements AppMapFileChangeListener {
    @NotNull private final AtomicBoolean appMapRecordedThisSession = new AtomicBoolean(false);

    @Override
    public void afterAppMapFileChange(Set<AppMapFileEventType> changeTypes) {
        if (!changeTypes.contains(AppMapFileEventType.Create) || !changeTypes.contains(AppMapFileEventType.Modify)) {
            return;
        }

        if (!appMapRecordedThisSession.compareAndSet(false, true)) {
            return;
        }

        TelemetryService.getInstance().sendEvent("appmap:record");
    }
}
