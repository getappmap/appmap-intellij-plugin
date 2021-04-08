package appland;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;

public class AppLandJCEFTestAction extends AnAction {
    private static final String PLUGIN_ID = "app.land.appmap";
    private static final Logger LOG = Logger.getInstance("#appland");

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!JBCefApp.isSupported()) {
            Messages.showErrorDialog("JCEF is not supported on the current platform.", "AppLand JCEF");
            return;
        }

        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        assert plugin != null;

        var basePath = plugin.getPluginPath();
        assert basePath != null;

        var project = e.getProject();
        assert (project != null);

        var htmlFilePath = basePath.resolve("appmap").resolve("index.html");
        try {
            openFile(project, htmlFilePath.toUri().toURL().toString(), null);
        }
        catch (MalformedURLException exception) {
            LOG.error(exception);
        }
    }

    private void openFile(@NotNull Project project, @Nullable String url, @Nullable String content) {
        assert JBCefApp.isSupported();
        if (url == null && content == null) throw new IllegalArgumentException();

        if (url != null) {
            HTMLEditorProvider.openEditor(project, "App.Land Browser", url, "");
        }
        else {
            HTMLEditorProvider.openEditor(project, "App.Land Browser", content);
        }
    }
}
