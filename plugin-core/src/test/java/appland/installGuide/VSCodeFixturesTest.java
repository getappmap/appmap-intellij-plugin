package appland.installGuide;

import appland.AppMapBaseTest;
import appland.installGuide.projectData.ProjectDataService;
import com.intellij.openapi.application.WriteAction;
import org.junit.Assert;
import org.junit.Test;

public class VSCodeFixturesTest extends AppMapBaseTest {
    @Override
    protected String getBasePath() {
        return "vscode/workspaces";
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void projectA() {
        // copy into src, which is the only content root of the test project
        WriteAction.runAndWait(() -> myFixture.copyDirectoryToProject("project-a", ""));

        var projects = ProjectDataService.getInstance(getProject()).getAppMapProjects(true);
        Assert.assertEquals(1, projects.size());
    }
}
