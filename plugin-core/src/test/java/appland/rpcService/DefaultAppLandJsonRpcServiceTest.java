package appland.rpcService;

import appland.AppMapBaseTest;
import appland.cli.AppLandCommandLineService;
import appland.cli.TestAppLandDownloadService;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapSecureApplicationSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.settings.AppMapSettingsReloadProjectListener;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
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
    @Before
    public void setupListener() {
        // by default, the listener is inactive in test mode to avoid side-effects
        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsReloadProjectListener());
    }

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
                        new AppLandJsonRpcListener() {
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
            var commandLine = AppLandCommandLineService.getInstance().createAppMapJsonRpcCommand(null);
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
    public void restartAfterOpenAIKeyChanged() throws Exception {
        AppMapSecureApplicationSettingsService.getInstance().setOpenAIKey(null);

        waitForJsonRpcServer();
        try {
            var latch = createWaitForJsonRpcServerRestartCondition(false);

            AppMapSecureApplicationSettingsService.getInstance().setOpenAIKey("my-open-ai-key");

            assertTrue("The JSON-RPC server must restart after a change to the OpenAI key", latch.await(60, TimeUnit.SECONDS));
        } finally {
            AppMapSecureApplicationSettingsService.getInstance().setOpenAIKey(null);
        }
    }

    @Test
    public void restartAfterAppMapEnvChanged() throws Exception {
        var baseEnv = Map.of("APPMAP_NAVIE_MODEL", "my-custom-model");
        AppMapApplicationSettingsService.getInstance().setCliEnvironment(baseEnv);

        waitForJsonRpcServer();
        try {
            var latch = createWaitForJsonRpcServerRestartCondition(false);

            var modifiedEnv = Map.of("APPMAP_NAVIE_MODEL", "my-custom-model", "OPENAI_BASE_URL", "http://example.com");
            AppMapApplicationSettingsService.getInstance().setCliEnvironmentNotifying(modifiedEnv);

            assertTrue("The JSON-RPC server must restart after a change to the OpenAI key", latch.await(60, TimeUnit.SECONDS));
        } finally {
            AppMapApplicationSettingsService.getInstance().setCliEnvironment(Map.of());
        }
    }

    @Test
    public void restartAfterCopilotEnabled() throws Exception {
        AppMapApplicationSettingsService.getInstance().setCopilotIntegrationDisabled(true);

        waitForJsonRpcServer();
        try {
            var latch = createWaitForJsonRpcServerRestartCondition(false);

            AppMapApplicationSettingsService.getInstance().setCopilotIntegrationDisabledNotifying(false);

            assertTrue("The JSON-RPC server must restart after Copilot state changed", latch.await(60, TimeUnit.SECONDS));
        } finally {
            AppMapApplicationSettingsService.getInstance().setCliEnvironment(Map.of());
        }
    }

    @Test
    public void ensureServerPortIsReused() throws Exception {
        AppMapApplicationSettingsService.getInstance().setCopilotIntegrationDisabled(true);

        waitForJsonRpcServer();
        var firstPort = AppLandJsonRpcService.getInstance(getProject()).getServerPort();

        try {
            var latch = createWaitForJsonRpcServerRestartCondition(false);

            AppMapApplicationSettingsService.getInstance().setCopilotIntegrationDisabledNotifying(false);
            assertTrue("The JSON-RPC server must restart after Copilot state changed", latch.await(60, TimeUnit.SECONDS));

            var newPort = AppLandJsonRpcService.getInstance(getProject()).getServerPort();
            assertEquals("Port must remain the same after a restart", firstPort, newPort);
        } finally {
            AppMapApplicationSettingsService.getInstance().setCliEnvironment(Map.of());
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