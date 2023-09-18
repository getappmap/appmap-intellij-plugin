package appland.toolwindow.codeObjects;

import appland.AppMapBaseTest;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CodeObjectsModelTest extends AppMapBaseTest {
    @Test
    public void simpleProject() throws IOException {
        myFixture.copyDirectoryToProject("classMaps/projectSingleFile", "root");
        var model = new CodeObjectsModel(getProject(), getTestRootDisposable());
        var rootNode = (RootNode) model.getRoot();
        var topLevel = model.getChildren(rootNode);

        assertSize(3, topLevel);
        assertNodeStructure(model, topLevel.get(0), "Code", "classMaps/projectSingleFile/codeObjects.packages.txt");
        assertNodeStructure(model, topLevel.get(1), "HTTP server requests", "classMaps/projectSingleFile/codeObjects.http.txt");
        assertNodeStructure(model, topLevel.get(2), "Queries", "classMaps/projectSingleFile/codeObjects.database.txt");
    }

    /**
     * Ensure that only one tree node is created even if there are several classMap.json sources for the same node path.
     */
    @Test
    public void duplicateIdsProject() throws IOException {
        myFixture.copyDirectoryToProject("classMaps/projectDuplicateIds", "root");
        var model = new CodeObjectsModel(getProject(), getTestRootDisposable());
        var rootNode = (RootNode) model.getRoot();
        var topLevel = model.getChildren(rootNode);

        assertSize(3, topLevel);
        assertNodeStructure(model, topLevel.get(0), "Code", "classMaps/projectDuplicateIds/codeObjects.packages.txt");
        assertNodeStructure(model, topLevel.get(1), "HTTP server requests", "classMaps/projectDuplicateIds/codeObjects.http.txt");
        assertNodeStructure(model, topLevel.get(2), "Queries", "classMaps/projectDuplicateIds/codeObjects.database.txt");
    }

    private void assertNodeStructure(@NotNull CodeObjectsModel model,
                                     @NotNull Node node,
                                     @NotNull String nodeDisplayName,
                                     @NotNull String expectedOutputFileName) throws IOException {
        assertEquals(nodeDisplayName, node.getDisplayName());
        assertTreeNodes(model, node, expectedOutputFileName);
    }

    private void assertTreeNodes(@NotNull CodeObjectsModel model,
                                 @NotNull Node node,
                                 @NotNull String expectedOutputFileName) throws IOException {
        var output = new StringBuilder();
        buildTreeRepresentation(model, node, output, 0);
        var fileContent = Files.readString(Paths.get(getTestDataPath()).resolve(expectedOutputFileName));
        var expectedOutput = StringUtil.convertLineSeparators(fileContent).trim();
        assertEquals("Tree rendering does not match expected file content: " + expectedOutputFileName, expectedOutput, output.toString().trim());
    }

    private void buildTreeRepresentation(@NotNull CodeObjectsModel model, @NotNull Node node, @NotNull StringBuilder output, int indentationLevel) {
        var prefix = StringUtil.repeat("  ", indentationLevel);
        var type = node instanceof AbstractClassMapItemNode ? ((AbstractClassMapItemNode) node).getNodeType().getName() : null;
        var suffix = type != null ? " (" + type + ")" : "";
        output.append(prefix).append(node.getDisplayName()).append(suffix).append("\n");
        for (var child : model.getChildren(node)) {
            buildTreeRepresentation(model, child, output, indentationLevel + 1);
        }
    }
}
