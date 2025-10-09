package appland.cli;

import appland.AppMapBaseTest;
import com.intellij.util.text.SemVer;
import org.junit.Test;

public class SemVerComparatorTest extends AppMapBaseTest {
    @Test
    public void comparator() {
        var comparator = SemVerComparator.INSTANCE;
        assertEquals(0, comparator.compare(SemVer.parseFromText("1.2.3"), SemVer.parseFromText("1.2.3")));
        assertEquals(1, comparator.compare(null, SemVer.parseFromText("1.2.3")));
        assertEquals(-1, comparator.compare(SemVer.parseFromText("1.2.3"), null));

        assertEquals(1, comparator.compare(SemVer.parseFromText("2.0.0"), SemVer.parseFromText("1.2.3")));
        assertEquals(-1, comparator.compare(SemVer.parseFromText("1.2.3"), SemVer.parseFromText("2.0.0")));
        assertEquals(1, comparator.compare(SemVer.parseFromText("1.3.0"), SemVer.parseFromText("1.2.999")));
    }
}