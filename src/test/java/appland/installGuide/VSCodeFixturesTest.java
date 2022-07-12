package appland.installGuide;

import appland.AppMapBaseTest;
import appland.installGuide.projectData.ProjectDataService;
import org.junit.Assert;
import org.junit.Test;

public class VSCodeFixturesTest extends AppMapBaseTest {
    @Override
    protected String getBasePath() {
        return "vscode/workspaces";
    }

    @Test
    public void projectA() {
        // copy into src, which is the only content root of the test project
        var root = myFixture.copyDirectoryToProject("project-a", "");

        var projects = ProjectDataService.getInstance(getProject()).getAppMapProjects();
        Assert.assertEquals(1, projects.size());
    }
}
