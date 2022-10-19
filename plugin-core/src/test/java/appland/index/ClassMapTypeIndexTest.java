package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ClassMapTypeIndexTest extends AppMapBaseTest {
    @Test
    public void emptyResult() {
        assertEmpty(ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Package));
        assertEmpty(ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Query));
        assertEmpty(ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.HTTP));
    }

    @Test
    public void singleFile() {
        // use fixture dir, because the index only analyzes files with name classMap.json
        myFixture.copyDirectoryToProject("classMaps/projectSingleFile", "root");

        // root items
        assertSize(1, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Database));
        assertSize(1, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.HTTP));

        // leafs
        assertSize(22 + 2, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Package));
        assertSize(80, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Class));
        assertSize(23, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Query));
        assertSize(1, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Route));

        // each of the leaf item results must have two files attached, because the project contains a duplicate classMap
        assertItemsFileCount("One associated AppMap file expected",
                1,
                List.of(ClassMapItemType.Package, ClassMapItemType.Class, ClassMapItemType.Query, ClassMapItemType.Route));
    }

    @Test
    public void duplicateItems() {
        // use fixture dir, because the index only analyzes files with name classMap.json
        myFixture.copyDirectoryToProject("classMaps/projectDuplicateIds", "root");

        // root items
        assertSize(1, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.Database));
        assertSize(1, ClassMapTypeIndex.findItems(getProject(), ClassMapItemType.HTTP));

        // each of the leaf item results must have two files attached, because the project contains a duplicate classMap
        assertItemsFileCount("Two associated AppMap files expected",
                2,
                List.of(ClassMapItemType.Package, ClassMapItemType.Class, ClassMapItemType.Query, ClassMapItemType.Route));
    }

    private void assertItemsFileCount(String message, int expected, List<ClassMapItemType> types) {
        for (var type : types) {
            var result = ClassMapTypeIndex.findItems(getProject(), type);
            assertFalse(result.isEmpty());

            for (var value : result.values()) {
                assertEquals(message, expected, value.size());
            }
        }
    }

    private void assertEmpty(@NotNull Map<ClassMapItem, List<VirtualFile>> data) {
        assertSize(0, data);
    }

    private void assertSize(int expectedSize, @NotNull Map<ClassMapItem, List<VirtualFile>> data) {
        assertEquals(expectedSize, data.size());
    }
}