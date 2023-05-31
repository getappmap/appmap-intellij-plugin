package appland.toolwindow;

import appland.AppMapBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanelWithEmptyText;

final class JcefUnsupportedPanel extends JBPanelWithEmptyText implements AppMapToolWindowContent {
    @SuppressWarnings("DialogTitleCapitalization")
    public JcefUnsupportedPanel() {
        getEmptyText().setText(AppMapBundle.get("toolwindow.jcefUnsupported.emptyText"), SimpleTextAttributes.ERROR_ATTRIBUTES);
        getEmptyText().appendLine(AppMapBundle.get("toolwindow.jcefUnsupported.emptyText2"));
        getEmptyText().appendLine(AppMapBundle.get("toolwindow.jcefUnsupported.emptyText3"));
        getEmptyText().appendLine(AppMapBundle.get("toolwindow.jcefUnsupported.linkTitle"), SimpleTextAttributes.LINK_ATTRIBUTES, e -> {
            BrowserUtil.browse(AppMapBundle.get("toolwindow.jcefUnsupported.linkUrl"));
        });
    }

    @Override
    public void onToolWindowShown() {
    }

    @Override
    public void onToolWindowHidden() {
    }

    @Override
    public void dispose() {
    }
}
