package appland.installGuide;

import appland.AppMapBundle;
import appland.settings.AppMapApplicationSettingsService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public enum InstallGuideViewPage {
    InstallAgent("project-picker"),
    RecordAppMaps("record-appmaps"),
    OpenAppMaps("open-appmaps"),
    RuntimeAnalysis("investigate-findings");

    // value used by the JS application as "page" values
    @Getter
    private final String pageId;

    public boolean isEnabled() {
        if (this == RuntimeAnalysis) {
            return AppMapApplicationSettingsService.getInstance().isAnalysisEnabled();
        }
        return true;
    }

    public @NotNull String getPageTitle() {
        switch (this) {
            case InstallAgent:
                return AppMapBundle.get("installGuide.pageInstallAgent.title");
            case RecordAppMaps:
                return AppMapBundle.get("installGuide.pageRecordAppMaps.title");
            case OpenAppMaps:
                return AppMapBundle.get("installGuide.pageOpenAppMaps.title");
            case RuntimeAnalysis:
                return AppMapBundle.get("installGuide.pageRuntimeAnalysis.title");
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }
}
