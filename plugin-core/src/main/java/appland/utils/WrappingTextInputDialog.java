package appland.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.MessageMultilineInputDialog;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class WrappingTextInputDialog extends MessageMultilineInputDialog {
    public WrappingTextInputDialog(@NotNull Project project,
                                   @NotNull String message,
                                   @NotNull String title,
                                   @Nullable Icon icon,
                                   @Nullable @NonNls String initialValue,
                                   @Nullable InputValidator validator,
                                   String @NotNull [] options,
                                   int defaultOption) {
        super(project, message, title, icon, initialValue, validator, options, defaultOption);
    }

    @Override
    protected JTextComponent createTextFieldComponent() {
        var area = new JBTextArea(4, 50);
        area.setLineWrap(true);
        return area;
    }
}
