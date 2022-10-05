package appland.execution;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.JavaPsiTestCase;

public abstract class BaseAppMapJavaTest extends JavaPsiTestCase {
    @Override
    protected void setUp() throws Exception {
        EdtTestUtil.runInEdtAndWait(super::setUp);
    }

    @Override
    protected void tearDown() throws Exception {
        EdtTestUtil.runInEdtAndWait(super::tearDown);
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Override
    protected Sdk getTestProjectJdk() {
        //noinspection UnstableApiUsage
        return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    }
}
