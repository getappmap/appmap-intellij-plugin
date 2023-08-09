package appland.toolwindow.installGuide;

import appland.installGuide.InstallGuideViewPage;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Label representing a single item of user milestones, e.g. "Install AppMap extension".
 */
class StatusLabel extends JBLabel {
    private final @Nullable Runnable clickAction;
    @Getter
    private final InstallGuideViewPage page;

    StatusLabel(@NotNull InstallGuideViewPage page, @Nullable Runnable clickAction) {
        super(clickAction == null ? page.getPageTitle() : createLink(page.getPageTitle()), null, LEADING);
        this.page = page;
        this.clickAction = clickAction;

        setStatus(InstallGuideStatus.Incomplete);
        setCopyable(true);
    }

    @Override
    protected @NotNull HyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                if (clickAction != null && "action://".equals(e.getDescription())) {
                    clickAction.run();
                }
            }
        };
    }

    void setStatus(@NotNull InstallGuideStatus status) {
        setIcon(status.getIcon());
    }

    private static String createLink(@NotNull String label) {
        return String.format("<html><a href='action://'>%s</a></html>", label);
    }
}
