package appland.projectPicker;

import appland.AppMapBaseTest;
import org.junit.Test;

public class LanguagesTest extends AppMapBaseTest {
    @Test
    public void languages() {
        var languages = Languages.getLanguages();
        assertNotEmpty(languages);

        var java = Languages.getLanguage("java");
        assertNotNull(java);
        assertEquals("java", java.id);
    }
}