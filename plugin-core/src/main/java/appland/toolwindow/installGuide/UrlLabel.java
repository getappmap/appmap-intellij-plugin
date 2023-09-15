package appland.toolwindow.installGuide;

import appland.Icons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class UrlLabel extends JBLabel {
    public UrlLabel(@NotNull String label, @NotNull String url) {
        super(createLink(label, url));

        setIcon(Icons.LINK_VISITED);

        // to enable link support
        setCopyable(true);
    }

    private static String createLink(String label, String url) {
        return String.format("<html><a href='%s'>%s</a></html>", url, label);
    }

    @Override
    protected @NotNull HyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                var url = e.getDescription();
                BrowserUtil.browse(url);
            }
        };
    }
}
