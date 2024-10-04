package appland.webviews.webserver;

import appland.AppMapBaseTest;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.builtInWebServer.BuiltInServerOptions;
import org.jetbrains.ide.BuiltInServerManager;
import org.junit.Test;

import java.io.IOException;

public class AppMapWebviewTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void baseUrl() {
        var port = BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
        var baseUrl = AppMapWebview.getBaseUrl();
        assertEquals("The base URL must not contain a trailing slash", "http://localhost:" + port, baseUrl);
    }

    @Test
    public void webviewUrl() {
        var port = BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
        assertEquals("A webview base URL must not contain a double slash",
                "http://localhost:" + port + "/_appmap-webviews/appland-signin/index.html",
                AppMapWebview.SignIn.getIndexHtmlUrl());
    }

    @Test
    public void webViewIndexHtmlUrls() throws IOException {
        var serverManager = BuiltInServerManager.getInstance();
        serverManager.waitForStart();

        for (var webview : AppMapWebview.values()) {
            // throws an exception if the URL is unavailable
            HttpRequests.request(webview.getIndexHtmlUrl()).readString();
        }
    }
}