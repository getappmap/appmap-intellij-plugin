package appland.testRules;

import com.intellij.util.net.ProxyConfiguration;
import com.intellij.util.net.ProxySettings;
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
                var settings = ProxySettings.getInstance();
                var originalConfig = settings.getProxyConfiguration();

                try {
                    settings.setProxyConfiguration(ProxyConfiguration.proxy(ProxyConfiguration.ProxyProtocol.HTTP, "127.0.0.1", mockServerRule.getPort(), ""));

                    statement.evaluate();
                } finally {
                    settings.setProxyConfiguration(originalConfig);
                }
            }
        };
    }
}
