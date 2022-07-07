package appland.installGuide;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public enum InstallGuideViewType {
    InstallGuide("INSTALL_GUIDE");

    // value used by the JS application for "transition" messages
    private final String transitionValue;

    InstallGuideViewType(String transitionValue) {
        this.transitionValue = transitionValue;
    }

    @Nullable
    public static InstallGuideViewType findByTransitionTarget(@NotNull String target) {
        for (InstallGuideViewType type : InstallGuideViewType.values()) {
            if (type.transitionValue.equals(target)) {
                return type;
            }
        }
        return null;
    }

    public Path getHTMLPath() {
        switch (this) {
            case InstallGuide:
                return AppMapPlugin.getInstallGuideHTMLPath();
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }

    public String getPageTitle() {
        switch (this) {
            case InstallGuide:
                return AppMapBundle.get("userMilestones.installGuideTitle");
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }
}
