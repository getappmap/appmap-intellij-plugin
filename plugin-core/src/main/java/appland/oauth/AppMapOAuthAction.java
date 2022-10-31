package appland.oauth;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

@SuppressWarnings("ComponentNotRegistered")
public class AppMapOAuthAction extends AnAction {
    public AppMapOAuthAction() {
        super("AppMap OAuth");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var response = AppMapOAuthService.getInstance().authorize();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var responseData = response.get();
                ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage("Result:" + responseData, "OAuth"));
            } catch (InterruptedException | ExecutionException ex) {
                System.out.println(ex.getMessage());
            }
        });
    }
}
