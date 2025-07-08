package appland.toolwindow.signInView;

import appland.notifications.AppMapNotifications;
import appland.oauth.AppMapLoginAction;
import appland.settings.AppMapApplicationSettingsService;
import appland.toolwindow.AppMapToolWindowContent;
import appland.utils.GsonUtils;
import appland.webviews.ConsoleInitMessageHandler;
import appland.webviews.OpenExternalLinksHandler;
import appland.webviews.OpenExternalTargetLinksHandler;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SignInViewPanel extends SimpleToolWindowPanel implements Disposable, AppMapToolWindowContent {
    private static final Logger LOG = Logger.getInstance(SignInViewPanel.class);

    private final Project project;

    private final AtomicBoolean pageLoaded = new AtomicBoolean(false);
    private final JCEFHtmlPanel htmlPanel = new JCEFHtmlPanel(true, null, null);
    // queries must be created before the webview is initialized
    private final @NotNull JBCefJSQuery postMessageQuery = JBCefJSQuery.create((JBCefBrowserBase) htmlPanel);

    public SignInViewPanel(@NotNull Project project, @NotNull Disposable parentDisposable) {
        super(true, true);
        this.project = project;

        Disposer.register(parentDisposable, this);

        add(htmlPanel.getComponent());
    }

    @Override
    public void dispose() {
    }

    @Override
    public void onToolWindowShown() {
        if (pageLoaded.compareAndSet(false, true)) {
            try {
                loadWebView();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onToolWindowHidden() {
    }

    private void loadWebView() throws MalformedURLException {
        htmlPanel.getJBCefClient().addRequestHandler(new OpenExternalLinksHandler(), htmlPanel.getCefBrowser());

        // Disable navigation to / of the built-in webserver, which is used by the webview when "Sign in" is clicked.
        htmlPanel.getJBCefClient().addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                return AppMapWebview.getBaseUrl().equals(StringUtil.trimEnd(request.getURL(), "/"));
            }
        }, htmlPanel.getCefBrowser());

        // open new webview windows, which are opened via <a href="..." target="_blank", in the external browser
        htmlPanel.getJBCefClient().addLifeSpanHandler(new OpenExternalTargetLinksHandler(), htmlPanel.getCefBrowser());

        htmlPanel.getJBCefClient().addDisplayHandler(new ConsoleInitMessageHandler(this::initWebView), htmlPanel.getCefBrowser());

        htmlPanel.loadURL(AppMapWebview.getBaseUrlWithPath("signin.html"));
    }

    private void initWebView() {
        if (Registry.is("appmap.webview.open.dev.tools", false)) {
            ApplicationManager.getApplication().invokeLater(htmlPanel::openDevtools);
        }

        // add handler for messages sent by the JS application
        postMessageQuery.addHandler(request -> {
            try {
                var json = GsonUtils.GSON.fromJson(request, JsonObject.class);
                var type = json.has("type") ? json.getAsJsonPrimitive("type").getAsString() : null;
                if (type != null && handleWebviewMessage(type, json)) {
                    return new JBCefJSQuery.Response("success");
                }
            } catch (Exception e) {
                LOG.warn("error handling command: " + request, e);
            }
            return new JBCefJSQuery.Response("", 1, "unhandled message");
        });
        htmlPanel.getCefBrowser().executeJavaScript(createCallbackJS(postMessageQuery, "postMessage"), "", 0);

        // trigger init of JS application
        htmlPanel.getCefBrowser().executeJavaScript("window.postMessage({\"type\": \"init\" })", "", 0);
    }

    private boolean handleWebviewMessage(@NotNull String id, @NotNull JsonObject message) {
        switch (id) {
            // OAuth
            case "sign-in":
                AppMapLoginAction.authenticate();
                return true;

            // email authentication
            case "activate":
                var apiKeyProperty = message.getAsJsonPrimitive("apiKey");
                if (apiKeyProperty != null && apiKeyProperty.isString()) {
                    AppMapApplicationSettingsService.getInstance().setApiKeyNotifying(apiKeyProperty.getAsString());
                }
                return true;

            // known message, but not handled
            case "click-sign-in-link":
                return true;

            // handler to show message about broken text input on Linux
            case "email-input-focused":
                if (AppMapNotifications.isWebviewTextInputBroken()) {
                    AppMapNotifications.showWebviewTextInputBrokenMessage(project, false);
                }
                return true;

            default:
                return false;
        }
    }

    @NotNull
    private String createCallbackJS(@NotNull JBCefJSQuery query, @NotNull String functionName) {
        return "if (!window.AppLand) window.AppLand={}; window.AppLand." + functionName + "=function(name) {" +
                query.inject("name") + "};";
    }
}
