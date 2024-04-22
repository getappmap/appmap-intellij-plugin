package appland.toolwindow.navie;

import appland.AppMapBundle;
import appland.toolwindow.AppMapContentPanel;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.ide.plugins.newui.ColorButton;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NaviePanel extends AppMapContentPanel {
    private final Project project;

    public NaviePanel(@NotNull Project project, @NotNull Disposable parent) {
        super(true);
        this.project = project;
    }

    @Override
    protected void setupPanel() {
        add(newWrappingLabel(AppMapBundle.get("toolwindow.appmap.navie.intro"), false));
        add(createNavieChatButton());
        add(newWrappingLabel(AppMapBundle.get("toolwindow.appmap.navie.details"), true));
    }

    private @NotNull JButton createNavieChatButton() {
        // anonymous class because setBgColor and the related methods have protected visibility
        return new ColorButton() {
            {
                setText(AppMapBundle.get("toolwindow.appmap.navie.newNavieChat"));

                addActionListener(e -> NavieEditorProvider.openEditor(project, DataContext.EMPTY_CONTEXT));

                // We're using theme colors of similar UI to choose matching colors
                setBgColor(JBUI.CurrentTheme.GotItTooltip.background(true));
                setTextColor(JBUI.CurrentTheme.GotItTooltip.foreground(true));
                setFocusedTextColor(JBUI.CurrentTheme.GotItTooltip.foreground(true));
                setBorderColor(JBUI.CurrentTheme.GotItTooltip.borderColor(true));
                setFocusedBorderColor(JBUI.CurrentTheme.Focus.focusColor());
            }
        };
    }

    private static @NotNull JBLabel newWrappingLabel(@NotNull String text, boolean topPadding) {
        var label = new JBLabel(text, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.NORMAL);
        label.setAllowAutoWrapping(true);
        label.setBorder(JBUI.Borders.empty(topPadding ? 10 : 0, 0, 10, 0));
        return label;
    }
}
