package appland.toolwindow.installGuide;

import appland.installGuide.InstallGuideViewPage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static appland.installGuide.InstallGuideEditorProvider.open;

/**
 * Manages a list of collapsible user milestone panels.
 */
public class InstallGuidePanel extends JPanel {
    public InstallGuidePanel(@NotNull Project project) {
        super(new VerticalLayout(5));

        addPanel("Quickstart", new InstallGuideContentPanel() {
            @Override
            protected void setupPanel() {
                for (var page : InstallGuideViewPage.values()) {
                    if (page.isEnabled(project)) {
                        add(new StatusLabel(getStatus(page), page.getPageTitle(), () -> open(project, page)));
                    }
                }
            }
        });

        addPanel("Documentation", new InstallGuideContentPanel() {
            @Override
            protected void setupPanel() {
                add(new UrlLabel("Quickstart", "https://appmap.io/docs/quickstart"));
                add(new UrlLabel("AppMap overview", "https://appmap.io/docs/appmap-overview"));
                add(new UrlLabel("How to use AppMap diagrams", "https://appmap.io/docs/how-to-use-appmap-diagrams"));
                add(new UrlLabel("Reference", "https://appmap.io/docs/reference"));
                add(new UrlLabel("Troubleshooting", "https://appmap.io/docs/troubleshooting"));
                add(new UrlLabel("Recording methods", "https://appmap.io/docs/recording-methods"));
                add(new UrlLabel("Community", "https://appmap.io/docs/community"));
                add(new UrlLabel("FAQ", "https://appmap.io/docs/faq"));
            }
        });
    }

    private void addPanel(@NotNull String title, @NotNull InstallGuideContentPanel panel) {
        add(new CollapsibleInstallGuidePanel(panel, false, title));
    }

    @NotNull
    private static InstallGuideStatus getStatus(@NotNull InstallGuideViewPage page) {
        return InstallGuideStatus.Incomplete;
    }
}
