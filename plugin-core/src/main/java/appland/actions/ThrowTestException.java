package appland.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ThrowTestException extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        throw new IllegalStateException("AppMap test exception, " + Math.random());
    }
}
