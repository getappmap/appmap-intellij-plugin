package appland.projectPicker.editor;

import appland.AppMapPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.jcef.*;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectPickerEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance("#appmap.project-picker");
    private static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    private final Project project;
    @NotNull
    private final LightVirtualFile file;

    private final JBCefClient jcefClient = JBCefApp.getInstance().createClient();
    private final JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(jcefClient, null);
    private final JBCefJSQuery jcefBridge = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    private final AtomicBoolean navigating = new AtomicBoolean(false);

    public ProjectPickerEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = (LightVirtualFile) file;

        Disposer.register(this, jcefClient);
        Disposer.register(this, contentPanel);
        Disposer.register(this, jcefBridge);

        setupJCEF();
        loadApplication();
    }

    private void setupJCEF() {
        contentPanel.setErrorPage((errorCode, errorText, failedUrl) -> {
            if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED && navigating.getAndSet(false)) {
                return null;
            }
            return JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl);
        });

        contentPanel.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if (READY_MESSAGE_ID.equals(message)) {
                    onJavaScriptApplicationReady();
                    return true;
                }

                var output = String.format("AppMap JS, %s:%d, %s", source, line, message);
                switch (level) {
                    case LOGSEVERITY_FATAL:
                        LOG.error(output);
                        return true;
                    // logging ERROR as warning because the AppMap app always logs errors about svg image dimensions at start
                    case LOGSEVERITY_ERROR:
                    case LOGSEVERITY_WARNING:
                        LOG.warn(output);
                        return true;
                    case LOGSEVERITY_INFO:
                        LOG.info(output);
                        return true;
                    default:
                        LOG.debug(output);
                        return true;
                }
            }
        }, contentPanel.getCefBrowser());
    }

    private void loadApplication() {
        try {
            contentPanel.loadURL(AppMapPlugin.getProjectPickerHTMLPath().toUri().toURL().toString());
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    /**
     * This method is called after the JavaScript application notified that it's ready to receive data.
     */
    private void onJavaScriptApplicationReady() {
        var json = createAppMapsInitJSON();
        contentPanel.getCefBrowser().executeJavaScript("window.loadAppLandProjects(" + json + ")", "", 0);
    }

    private String createAppMapsInitJSON() {
        return file.getContent().toString();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return contentPanel.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "Project Picker";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        LOG.debug("Disposing project picker editor");
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }
}
