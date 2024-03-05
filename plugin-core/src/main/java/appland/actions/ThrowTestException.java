package appland.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import org.jetbrains.annotations.NotNull;

public class ThrowTestException extends AnAction implements UpdateInBackground {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        throw new IllegalStateException("AppMap test exception, " + Math.random());
    }
}
