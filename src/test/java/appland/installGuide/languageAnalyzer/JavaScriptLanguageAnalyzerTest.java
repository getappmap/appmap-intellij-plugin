package appland.installGuide.languageAnalyzer;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class JavaScriptLanguageAnalyzerTest extends InstallGuideBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/sample-projects";
    }

    @Test
    public void mocha8() {
        var result = loadDirectory("javascript-mocha");
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "mocha", Score.Good);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mocha7() {
        var result = loadDirectory("javascript-mocha7");
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "mocha", Score.Bad);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void expressMocha() {
        var result = loadDirectory("javascript-express-mocha");
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "mocha", Score.Good);
        assertFramework(result.getFeatures().web, "express.js", Score.Good);
    }

    @NotNull
    private ProjectAnalysis loadDirectory(@NotNull String sourceFilePath) {
        var root = myFixture.copyDirectoryToProject(sourceFilePath, "root");
        var result = LanguageAnalyzers.JAVASCRIPT_LANGUAGE_ANALYZER.analyze(root);
        assertNotNull(result);
        assertEquals("root", result.getName());
        assertEquals("/src/root", result.getPath());
        return result;
    }

    private void assertJavaScriptSettings(Features features, String buildSystemFileName, String buildPlugin) {
        assertEquals("JavaScript", features.lang.title);
        assertEquals(buildSystemFileName, features.lang.depFile);
        assertEquals(buildPlugin, features.lang.plugin);
        assertEquals("package", features.lang.pluginType);
        assertEquals(Score.Good, features.lang.score);
        assertFalse(features.lang.text.isEmpty());
    }
}