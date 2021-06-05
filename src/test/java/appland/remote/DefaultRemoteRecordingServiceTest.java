package appland.remote;

import appland.AppMapBaseTest;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static appland.remote.DefaultRemoteRecordingService.url;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class DefaultRemoteRecordingServiceTest extends AppMapBaseTest {
    @Rule
    public final WireMockRule serverRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // create temp files on disk
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void urls() {
        assertEquals("https://host.name/_appmap/record", url("https://host.name", "_appmap/record"));
        assertEquals("https://host.name/_appmap/record", url("https://host.name", "/_appmap/record"));
        assertEquals("https://host.name/_appmap/record", url("https://host.name/", "_appmap/record"));
        assertEquals("https://host.name/_appmap/record", url("https://host.name/", "/_appmap/record"));
    }

    @Test
    public void startRecording() {
        serverRule.stubFor(post(DefaultRemoteRecordingService.URL_SUFFIX).willReturn(ok()));

        var started = RemoteRecordingService.getInstance().startRecording(serverRule.baseUrl());
        assertTrue("startRecording should return true for status == 200", started);
    }

    @Test
    public void startRecordingFailing() {
        serverRule.stubFor(post(DefaultRemoteRecordingService.URL_SUFFIX).willReturn(status(HttpStatus.SC_CONFLICT)));

        var started = RemoteRecordingService.getInstance().startRecording(serverRule.baseUrl());
        assertFalse("startRecording should return false for status == 409", started);
    }

    @Test
    public void recordingStatusActive() {
        serverRule.stubFor(get(DefaultRemoteRecordingService.URL_SUFFIX).willReturn(ok("{\"enabled\": true}")));

        var started = RemoteRecordingService.getInstance().isRecording(serverRule.baseUrl());
        assertTrue("startRecording should return false for status == 409", started);
    }

    @Test
    public void recordingStatusInactive() {
        serverRule.stubFor(get(DefaultRemoteRecordingService.URL_SUFFIX).willReturn(ok("{\"enabled\": false}")));

        var started = RemoteRecordingService.getInstance().isRecording(serverRule.baseUrl());
        assertFalse("startRecording should return false for status == 409", started);
    }

    @Test
    public void stopRecording() throws IOException {
        serverRule.stubFor(delete(DefaultRemoteRecordingService.URL_SUFFIX).willReturn(ok("{\"metadata\":{\"description\":\"my description\"}}")));

        var tempDir = Paths.get(myFixture.getTempDirFixture().getTempDirPath());
        var newFile = ProgressManager.getInstance().runProcessWithProgressSynchronously((ThrowableComputable<Path, IOException>) () -> {
            return RemoteRecordingService.getInstance().stopRecording(serverRule.baseUrl(), tempDir, "new name");
        }, "", false, getProject());
        assertNotNull("stopRecording should return the file for status == 200", newFile);
        assertEquals("{\"metadata\":{\"description\":\"my description\",\"name\":\"new name\"}}", Files.readString(newFile));
    }

    @Test
    public void stopRecordingFailing() throws IOException {
        serverRule.stubFor(delete(DefaultRemoteRecordingService.URL_SUFFIX).willReturn(status(HttpStatus.SC_NOT_FOUND)));

        var tempDir = Paths.get(myFixture.getTempDirFixture().getTempDirPath());
        var newFile = ProgressManager.getInstance().runProcessWithProgressSynchronously((ThrowableComputable<Path, IOException>) () -> {
            return RemoteRecordingService.getInstance().stopRecording(serverRule.baseUrl(), tempDir, "new name");
        }, "", false, getProject());
        assertNull("stopRecording should null for status == 409", newFile);
    }

    @Test
    public void isRecordingPortOutOfRange() {
        // must not throw an exception
        assertFalse(RemoteRecordingService.getInstance().isRecording("http://www.example.com:80800"));
    }
}