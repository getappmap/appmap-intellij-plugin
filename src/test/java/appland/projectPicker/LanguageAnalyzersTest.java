package appland.projectPicker;

import appland.AppMapBaseTest;
import org.junit.Test;

public class LanguageAnalyzersTest extends AppMapBaseTest {
    @Test
    public void analyzers() {
        var java = Languages.getLanguage("java");
        assertNotNull(java);
        assertNotNull(LanguageAnalyzers.create(java));
    }
}