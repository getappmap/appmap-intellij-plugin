package appland.installGuide;

import appland.AppMapBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum InstallGuideViewPage {
    InstallGuide("INSTALL_GUIDE");

    // value used by the JS application as "page" values
    @Getter
    private final String pageId;

    public static @Nullable InstallGuideViewPage findByPageId(@NotNull String pageId) {
        for (var type : InstallGuideViewPage.values()) {
            if (type.pageId.equals(pageId)) {
                return type;
            }
        }
        return null;
    }

    InstallGuideViewPage(@NotNull String pageId) {
        this.pageId = pageId;
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
