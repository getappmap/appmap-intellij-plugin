package appland.problemsView;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TestFindingsManager extends FindingsManager {
    public TestFindingsManager(@NotNull Project project) {
        super(project);
    }

    public static void reset(@NotNull Project project) {
        FindingsManager.getInstance(project).reset();
    }
}
