package appland.execution;

import appland.cli.AppLandCommandLineService;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.JavaPsiTestCase;

public abstract class BaseAppMapJavaTest extends JavaPsiTestCase {
    @Override
    protected void setUp() throws Exception {
        EdtTestUtil.runInEdtAndWait(super::setUp);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            AppLandCommandLineService.getInstance().stopAll(true);
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            EdtTestUtil.runInEdtAndWait(super::tearDown);
        }
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Override
    protected Sdk getTestProjectJdk() {
        // we need Java 11 for our AppMap tests, but CI has Java 17 to build and compile the plugin
        var customJdkPath = System.getenv("APP_MAP_JDK");
        if (customJdkPath != null) {
            Logger.getInstance(BaseAppMapJavaTest.class).warn("Using custom JDK for test setup: " + customJdkPath);

            var sdk = ExternalSystemJdkUtil.addJdk(customJdkPath);
            Disposer.register(getTestRootDisposable(), () -> {
                WriteAction.runAndWait(() -> {
                    JavaAwareProjectJdkTableImpl.getInstanceEx().removeJdk(sdk);
                });
            });
            return sdk;
        }

        //noinspection UnstableApiUsage,deprecation
        return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    }
}
