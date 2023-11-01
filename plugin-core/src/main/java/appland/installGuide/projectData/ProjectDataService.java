package appland.installGuide.projectData;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ProjectDataService {
    static @NotNull ProjectDataService getInstance(@NotNull Project project) {
        return project.getService(ProjectDataService.class);
    }

    /**
     * @param updateMetadata If the metadata should be updated before retuning the cached projects.
     *                       If {@code true}, then this method MUST NOT be invoked in a ReadAction.
     * @return The available AppMap projects
     */
    @NotNull List<ProjectMetadata> getAppMapProjects(boolean updateMetadata);
}
