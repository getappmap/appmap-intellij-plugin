package appland.cli;

import appland.AppMapBaseTest;
import appland.testRules.MockServerSettingsRule;
import appland.testRules.OverrideIdeHttpProxyRule;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class DefaultAppLandDownloadServiceProxyTest extends AppMapBaseTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, false);
    @Rule
    public MockServerSettingsRule mockServerSettingsRule = new MockServerSettingsRule();
    @Rule
    public OverrideIdeHttpProxyRule ideHttpProxyRule = new OverrideIdeHttpProxyRule(mockServerRule);

    private @Nullable String originalManifestUrl;

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Before
    public void removeMockBinary() {
        // The mock manifest uses version "1.2.3" and the download writes a fake binary to the
        // persistent cache. Delete it before each run so isDownloaded() never short-circuits
        // the download and the proxy assertion always holds.
        deleteMockBinary();
    }

    @AfterClass
    public static void cleanup() {
        TestAppLandDownloadService.removeDownloads();
        deleteMockBinary();
    }

    private static void deleteMockBinary() {
        var path = LocalAssetRepository.getExecutableFilePath(
                CliTool.AppMap, "1.2.3",
                CliPlatform.currentPlatform(), CliPlatform.currentArch(), true);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    @After
    public void resetManifest() {
        // Restore the original manifest URL after the test
        appland.settings.AppMapApplicationSettingsService.getInstance().setAppmapManifestUrl(originalManifestUrl);
    }

    @Test
    public void downloadWithHttpProxy() throws Exception {
        var toolType = CliTool.AppMap;

        // Clear manifest cache to ensure it fetches
        ManifestManager.clearCache();

        var manifestUrlString = "http://test/manifest.json";
        var binaryUrlString = "http://test/binary.bin";
        
        // Mock manifest endpoint
        var platformId = CliPlatform.getId();
        var manifestJson = String.format("{\"tag_name\":\"v1.2.3\",\"assets\":[{\"name\":\"appmap-%s\",\"url\":\"%s\"}]}", platformId, binaryUrlString);
        mockServerRule.getClient()
                .when(HttpRequest.request().withMethod("GET").withPath("/manifest.json"))
                .respond(HttpResponse.response().withBody(manifestJson));

        // Mock binary endpoint
        mockServerRule.getClient()
                .when(HttpRequest.request().withMethod("GET").withPath("/binary.bin"))
                .respond(HttpResponse.response().withBody("mock binary content"));

        var settings = appland.settings.AppMapApplicationSettingsService.getInstance();

        // store the original manifest URL to restore it after the test
        originalManifestUrl = settings.getAppmapManifestUrl();

        // Override the manifest URL for the test via settings
        settings.setAppmapManifestUrlNotifying(manifestUrlString);

        // Perform the download using the mock HTTP proxy server
        AppLandDownloadServiceTestUtil.assertDownloadLatestCliVersions(getProject(), toolType, getTestRootDisposable());

        var hasManifestRequest = Arrays.stream(mockServerRule.getClient().retrieveRecordedRequests(null))
                .anyMatch(request -> request.containsHeader("host", "test") && request.getPath().getValue().equals("/manifest.json"));
        var hasBinaryRequest = Arrays.stream(mockServerRule.getClient().retrieveRecordedRequests(null))
                .anyMatch(request -> request.containsHeader("host", "test") && request.getPath().getValue().equals("/binary.bin"));
                
        Assert.assertTrue("Manifest must be downloaded with the IDE's HTTP proxy", hasManifestRequest);
        Assert.assertTrue("AppMap CLI tools must be downloaded with the IDE's HTTP proxy", hasBinaryRequest);
    }
}