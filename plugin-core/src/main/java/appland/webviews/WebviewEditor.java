package appland.webviews;

import appland.AppMapBundle;
import appland.utils.GsonUtils;
import appland.webviews.webserver.AppMapWebview;
import appland.webviews.webserver.WebviewAuthTokenRequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for editors based on a JCEF webview.
 * It implements most of the shared functionality and allows to customize link handling, message handling and more.
 */
public abstract class WebviewEditor<T> extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance(WebviewEditor.class);

    protected final @NotNull Project project;
    protected final @NotNull AppMapWebview webview;
    protected final @NotNull VirtualFile file;
    private final @NotNull Set<String> supportedMessages;
    protected final @NotNull Gson gson;
    protected final @NotNull JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(true, null, null);

    private final @NotNull AtomicBoolean isWebViewReady = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean navigating = new AtomicBoolean(false);
    // queries must be created before the webview is initialized
    private final @NotNull JBCefJSQuery postMessageQuery = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    public WebviewEditor(@NotNull Project project,
                         @NotNull AppMapWebview webview,
                         @NotNull VirtualFile file,
                         @NotNull Set<String> supportedMessages) {
        this.project = project;
        this.webview = webview;
        this.file = file;
        this.supportedMessages = supportedMessages;
        this.gson = Objects.requireNonNullElse(createCustomizedGson(), GsonUtils.GSON);

        // contentPanel and jcefBridge register with the client as Disposable parent
        Disposer.register(this, contentPanel.getJBCefClient());

        setupJCEF();
        loadApplication();
    }

    /**
     * @return The data to initialize this webview. The data is passed to {@link #setupInitMessage(Object, JsonObject)}
     * and also to {@link #afterInit(Object)}.
     */
    @RequiresBackgroundThread
    protected abstract @Nullable T createInitData();

    /**
     * Initialize the init message payload.
     * This method is executed under progress in a background thread.
     *
     * @param initData Data returned by {@link #createInitData()}.
     * @param payload  JSON object to send with the initial "init" message to the webview application.
     */
    @RequiresBackgroundThread
    protected abstract void setupInitMessage(@Nullable T initData, @NotNull JsonObject payload);

    /**
     * This method allows to post-process editor init, e.g. to send telemetry messages.
     *
     * @param initData Data used to init this editor
     */
    @RequiresBackgroundThread
    protected abstract void afterInit(@Nullable T initData);

    /**
     * @throws Exception If an error occurred while handling the message
     */
    abstract protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception;

    /**
     * @return A customized instance of {@code com.google.gson.Gson} or {@code null} if not customized GSON is needed
     */
    protected @Nullable Gson createCustomizedGson() {
        return null;
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
    public void setState(@NotNull FileEditorState state) {
    }

    public void clearState() {
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
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    /**
     * Opens the developer tools panel associated with this editor's webview.
     */
    public final void openDevTools() {
        ApplicationManager.getApplication().invokeLater(contentPanel::openDevtools);
    }

    /**
     * @param type ID of the message
     * @return A JSON object suitable to be used with {@link #postMessage(JsonElement)}.
     */
    protected @NotNull JsonObject createMessageObject(@NotNull String type) {
        return GsonUtils.singlePropertyObject("type", type);
    }

    protected void postMessage(@NotNull JsonElement json) {
        contentPanel.getCefBrowser().executeJavaScript("window.postMessage(" + gson.toJson(json) + ")", "", 0);
    }

    protected boolean isWebViewReady() {
        return isWebViewReady.get();
    }

    private void setupJCEF() {
        // open links to https://appmap.io in the external browser
        contentPanel.getJBCefClient().addRequestHandler(new OpenExternalLinksHandler(), contentPanel.getCefBrowser());
        // open new webview windows, which are opened via <a href="..." target="_blank", in the external browser
        contentPanel.getJBCefClient().addLifeSpanHandler(new OpenExternalTargetLinksHandler(), contentPanel.getCefBrowser());
        // add auth tokens to our localhost requests
        contentPanel.getJBCefClient().addRequestHandler(new WebviewAuthTokenRequestHandler(), contentPanel.getCefBrowser());

        contentPanel.setErrorPage(new DefaultWebviewErrorPage(navigating));
        contentPanel.getJBCefClient().addDisplayHandler(new ConsoleInitMessageHandler(this::initWebviewApplication), contentPanel.getCefBrowser());
    }

    private void loadApplication() {
        contentPanel.loadURL(webview.getIndexHtmlUrl());

        if (Registry.is("appmap.webview.open.dev.tools", false)) {
            ApplicationManager.getApplication().invokeLater(this::openDevTools);
        }
    }

    private void initWebviewApplication() {
        postMessageQuery.addHandler(request -> {
            try {
                var json = gson.fromJson(request, JsonObject.class);
                var type = json.has("type") ? json.getAsJsonPrimitive("type").getAsString() : null;
                if (type != null) {
                    if (!supportedMessages.contains(type)) {
                        return null;
                    }

                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            handleMessage(type, json);
                        } catch (Exception e) {
                            LOG.warn("error handling webview message: " + type, e);
                        }
                    });

                    return new JBCefJSQuery.Response("success");
                }
            } catch (Exception e) {
                LOG.warn("error handling command: " + request, e);
            }
            return null;
        });

        contentPanel.getCefBrowser().executeJavaScript(createCallbackJS(postMessageQuery, "postMessage"), "", 0);

        // send init message to webview to launch the JS application in a background thread
        isWebViewReady.set(true);
        new Task.Backgroundable(project, getLoadingProgressTitle(), false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!isValid()) {
                    // editor was closed before the init completed
                    return;
                }

                var initData = createInitData();
                var initMessage = createMessageObject("init");
                setupInitMessage(initData, initMessage);
                postMessage(initMessage);
                afterInit(initData);
            }
        }.queue();
    }

    protected @NotNull String getLoadingProgressTitle() {
        return AppMapBundle.get("webview.loading");
    }

    @NotNull
    private String createCallbackJS(JBCefJSQuery query, @NotNull String functionName) {
        return "if (!window.AppLand) window.AppLand={}; window.AppLand." + functionName + "=function(name) {" +
                query.inject("name") + "};";
    }
}
