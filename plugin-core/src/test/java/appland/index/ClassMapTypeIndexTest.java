package appland.index;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ClassMapTypeIndexTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void emptyResult() {
        assertItemCount(ClassMapItemType.Package, 0);
        assertItemCount(ClassMapItemType.Query, 0);
        assertItemCount(ClassMapItemType.HTTP, 0);
    }

    @Test
    public void singleFile() {
        // use fixture dir, because the index only analyzes files with name classMap.json
        myFixture.copyDirectoryToProject("classMaps/projectSingleFile", "root");

        // root items
        assertItemCount(ClassMapItemType.Database, 1);
        assertItemCount(ClassMapItemType.HTTP, 1);

        // leafs
        assertItemCount(ClassMapItemType.Package, 22 + 2);
        assertItemCount(ClassMapItemType.Class, 80);
        assertItemCount(ClassMapItemType.Query, 23);
        assertItemCount(ClassMapItemType.Route, 1);

        // each of the leaf item results must have two files attached, because the project contains a duplicate classMap
        assertItemsFileCount("One associated AppMap file expected",
                1,
                List.of(ClassMapItemType.Package, ClassMapItemType.Class, ClassMapItemType.Query, ClassMapItemType.Route));
    }

    @Test
    public void unexpectedClassMapType() {
        // use fixture dir, because the index only analyzes files with name classMap.json
        myFixture.copyDirectoryToProject("classMaps/unexpectedClassMapType", "root");

        // items must still be found even if one of the items has an unknown class map type
        assertItemCount(ClassMapItemType.Database, 1);
        assertItemCount(ClassMapItemType.HTTP, 1);
    }

    @Test
    public void duplicateItems() {
        // use fixture dir, because the index only analyzes files with name classMap.json
        myFixture.copyDirectoryToProject("classMaps/projectDuplicateIds", "root");

        // root items
        assertItemCount(ClassMapItemType.Database, 1);
        assertItemCount(ClassMapItemType.HTTP, 1);

        // each of the leaf item results must have two files attached, because the project contains a duplicate classMap
        assertItemsFileCount("Two associated AppMap files expected",
                2,
                List.of(ClassMapItemType.Package, ClassMapItemType.Class, ClassMapItemType.Query, ClassMapItemType.Route));
    }

    @Test
    public void insideExcluded() throws Exception {
        var root = myFixture.copyDirectoryToProject("classMaps/projectDuplicateIds", "root");

        // AppMapIndexedRootsSetContributor only un-excludes directories in a content root with appmap.yml
        createAppMapYaml(root.getParent(), root.getName());

        withExcludedFolder(root, () -> {
            // root items, classMap files in excluded folders must be processed by the query
            assertItemCount(ClassMapItemType.Database, 1);
            assertItemCount(ClassMapItemType.HTTP, 1);
        });
    }

    private void assertItemCount(@NotNull ClassMapItemType itemType, int expectedSize) {
        assertSize(expectedSize, ReadAction.compute(() -> ClassMapTypeIndex.findItems(getProject(), itemType)));
    }

    private void assertItemsFileCount(String message, int expected, List<ClassMapItemType> types) {
        for (var type : types) {
            var result = ReadAction.compute(() -> ClassMapTypeIndex.findItems(getProject(), type));
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