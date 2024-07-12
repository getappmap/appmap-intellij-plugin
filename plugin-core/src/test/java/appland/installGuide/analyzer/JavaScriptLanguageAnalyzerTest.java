package appland.installGuide.analyzer;

import appland.installGuide.analyzer.languages.JavaScriptLanguageAnalyzer;
import org.junit.Test;

public class JavaScriptLanguageAnalyzerTest extends LanguageAnalyzerBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/sample-projects";
    }

    @Test
    public void jest() {
        var result = loadDirectory("javascript-jest", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Jest", Score.Okay);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mocha8() {
        var result = loadDirectory("javascript-mocha", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Okay);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mocha7() {
        var result = loadDirectory("javascript-mocha7", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Bad);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mochaPackageLock() {
        var result = loadDirectory("javascript-mocha-npm-lock", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Okay);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mocha7PackageLock() {
        var result = loadDirectory("javascript-mocha7-npm-lock", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Bad);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mochaYarn1Lock() {
        var result = loadDirectory("javascript-mocha-yarn1", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Okay);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mochaYarn2Lock() {
        var result = loadDirectory("javascript-mocha-yarn2", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Okay);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void expressMocha() {
        var result = loadDirectory("javascript-express-mocha", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "Mocha", Score.Okay);
        assertFramework(result.getFeatures().web, "Express.js", Score.Okay);
    }

    private void assertJavaScriptSettings(Features features, String buildSystemFileName, String buildPlugin) {
        assertEquals("JavaScript", features.lang.title);
        assertEquals(buildSystemFileName, features.lang.depFile);
        assertEquals(buildPlugin, features.lang.plugin);
        assertEquals("package", features.lang.pluginType);
        assertEquals(Score.Okay, features.lang.score);
        assertFalse(features.lang.text.isEmpty());
    }
}