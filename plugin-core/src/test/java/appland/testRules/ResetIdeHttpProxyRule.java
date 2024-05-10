package appland.testRules;

import com.intellij.util.net.HttpConfigurable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class ResetIdeHttpProxyRule implements TestRule {
    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                var oldProxySettings = HttpConfigurable.getInstance().getState();
                if (oldProxySettings == null) {
                    oldProxySettings = new HttpConfigurable();
                }

                try {
                    statement.evaluate();
                } finally {
                    HttpConfigurable.getInstance().loadState(oldProxySettings);
                }
            }
        };
    }
}
