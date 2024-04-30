package appland.javaAgent;

import appland.utils.SystemProperties;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.nio.file.Paths;
import java.util.function.Supplier;

public class OverrideJavaAgentLocationRule implements TestRule {
    private final @NotNull Supplier<CodeInsightTestFixture> fixtureSupplier;

    public OverrideJavaAgentLocationRule(@NotNull Supplier<CodeInsightTestFixture> fixtureSupplier) {
        this.fixtureSupplier = fixtureSupplier;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                var path = Paths.get(fixtureSupplier.get().getTempDirFixture().getTempDirPath()).resolve("appmap-agent");
                NioFiles.deleteRecursively(path);
                SystemProperties.withPropertyValue(SystemProperties.USER_HOME, path.toString(), statement::evaluate);
            }
        };
    }
}
