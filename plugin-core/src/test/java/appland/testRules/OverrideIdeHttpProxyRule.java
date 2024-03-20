package appland.testRules;

import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockserver.junit.MockServerRule;

public final class OverrideIdeHttpProxyRule implements TestRule {
    private final @NotNull MockServerRule mockServerRule;

    public OverrideIdeHttpProxyRule(@NotNull MockServerRule mockServerRule) {
        this.mockServerRule = mockServerRule;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                var settings = HttpConfigurable.getInstance();
                var wasHttpProxy = settings.USE_HTTP_PROXY;
                var wasProxyHost = settings.PROXY_HOST;
                var wasProxyPort = settings.PROXY_PORT;

                try {
                    settings.USE_HTTP_PROXY = true;
                    settings.PROXY_HOST = "127.0.0.1";
                    settings.PROXY_PORT = mockServerRule.getPort();

                    statement.evaluate();
                } finally {
                    settings.USE_HTTP_PROXY = wasHttpProxy;
                    settings.PROXY_HOST = wasProxyHost;
                    settings.PROXY_PORT = wasProxyPort;
                }
            }
        };
    }
}
