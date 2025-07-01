package appland.toolwindow.navie;

import appland.AppMapBundle;
import appland.actions.QuickReviewAction;
import appland.toolwindow.AppMapContentPanel;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.ide.plugins.newui.ColorButton;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NaviePanel extends AppMapContentPanel {
    private final @NotNull Project project;

    public NaviePanel(@NotNull Project project, @NotNull Disposable parent) {
        super(true);
        this.project = project;
    }

    @Override
    protected void setupPanel() {
        add(newWrappingLabel(AppMapBundle.get("toolwindow.appmap.navie.intro"), false));
        add(createNavieChatButton());
        add(createQuickReviewButton());
        add(newWrappingLabel(AppMapBundle.get("toolwindow.appmap.navie.details"), true));
    }

    private @NotNull JButton createButton() {
        return new ColorButton() {
            {
                // We're using theme colors of similar UI to choose matching colors
                setBgColor(JBUI.CurrentTheme.GotItTooltip.background(true));
                setTextColor(JBUI.CurrentTheme.GotItTooltip.foreground(true));
                setFocusedTextColor(JBUI.CurrentTheme.GotItTooltip.foreground(true));
                setBorderColor(JBUI.CurrentTheme.GotItTooltip.borderColor(true));
                setFocusedBorderColor(JBUI.CurrentTheme.Focus.focusColor());
            }
        };
    }

    private @NotNull JButton createNavieChatButton() {
        var button = createButton();
        button.setText(AppMapBundle.get("toolwindow.appmap.navie.newNavieChat"));
        button.addActionListener(e -> {
            // Provide context of current editor, even if it's not in focus.
            // We want to provide the selection of the editor to Navie.
            var currentEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            var editorContext = currentEditor != null
                    ? EditorUtil.getEditorDataContext(currentEditor)
                    : DataContext.EMPTY_CONTEXT;

            NavieEditorProvider.openEditor(project, editorContext);
        });
        return button;
    }

    private @NotNull JButton createQuickReviewButton() {
        var button = createButton();
        button.setText(AppMapBundle.get("toolwindow.appmap.navie.quickReview"));
        button.addActionListener(e -> {
            var manager = ActionManager.getInstance();
            var action = manager.getAction(QuickReviewAction.ACTION_ID);
            assert (action != null);
            manager.tryToExecute(action, null, this, ActionPlaces.TOOLWINDOW_CONTENT, true);
        });
        return button;
    }

    private static @NotNull JBLabel newWrappingLabel(@NotNull String text, boolean topPadding) {
        var label = new JBLabel(text, UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.NORMAL);
        label.setAllowAutoWrapping(true);
        label.setBorder(JBUI.Borders.empty(topPadding ? 10 : 0, 0, 10, 0));
        return label;
    }
}