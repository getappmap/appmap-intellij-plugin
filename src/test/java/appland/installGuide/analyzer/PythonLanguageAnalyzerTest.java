package appland.installGuide.analyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class PythonLanguageAnalyzerTest extends InstallGuideBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/sample-projects";
    }

    @Test
    public void pipDjango() {
        var result = loadDirectory("python-pip-django");
        assertLanguage(result.getFeatures(), "requirements.txt", null);
        assertWeb(result.getFeatures(), "Django");
        assertIsBad(result.getFeatures().test);
    }

    @Test
    public void pipFlask() {
        var result = loadDirectory("python-pip-flask");
        assertLanguage(result.getFeatures(), "requirements.txt", null);
        assertWeb(result.getFeatures(), "flask");
        assertIsBad(result.getFeatures().test);
    }

    @Test
    public void pipFlaskUnittest() {
        var result = loadDirectory("python-pip-flask-unittest");
        assertLanguage(result.getFeatures(), "requirements.txt", null);
        assertWeb(result.getFeatures(), "flask");
        assertTest(result.getFeatures(), "unittest");
    }

    @Test
    public void pyprojectDjango() {
        var result = loadDirectory("python-pyproject-django");
        assertLanguage(result.getFeatures(), "pyproject.toml", null);
        assertWeb(result.getFeatures(), "Django");
        assertIsBad(result.getFeatures().test);
    }

    private void assertLanguage(@NotNull Features features, @Nullable String depFile, @Nullable String pluginValue) {
        assertEquals("Python", features.lang.title);
        assertEquals(depFile, features.lang.depFile);
        assertEquals(pluginValue, features.lang.plugin);
        assertEquals(Score.Okay, features.lang.score);
        assertFalse(features.lang.text.isEmpty());
    }

    private void assertWeb(@NotNull Features features, @Nullable String title) {
        assertNotNull(features.web);
        assertEquals(Score.Okay, features.web.score);
        assertEquals(title, features.web.title);
    }

    private void assertTest(@NotNull Features features, @Nullable String title) {
        assertNotNull(features.test);
        assertEquals(Score.Okay, features.test.score);
        assertEquals(title, features.test.title);
    }
}