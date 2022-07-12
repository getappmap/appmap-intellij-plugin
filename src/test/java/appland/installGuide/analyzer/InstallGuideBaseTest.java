package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class InstallGuideBaseTest extends AppMapBaseTest {
    protected void assertFramework(@Nullable Feature result, @Nullable String title, @NotNull Score score) {
        assertNotNull(result);
        assertEquals(title, result.title);
        assertEquals(score, result.score);
    }
}
