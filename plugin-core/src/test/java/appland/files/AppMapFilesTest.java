package appland.files;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AppMapFilesTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void files() {
        var file = myFixture.configureByText("a.json", "");
        assertFalse(AppMapFiles.isAppMap(file.getVirtualFile()));

        file = myFixture.configureByText("a.appmap.json", "");
        assertTrue(AppMapFiles.isAppMap(file.getVirtualFile()));
    }

    @Test
    public void updateMapMapName() throws IOException {
        var file = FileUtilRt.createTempFile("appmap-test", ".appmap.json").toPath();
        Files.write(file, "{}".getBytes(StandardCharsets.UTF_8));

        var success = AppMapFiles.updateMetadata(file, "updated AppMap name");
        assertTrue(success);
        assertEquals("{\"metadata\":{\"name\":\"updated AppMap name\"}}", Files.readString(file));
    }

    @Test
    public void updateExistingMapMapName() throws IOException {
        var file = FileUtilRt.createTempFile("appmap-test", ".appmap.json").toPath();
        Files.write(file, "{\"metadata\": {\"name\":\"old AppMap name\"}}".getBytes(StandardCharsets.UTF_8));

        var success = AppMapFiles.updateMetadata(file, "updated AppMap name");
        assertTrue(success);
        assertEquals("{\"metadata\":{\"name\":\"updated AppMap name\"}}", Files.readString(file));
    }
}