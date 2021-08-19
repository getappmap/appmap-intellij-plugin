package appland.toolwindow.milestones;

import appland.milestones.UserMilestonesEditorProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

class MilestoneActions {
    static void openQuickstart(@NotNull Project project) {
        UserMilestonesEditorProvider.openUserQuickstart(project);
    }

    static void openAppMaps(@NotNull Project project) {
        UserMilestonesEditorProvider.openUserAppMaps(project);
    }

    // removed commented code of "installAppMapAgent"
}
