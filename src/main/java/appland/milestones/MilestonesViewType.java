package appland.milestones;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public enum MilestonesViewType {
    InstallAgent("INSTALL_AGENT"),
    RecordAppMaps("RECORD_APPMAPS"),
    AppMapsTable("OPEN_APPMAPS");

    // value used by the JS application for "transition" messages
    private final String transitionValue;

    MilestonesViewType(String transitionValue) {
        this.transitionValue = transitionValue;
    }

    @Nullable
    public static MilestonesViewType findByTransitionTarget(@NotNull String target) {
        for (MilestonesViewType type : MilestonesViewType.values()) {
            if (type.transitionValue.equals(target)) {
                return type;
            }
        }
        return null;
    }

    public Path getHTMLPath() {
        switch (this) {
            case InstallAgent:
                return AppMapPlugin.getInstallAgentHTMLPath();
            case RecordAppMaps:
                return AppMapPlugin.getRecordAppMapsHTMLPath();
            case AppMapsTable:
                return AppMapPlugin.getAppMapsTableHTMLPath();
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }

    public String getPageTitle() {
        switch (this) {
            case InstallAgent:
                return AppMapBundle.get("userMilestones.installAgentTitle");
            case RecordAppMaps:
                return AppMapBundle.get("userMilestones.recordAppMapsTitle");
            case AppMapsTable:
                return AppMapBundle.get("userMilestones.appMapsTableTitle");
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }
}
