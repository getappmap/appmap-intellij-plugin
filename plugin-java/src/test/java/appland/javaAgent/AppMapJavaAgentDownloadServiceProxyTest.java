package appland.javaAgent;

import appland.AppMapBaseTest;
import appland.testRules.MockServerSettingsRule;
import appland.testRules.OverrideIdeHttpProxyRule;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockserver.junit.MockServerRule;
import org.mockserver.verify.VerificationTimes;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class AppMapJavaAgentDownloadServiceProxyTest extends AppMapBaseTest {
    @Rule
    public TestRule agentDownloadRule = new OverrideJavaAgentLocationRule(() -> this.myFixture);
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    @Rule
    public MockServerSettingsRule mockServerSettingsRule = new MockServerSettingsRule();
    @Rule
    public OverrideIdeHttpProxyRule ideHttpProxyRule = new OverrideIdeHttpProxyRule(mockServerRule);

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // the override of the download location needs real files on disk
        return new TempDirTestFixtureImpl();
    }

    @Test
    public void httpProxyConnection() throws Throwable {
        var service = AppMapJavaAgentDownloadService.getInstance();

        Assert.assertNull("Java agent must not yet be downloaded", service.getJavaAgentPathIfExists());
        var wasDownloaded = service.downloadJavaAgentSync(new EmptyProgressIndicator());
        Assert.assertTrue("The Agent JAR must download using a proxy", wasDownloaded);

        mockServerRule.getClient().verify(
                request()
                    .withHeader("Host", MavenRelease.INSTANCE.getDownloadHost())
                    .withPath(".*/(appmap.*?.jar)"),
                VerificationTimes.once()
        );
    }

    @Test
    public void fallsBackToGitHub() throws Throwable {
        var service = AppMapJavaAgentDownloadService.getInstance();

        var client = mockServerRule.getClient();
        client.when(
            request()
                .withHeader("Host", MavenRelease.INSTANCE.getDownloadHost())
        ).respond(
            response().withStatusCode(403)
        );

        Assert.assertNull("Java agent must not yet be downloaded", service.getJavaAgentPathIfExists());
        var wasDownloaded = service.downloadJavaAgentSync(new EmptyProgressIndicator());
        Assert.assertTrue("The Agent JAR must download using a proxy", wasDownloaded);

        client.verify(
                request()
                    .withHeader("Host", GitHubRelease.INSTANCE.getDownloadHost())
                    .withPath(".*/(appmap.*?.jar)"),
                VerificationTimes.once()
        );
    }
}
