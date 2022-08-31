package appland;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.jetbrains.annotations.NotNull;

public abstract class AppMapBaseTest extends LightPlatformCodeInsightFixture4TestCase {
    /**
     * Creates a minimal version of AppMap JSON, which contains the metadata with a name.
     */
    public String createAppMapMetadataJSON(@NotNull String name) {
        return String.format("{\"metadata\": { \"name\": \"%s\" }}", name);
    }

    public String createAppMapMetadataJSON(@NotNull String name, int requestCount, int queryCount, int functionCount) {
        var events = createAppMapEvents(requestCount, queryCount);
        var classMap = createClassMap(functionCount);
        return String.format("{\n\"metadata\": { \"name\": \"%s\" },\n \"events\": %s\n,\n \"classMap\": %s\n}", name, events, classMap);
    }

    @NotNull
    public VirtualFile createTempDir(@NotNull String name) {
        var psiFile = myFixture.addFileToProject(name + "/file.txt", "");
        return ReadAction.compute(() -> psiFile.getParent().getVirtualFile());
    }

    private String createAppMapEvents(int requestCount, int queryCount) {
        var json = new StringBuilder();
        json.append("[");

        for (var i = 0; i < requestCount; i++) {
            json.append("{\"http_server_request\": {}}\n");
            if (i < requestCount - 1) {
                json.append(",");
            }
        }

        if (requestCount > 0 && queryCount > 0) {
            json.append(",");
        }

        for (var i = 0; i < queryCount; i++) {
            json.append("{\"sql_query\": {}}\n");
            if (i < queryCount - 1) {
                json.append(",");
            }
        }

        json.append("]");
        return json.toString();
    }

    private String createClassMap(int functionCount) {
        var json = new StringBuilder();
        json.append("[");

        for (var i = 0; i < functionCount; i++) {
            if (i % 2 == 0) {
                json.append("{\"type\": \"function\"}\n");
            } else {
                // nesting for odd numbers to test the recursive parsing
                json.append("{\"type\": \"package\", \"children\": [ {\"type\": \"function\"} ]}\n");
            }
            if (i < functionCount - 1) {
                json.append(",");
            }
        }

        json.append("]");
        return json.toString();
    }
}
