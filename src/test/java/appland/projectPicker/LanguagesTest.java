package appland.projectPicker;

import appland.AppMapBaseTest;
import org.junit.Test;

public class LanguagesTest extends AppMapBaseTest {
    @Test
    public void languages() {
        var languages = Languages.getLanguages();
        assertNotEmpty(languages);

        assertEquals("java", Languages.getLanguage(".java").id);
    }
}