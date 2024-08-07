package appland.cli;

import appland.AppMapBaseTest;
import appland.testRules.MockServerSettingsRule;
import appland.testRules.OverrideIdeHttpProxyRule;
import com.intellij.util.Urls;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Arrays;

import static appland.cli.DefaultAppLandDownloadService.currentArch;
import static appland.cli.DefaultAppLandDownloadService.currentPlatform;

public class DefaultAppLandDownloadServiceProxyTest extends AppMapBaseTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, false);
    @Rule
    public MockServerSettingsRule mockServerSettingsRule = new MockServerSettingsRule();
    @Rule
    public OverrideIdeHttpProxyRule ideHttpProxyRule = new OverrideIdeHttpProxyRule(mockServerRule);

    @Test
    public void downloadWithHttpProxy() throws Exception {
        var toolType = CliTool.AppMap;

        var latestVersion = AppLandDownloadService.getInstance().fetchLatestRemoteVersion(toolType);
        Assert.assertNotNull(latestVersion);

        var binaryDownloadUrl = Urls.parse(toolType.getDownloadUrl(latestVersion, currentPlatform(), currentArch()), false);
        Assert.assertNotNull(binaryDownloadUrl);

        // override response body of the binary request to avoid OutOfMemory leaks with Mockserver
        mockServerRule.getClient()
                .when(HttpRequest.request().withMethod("GET").withPath(binaryDownloadUrl.getPath()))
                .respond(HttpResponse.response().withBody(""));

        // Perform the download using the mock HTTP proxy server
        AppLandDownloadServiceTestUtil.downloadLatestCliVersions(getProject(), toolType, getTestRootDisposable());

        var hasDownloadRequest = Arrays.stream(mockServerRule.getClient().retrieveRecordedRequests(null))
                .anyMatch(request -> {
                    var path = request.getPath().getValue();
                    return request.containsHeader("host", binaryDownloadUrl.getAuthority()) && path.equals(binaryDownloadUrl.getPath());
                });
        Assert.assertTrue("AppMap CLI tools must be downloaded with the IDE's HTTP proxy", hasDownloadRequest);
    }
}