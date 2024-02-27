package appland.installGuide;

import appland.AppMapBundle;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum InstallGuideViewPage {
    InstallAgent("project-picker"),
    RecordAppMaps("record-appmaps"),
    AskNavieAI("ask-navie-ai");

    // value used by the JS application as "page" values
    private final String pageId;

    public static @NotNull InstallGuideViewPage findByPageId(@NotNull String id) {
        for (var currentId : values()) {
            if (id.equals(currentId.pageId)) {
                return currentId;
            }
        }

        throw new IllegalStateException("Unsupported id: " + id);
    }

    public @NotNull String getPageTitle() {
        switch (this) {
            case InstallAgent:
                return AppMapBundle.get("installGuide.pageInstallAgent.title");
            case RecordAppMaps:
                return AppMapBundle.get("installGuide.pageRecordAppMaps.title");
            case AskNavieAI:
                return AppMapBundle.get("installGuide.pageAskNavie.title");
            default:
                throw new IllegalStateException("Unsupported type: " + this);
        }
    }

    public void open(@NotNull Project project) {
        if (this == InstallGuideViewPage.AskNavieAI) {
            NavieEditorProvider.openEditor(project, DataContext.EMPTY_CONTEXT);
        } else {
            InstallGuideEditorProvider.open(project, this);
        }
    }
}
