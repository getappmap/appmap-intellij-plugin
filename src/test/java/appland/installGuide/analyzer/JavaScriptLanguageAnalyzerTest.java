package appland.installGuide.analyzer;

import appland.installGuide.analyzer.languages.JavaScriptLanguageAnalyzer;
import org.junit.Test;

public class JavaScriptLanguageAnalyzerTest extends LanguageAnalyzerBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/sample-projects";
    }

    @Test
    public void mocha8() {
        var result = loadDirectory("javascript-mocha", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "mocha", Score.Okay);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void mocha7() {
        var result = loadDirectory("javascript-mocha7", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "mocha", Score.Bad);
        assertNull(result.getFeatures().web);
    }

    @Test
    public void expressMocha() {
        var result = loadDirectory("javascript-express-mocha", new JavaScriptLanguageAnalyzer());
        assertJavaScriptSettings(result.getFeatures(), "package.json", "@appland/appmap-agent-js");
        assertFramework(result.getFeatures().test, "mocha", Score.Okay);
        assertFramework(result.getFeatures().web, "express.js", Score.Okay);
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