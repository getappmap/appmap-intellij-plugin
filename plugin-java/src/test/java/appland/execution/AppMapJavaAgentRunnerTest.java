package appland.execution;

import appland.AppLandTestExecutionPolicy;
import appland.AppMapPlugin;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AppMapJavaAgentRunnerTest extends BaseAppMapJavaTest {
    @Test
    public void testJavaRunConfiguration() throws Exception {
        var javaPath = Paths.get(AppLandTestExecutionPolicy.findAppMapHomePath()).resolve("appmap-java/simpleProject").toString();

        WriteAction.runAndWait(() -> {
            var module = createModuleFromTestData(javaPath, "simpleProject", StdModuleTypes.JAVA, true);
            ModuleRootModificationUtil.setModuleSdk(module, getTestProjectJdk());
        });

        var mainClass = ReadAction.compute(() -> myJavaFacade.findClass("com.simple.main"));
        var context = ReadAction.compute(() -> new ConfigurationContext(mainClass.findMethodsByName("main", true)[0]));
        assertNotNull(context);

        var runConfiguration = new ApplicationConfiguration("AppMap test", myProject);
        runConfiguration.setMainClass(mainClass);

        assertTrue(AppMapJvmExecutor.getInstance().isApplicable(myProject));
        assertNotNull(ProgramRunner.getRunner(AppMapJvmExecutor.EXECUTOR_ID, runConfiguration));

        var latch = new CountDownLatch(1);
        var runConfigDescriptor = new AtomicReference<RunContentDescriptor>();
        var env = ExecutionEnvironmentBuilder.create(AppMapJvmExecutor.getInstance(), runConfiguration).build(descriptor -> {
            runConfigDescriptor.set(descriptor);
            latch.countDown();
        });

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ProgramRunnerUtil.executeConfiguration(env, false, true);
            } catch (Exception e) {
                addSuppressedException(e);
            }
        });

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        var descriptor = runConfigDescriptor.get();
        assertNotNull(descriptor);

        var cmdline = descriptor.getProcessHandler().toString();
        assertTrue("The JVM command must be patched with the AppMap agent", cmdline.contains(AppMapPlugin.getJavaAgentPath().toString()));
    }
}