package appland.toolwindow.signInView;

import appland.oauth.AppMapLoginAction;
import appland.toolwindow.AppMapToolWindowContent;
import appland.utils.GsonUtils;
import appland.webviews.ConsoleInitMessageHandler;
import appland.webviews.webserver.AppMapWebview;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SignInViewPanel extends SimpleToolWindowPanel implements Disposable, AppMapToolWindowContent {
    private static final Logger LOG = Logger.getInstance(SignInViewPanel.class);

    private final AtomicBoolean pageLoaded = new AtomicBoolean(false);
    private final JCEFHtmlPanel htmlPanel = new JCEFHtmlPanel(true, null, null);
    // queries must be created before the webview is initialized
    private final @NotNull JBCefJSQuery postMessageQuery = JBCefJSQuery.create((JBCefBrowserBase) htmlPanel);

    public SignInViewPanel(@NotNull Disposable parentDisposable) {
        super(true, true);
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
        htmlPanel.getJBCefClient().addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                // Disable navigation to / of the built-in webserver,
                // which is used by the webview when "Sign in" is clicked.
                if (AppMapWebview.getBaseUrl().equals(StringUtil.trimEnd(request.getURL(), "/"))) {
                    return true;
                }

                // open link in the external browser
                if (user_gesture) {
                    BrowserUtil.browse(request.getURL());
                    return true;
                }

                return false;
            }
        }, htmlPanel.getCefBrowser());

        // open new webview windows, which are opened via <a href="..." target="_blank", in the external browser
        htmlPanel.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                if (target_url.startsWith("http://") || target_url.startsWith("https://")) {
                    BrowserUtil.browse(target_url);
                    return true;
                }
                return false;
            }
        }, htmlPanel.getCefBrowser());

        htmlPanel.getJBCefClient().addDisplayHandler(new ConsoleInitMessageHandler(this::initWebView), htmlPanel.getCefBrowser());

        htmlPanel.loadURL(AppMapWebview.SignIn.getIndexHtmlUrl());
    }

    private void initWebView() {
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
            case "sign-in":
                AppMapLoginAction.authenticate();
                return true;

            // known message, but not handled
            case "click-sign-in-link":
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
