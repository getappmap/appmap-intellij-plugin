package appland.rpcService;

import appland.AppMapBaseTest;
import appland.cli.AppLandCommandLineService;
import appland.cli.TestAppLandDownloadService;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertArrayEquals;

public class DefaultAppLandJsonRpcServiceTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Before
    public void ensureToolsDownloaded() {
        TestAppLandDownloadService.ensureDownloaded();
    }

    @Test
    public void launchedWithProject() {
        waitForJsonRpcServer();

        var isRunning = AppLandJsonRpcService.getInstance(getProject()).isServerRunning();
        assertTrue("JSON-RPC server must be launched when a project is opened", isRunning);
    }

    @Test
    public void serverConfigurationRefreshAfterConfigChange() throws Exception {
        waitForJsonRpcServer();

        var latch = new CountDownLatch(1);
        var serverConfigFiles = new AtomicReference<Collection<VirtualFile>>();
        getProject().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppLandJsonRpcListener.TOPIC,
                        new AppLandJsonRpcListenerAdapter() {
                            @Override
                            public void serverConfigurationUpdated(@NotNull Collection<VirtualFile> contentRoots,
                                                                   @NotNull Collection<VirtualFile> appMapConfigFiles) {
                                serverConfigFiles.set(appMapConfigFiles);
                                latch.countDown();
                            }
                        });

        var appMapConfig = myFixture.copyFileToProject("appmap-config/appmap.yml");

        assertTrue("An AppMap config update must be sent to the JSON-RPC server", latch.await(30, TimeUnit.SECONDS));
        assertArrayEquals("The updated config file path must be sent",
                new VirtualFile[]{appMapConfig},
                serverConfigFiles.get().toArray());
    }

    @Test
    public void serverRestartAfterTermination() throws Exception {
        waitForJsonRpcServer();

        var latch = createWaitForJsonRpcServerRestartCondition(false);

        // kill and wait for restart
        TestAppLandJsonRpcService.killJsonRpcProcess(getProject());
        assertTrue("The JSON-RPC server must restart after unexpected termination", latch.await(60, TimeUnit.SECONDS));
    }

    @Test
    public void serverEnvironment() {
        final var settings = AppMapApplicationSettingsService.getInstance();
        settings.setApiKey("dummy");
        settings.setCliEnvironment(Map.of("FOO", "BAR", "BAZ", "QUX"));
        try {
            var commandLine = AppLandCommandLineService.getInstance().createAppMapJsonRpcCommand();
            assert commandLine != null;
            assertThat(commandLine.getEffectiveEnvironment(), allOf(
                    hasEntry("APPMAP_API_KEY", "dummy"),
                    hasEntry("FOO", "BAR"),
                    hasEntry("BAZ", "QUX")));
        } finally {
            settings.setApiKey(null);
            settings.setCliEnvironment(Map.of());
        }
    }

    @Test
    public void restartAfterApiKeyChange() throws Exception {
        waitForJsonRpcServer();

        var appMapSettings = AppMapApplicationSettingsService.getInstance();
        appMapSettings.setApiKey("dummy");
        try {
            var latch = createWaitForJsonRpcServerRestartCondition(false);

            // change API key and wait for restart
            appMapSettings.setApiKeyNotifying("new-api-key");

            assertTrue("The JSON-RPC server must restart after unexpected termination", latch.await(60, TimeUnit.SECONDS));
        } finally {
            appMapSettings.setApiKey(null);
        }
    }

    @Test
    public void codeEditorInfo() {
        // only run with IntelliJ Community
        Assume.assumeTrue("IC".equals(ApplicationInfo.getInstance().getBuild().getProductCode()));
        var editorInfo = DefaultAppLandJsonRpcService.createCodeEditorInfo();

        // For example:
        // IntelliJ IDEA 2023.2 by JetBrains s.r.o.
        // IntelliJ IDEA 2024.3 EAP by JetBrains s.r.o.
        var matches = editorInfo.matches("IntelliJ IDEA [0-9.]+( EAP| Beta)? by JetBrains s\\.r\\.o\\.");
        assertTrue("Code editor info must match: " + editorInfo, matches);
    }
}