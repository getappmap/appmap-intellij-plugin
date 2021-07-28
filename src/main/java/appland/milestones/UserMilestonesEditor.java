package appland.milestones;

import appland.AppMapPlugin;
import appland.files.FileLocation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.*;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserMilestonesEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance("#appmap.milestones");
    private static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    @NotNull
    private final Project project;
    @NotNull
    private final VirtualFile file;

    private final JBCefClient jcefClient = JBCefApp.getInstance().createClient();
    private final JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(jcefClient, null);
    private final JBCefJSQuery jcefBridge = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    private final AtomicBoolean initial = new AtomicBoolean(true);
    private final AtomicBoolean navigating = new AtomicBoolean(false);

    public UserMilestonesEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;

        Disposer.register(this, jcefClient);
        Disposer.register(this, contentPanel);
        Disposer.register(this, jcefBridge);

        setupJCEF();
        loadMilestonesApplication();
    }

    private void setupJCEF() {
        contentPanel.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                if (!initial.get()) {
                    return;
                }

                initial.set(false);
            }
        }, contentPanel.getCefBrowser());

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

    private void loadMilestonesApplication() {
        try {
            var filePath = AppMapPlugin.getUserMilestonesHTMLPath();
            String htmlFileURL = filePath.toUri().toURL().toString();
            contentPanel.loadURL(htmlFileURL);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private void onJavaScriptApplicationReady() {
        setupJCEFBridge();

//        var app = ApplicationManager.getApplication();
//        app.invokeLater(AppMapFileEditor.this::loadAppmapData, ModalityState.defaultModalityState());
    }

    private void setupJCEFBridge() {
        // viewSource callback handler
//        jcefBridge.addHandler(relativePath -> {
//            LOG.debug("viewSource callback handler, file: " + relativePath);
//            ApplicationManager.getApplication().invokeLater(() -> {
//                var location = FileLocation.parse(relativePath);
//                if (location != null) {
//                    var referencedFile = FileLookup.findRelativeFile(project, file, FileUtil.toSystemIndependentName(location.filePath));
//                    if (referencedFile != null) {
//                        // IntelliJ's lines are 0-based, AppMap lines seem to be 0-based
//                        var line = location.line == null ? -1 : location.line - 1;
//                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, referencedFile, line, -1);
//
//                        OpenInRightSplitAction.Companion.openInRightSplit(project, referencedFile, descriptor, true);
//                        return;
//                    }
//                }
//
//                // fallback message if the could not be found
//                showErrorDialog("File " + relativePath + " could not be found.", "AppMap");
//            }, ModalityState.defaultModalityState());
//            return null;
//        });
//        contentPanel.getCefBrowser().executeJavaScript(createCallbackJS(jcefBridge, "viewSource"), baseURL, 0);
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
        return "User Milestones";
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
        LOG.debug("Disposing user milestones editor");
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return file;
    }
}
