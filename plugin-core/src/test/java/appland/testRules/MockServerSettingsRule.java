package appland.testRules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockserver.configuration.ConfigurationProperties;

/**
 * Modifies the default settings of MockServer to be more suitable for this project.
 */
public final class MockServerSettingsRule implements TestRule {
    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ConfigurationProperties.disableSystemOut(true);

                statement.evaluate();
            }
        };
    }
}
