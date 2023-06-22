package appland.javaAgent;

import appland.AppMapBaseTest;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AppMapJavaAgentDownloadServiceTest extends AppMapBaseTest {
    @Rule
    public final TestRule systemPropertyUpdate = (statement, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            var oldUserHome = System.getProperty("user.dir");
            try {
                var path = Paths.get(myFixture.getTempDirFixture().getTempDirPath()).resolve("appmap-agent");
                NioFiles.deleteRecursively(path);

                System.setProperty("user.dir", path.toString());
                statement.evaluate();
            } finally {
                System.setProperty("user.dir", oldUserHome);
            }
        }
    };

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void agentDirPath(){
        var service = AppMapJavaAgentDownloadService.getInstance();
        var agentDir = service.getOrCreateAgentDir();
        assertNotNull(agentDir);

        var expectedParent = Paths.get(System.getProperty("user.dir")).resolve(".appmap");
        assertTrue("Agent dir must be under ~/.agent", agentDir.startsWith(expectedParent));
    }

    @Test
    public void multipleDownloadRequests() throws IOException {
        var service = AppMapJavaAgentDownloadService.getInstance();
        assertNull("JAR file must not exist in a clean temp directory", service.getJavaAgentPathIfExists());

        assertTrue("First attempt must download the file", service.downloadJavaAgentSync(new EmptyProgressIndicator()));
        assertNotNull("JAR file must exist after a download", service.getJavaAgentPathIfExists());
        if (SystemInfo.isUnix) {
            assertTrue("agent.jar must be a symbolic link", Files.isSymbolicLink(service.getJavaAgentPathIfExists()));
        }

        assertFalse("Second attempt must skip the download", service.downloadJavaAgentSync(new EmptyProgressIndicator()));
        assertNotNull("JAR file must still exist after a skipped download", service.getJavaAgentPathIfExists());
    }

    @Test
    public void lockFileGuard() throws IOException {
        var service = AppMapJavaAgentDownloadService.getInstance();

        var jarAsset = GitHubRelease.getLatestRelease(new EmptyProgressIndicator(), "getappmap", "appmap-java")
                .stream()
                .filter(asset -> asset.getFileName().endsWith(".jar"))
                .findFirst().orElse(null);
        assertNotNull(jarAsset);

        var agentDir = service.getOrCreateAgentDir();
        assertTrue("Agent target directory must exist", agentDir != null && Files.isDirectory(agentDir));

        var lockFilePath = agentDir.resolve(jarAsset.getFileName() + ".downloading");
        assertFalse(Files.exists(lockFilePath));
        Files.createFile(lockFilePath);

        assertFalse("Agent must not be downloaded if a new lock file exists", service.downloadJavaAgentSync(new EmptyProgressIndicator()));

        Files.setLastModifiedTime(lockFilePath, FileTime.from(Instant.now().minus(6, ChronoUnit.MINUTES)));
        assertTrue("Agent must be downloaded if an old lock file exists", service.downloadJavaAgentSync(new EmptyProgressIndicator()));
    }
}