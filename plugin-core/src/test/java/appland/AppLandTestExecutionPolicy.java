package appland;

import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy;
import org.jetbrains.annotations.NotNull;

/**
 * Test execution policy configured in build.gradle.kts.
 */
@SuppressWarnings("unused")
public class AppLandTestExecutionPolicy extends IdeaTestExecutionPolicy {
    @Override
    protected String getName() {
        return "AppLand";
    }

    @Override
    public String getHomePath() {
        return findAppMapHomePath();
    }

    public static @NotNull String findAppMapHomePath() {
        return System.getProperty("appland.testDataPath");
    }
}
