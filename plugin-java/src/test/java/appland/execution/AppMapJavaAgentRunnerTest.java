package appland.execution;

import appland.AppLandTestExecutionPolicy;
import appland.javaAgent.AppMapJavaAgentDownloadService;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.process.ProcessHandler;
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
    private ProcessHandler processHandler = null;

    @Override
    protected void tearDown() throws Exception {
        try {
            if (processHandler != null) {
                LOG.info("Terminating down run configuration process...");
                processHandler.destroyProcess();
                processHandler.waitFor(5_000);
                processHandler = null;
            }
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

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
        // 2023.1+ needs a ReadAction
        ReadAction.run(() -> runConfiguration.setMainClass(mainClass));

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

        processHandler = descriptor.getProcessHandler();
        assertNotNull(processHandler);
        var cmdline = processHandler.toString();

        var agentJarPath = AppMapJavaAgentDownloadService.getInstance().getJavaAgentPathIfExists();
        assertNotNull(agentJarPath);
        assertTrue("The JVM command must be patched with the AppMap agent", cmdline.contains(agentJarPath.toString()));
    }
}