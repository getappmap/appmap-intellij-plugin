package appland.rpcService;

import appland.AppMapBaseTest;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;

public class DefaultAppLandJsonRpcServiceTest extends AppMapBaseTest {
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
                            public void serverConfigurationUpdated(Collection<VirtualFile> appMapConfigFiles) {
                                serverConfigFiles.set(appMapConfigFiles);
                                latch.countDown();
                            }
                        });

        var appMapConfig = myFixture.copyFileToProject("appmap-config/appmap.yml");

        assertTrue("An AppMap config update must be sent to the JSON-RPC server", latch.await(30, TimeUnit.SECONDS));
        assertArrayEquals("The updated config file path must be sent", new VirtualFile[]{appMapConfig}, serverConfigFiles.get().toArray());
    }

    @Test
    public void serverRestartAfterTermination() throws Exception {
        waitForJsonRpcServer();

        var latch = new CountDownLatch(1);
        getProject().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppLandJsonRpcListener.TOPIC, new AppLandJsonRpcListenerAdapter() {
                    @Override
                    public void serverRestarted() {
                        latch.countDown();
                    }
                });

        // kill and wait for restart
        TestAppLandJsonRpcService.killJsonRpcProcess(getProject());
        assertTrue("The JSON-RPC server must restart after unexpected termination", latch.await(60, TimeUnit.SECONDS));
    }

    private void waitForJsonRpcServer() {
        var service = AppLandJsonRpcService.getInstance(getProject());

        var latch = new CountDownLatch(1);
        getProject().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppLandJsonRpcListener.TOPIC, new AppLandJsonRpcListenerAdapter() {
                    @Override
                    public void serverStarted() {
                        latch.countDown();
                    }
                });

        if (service.isServerRunning()) {
            return;
        }

        try {
            assertTrue("The AppMap JSON-RPC server must launch", latch.await(30, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            addSuppressedException(e);
        }
    }
}