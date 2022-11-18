package appland.installGuide;

import appland.AppMapBundle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public enum InstallGuideViewPage {
    InstallAgent("project-picker"),
    RecordAppMaps("record-appmaps"),
    OpenAppMaps("open-appmaps"),
    GenerateOpenAPI("openapi"),
    RuntimeAnalysis("investigate-findings");

    // value used by the JS application as "page" values
    @Getter
    private final String pageId;

    public @NotNull String getPageTitle() {
        switch (this) {
            case InstallAgent:
                return AppMapBundle.get("installGuide.pageInstallAgent.title");
            case RecordAppMaps:
                return AppMapBundle.get("installGuide.pageRecordAppMaps.title");
            case OpenAppMaps:
                return AppMapBundle.get("installGuide.pageOpenAppMaps.title");
            case GenerateOpenAPI:
                return AppMapBundle.get("installGuide.pageGenerateOpenAPI.title");
            case RuntimeAnalysis:
                return AppMapBundle.get("installGuide.pageRuntimeAnalysis.title");
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }
}
