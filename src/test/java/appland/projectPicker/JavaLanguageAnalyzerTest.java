package appland.projectPicker;

import appland.AppMapBaseTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class JavaLanguageAnalyzerTest extends AppMapBaseTest {
    @Override
    protected String getBasePath() {
        return "projectPicker/sample-projects";
    }

    @Test
    public void mavenPlain() {
        var result = loadDirectory("java-maven-plain");
        assertJavaSettings(result.getFeatures(), "pom.xml", "com.appland:appmap-maven-plugin");
        assertNull(result.getFeatures().web);
        assertNull(result.getFeatures().test);
    }

    @Test
    public void mavenSpring() {
        var result = loadDirectory("java-maven-spring");
        assertJavaSettings(result.getFeatures(), "pom.xml", "com.appland:appmap-maven-plugin");
        assertFramework(result.getFeatures().web, "Spring");
        assertNull(result.getFeatures().test);
    }

    @Test
    public void mavenSpringJUnit() {
        var result = loadDirectory("java-maven-spring-junit");
        assertJavaSettings(result.getFeatures(), "pom.xml", "com.appland:appmap-maven-plugin");
        assertFramework(result.getFeatures().web, "Spring");
        assertFramework(result.getFeatures().test, "JUnit");
    }

    @NotNull
    private Result loadDirectory(String sourceFilePath) {
        var root = myFixture.copyDirectoryToProject(sourceFilePath, "root");
        var result = LanguageAnalyzers.JAVA_LANGUAGE_ANALYZER.analyze(root);
        assertNotNull(result);
        assertEquals("root", result.getName());
        assertEquals("/src/root", result.getPath());
        return result;
    }

    private void assertJavaSettings(Features features, String buildSystemFileName, String buildPlugin) {
        assertEquals("Java", features.lang.title);
        assertEquals(buildSystemFileName, features.lang.depFile);
        assertEquals(buildPlugin, features.lang.plugin);
        assertEquals("plugin", features.lang.pluginType);
        assertEquals(Score.Good, features.lang.score);
        assertFalse(features.lang.text.isEmpty());
    }

    private void assertFramework(@Nullable Feature result, String Spring) {
        assertNotNull(result);
        assertEquals(Spring, result.title);
        assertEquals(Score.Good, result.score);
        assertFalse(result.title.isEmpty());
    }
}