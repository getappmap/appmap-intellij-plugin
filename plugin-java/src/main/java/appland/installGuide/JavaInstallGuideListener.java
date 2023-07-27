package appland.installGuide;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a new appmap.yml file in a Java project, when the install guide step "Install Agent" is executed.
 */
public class JavaInstallGuideListener implements InstallGuideListener {
    private static final Logger LOG = Logger.getInstance(JavaInstallGuideListener.class);

    private final @NotNull Project project;

    public JavaInstallGuideListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void afterInstallGuidePageOpened(@NotNull InstallGuideViewPage page) {
        if (page != InstallGuideViewPage.InstallAgent) {
            return;
        }
    }
}
