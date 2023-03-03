package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StreamingClassMapIteratorTest extends AppMapBaseTest {
    private static class DebuggingClassMapIterator extends StreamingClassMapIterator {
        List<String> ids = new ArrayList<>();

        @Override
        protected void onItem(@NotNull ClassMapItemType type, @Nullable String parentId, @NotNull String id, @NotNull String name, @Nullable String location, int level) {
            if (type != ClassMapItemType.Database
                    && type != ClassMapItemType.HTTP
                    && type != ClassMapItemType.Query
                    && type != ClassMapItemType.Route) {
                ids.add(StringUtil.repeat("  ", level) + id);
            }
        }
    }

    @Test
    public void emptyString() {
        var iterator = new DebuggingClassMapIterator();
        iterator.parse("");
        assertEmpty(iterator.ids);
    }

    @Test
    public void emptyArray() {
        var iterator = new DebuggingClassMapIterator();
        iterator.parse("[]");
        assertEmpty(iterator.ids);
    }

    @Test
    public void singleItem() {
        var iterator = new DebuggingClassMapIterator();
        iterator.parse("[{\"name\": \"json\", \"type\": \"package\"}]");
        assertEquals(List.of("package:json"), iterator.ids);
    }

    /**
     * Assert that we're creating the same items as the VSCode plugin, using the plugin's test data file.
     */
    @Test
    public void vscodeFixtureData() throws IOException {
        var classMapFile = myFixture.copyFileToProject("classMaps/ScannerJobsController_authenticated_user_admin_can_defer_a_finding.json");
        var classMapContent = VfsUtilCore.loadText(classMapFile);

        var expectedOutputFile = myFixture.copyFileToProject("classMaps/ScannerJobsController_authenticated_user_admin_can_defer_a_finding.txt");
        var expectedOutputContent = VfsUtilCore.loadText(expectedOutputFile);

        var iterator = new DebuggingClassMapIterator();
        iterator.parse(classMapContent);

        // compare as sorted lists, because the VSCode data has a different sort order
        var separator = SystemInfoRt.isWindows ? "\r\n" : "\n";
        var sortedExpected = prepareSortedList(StringUtil.split(expectedOutputContent, separator));
        var sortedActual = prepareSortedList(iterator.ids);

        assertEquals(sortedExpected, sortedActual);
    }

    @NotNull
    private static List<String> prepareSortedList(List<String> items) {
        return items.stream().map(item -> item + "\n").sorted(String::compareTo).collect(Collectors.toList());
    }
}