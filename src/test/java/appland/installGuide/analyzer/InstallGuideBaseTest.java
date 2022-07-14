package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class InstallGuideBaseTest extends AppMapBaseTest {
    protected void assertFramework(@Nullable Feature result, @Nullable String title, @NotNull Score score) {
        assertNotNull(result);
        assertEquals(title, result.title);
        assertEquals(score, result.score);
    }

    protected @NotNull ProjectAnalysis loadDirectory(@NotNull String sourceFilePath) {
        var root = myFixture.copyDirectoryToProject(sourceFilePath, "root");

        var language = new LanguageResolver().getLanguage(root);
        assertNotNull("Language must be detected", language);

        var analyzer = LanguageAnalyzer.create(language);
        assertNotNull(analyzer);

        return analyzeDirectory(root, analyzer);
    }

    protected @NotNull ProjectAnalysis loadDirectory(@NotNull String sourceFilePath, @NotNull LanguageAnalyzer analyzer) {
        var root = myFixture.copyDirectoryToProject(sourceFilePath, "root");
        return analyzeDirectory(root, analyzer);
    }

    @NotNull
    private static ProjectAnalysis analyzeDirectory(VirtualFile root, LanguageAnalyzer analyzer) {
        var result = analyzer.analyze(root);
        assertNotNull(result);
        assertEquals("root", result.getName());
        assertEquals("/src/root", result.getPath());
        return result;
    }
}
