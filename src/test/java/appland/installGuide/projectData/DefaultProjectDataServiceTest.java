package appland.installGuide.projectData;

import appland.AppMapBaseTest;
import com.intellij.openapi.application.WriteAction;
import org.junit.Test;

public class DefaultProjectDataServiceTest extends AppMapBaseTest {
    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void truncatedSampleObjects() {
        WriteAction.runAndWait(() -> myFixture.copyDirectoryToProject("appmap-scanner/with_findings", "root"));

        var projects = ProjectDataService.getInstance(getProject()).getAppMapProjects();
        assertEquals(1, projects.size());

        var samples = projects.get(0).sampleCodeObjects;
        assertNotNull(samples);

        var queries = samples.getQueries();
        assertEquals("Only the first 5 queries must be returned", 5, queries.size());
        // VSCode is showing these queries
        assertEquals("select owner0_.id as id1_0_0_, pets1_.id as id1_1_1_, owner0_.first_name as first_na2_0_0_, owner0_.last_name as last_nam3_0_0_, owner0_.address as address4_0_0_, owner0_.city as city5_0_0_, owner0_.telephone as telephon6_0_0_, pets1_.name as name2_1_1_, pets1_.birth_date as birth_da3_1_1_, pets1_.type_id as type_id4_1_1_, pets1_.owner_id as owner_id5_1_0__, pets1_.id as id1_1_0__ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.id=? order by pets1_.name", queries.get(0).name);
        assertEquals("select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?", queries.get(1).name);
        assertEquals("select visits0_.pet_id as pet_id4_6_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_ from visits visits0_ where visits0_.pet_id=? order by visits0_.visit_date asc", queries.get(2).name);
        assertEquals("select visits1_.pet_id as pet_id4_6_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_ from visits visits0_ where visits0_.pet_id=? order by visits0_.visit_date asc", queries.get(3).name);
        assertEquals("select visits2_.pet_id as pet_id4_6_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_ from visits visits0_ where visits0_.pet_id=? order by visits0_.visit_date asc", queries.get(4).name);

        var requests = samples.getHttpRequests();
        assertEquals("Only the first 5 requests must be returned", 5, requests.size());
        // VSCode is showing these requests
        assertEquals("GET /owners/:ownerId", requests.get(0).name);
        assertEquals("GET /owners/:ownerId/1", requests.get(1).name);
        assertEquals("GET /owners/:ownerId/2", requests.get(2).name);
        assertEquals("GET /owners/:ownerId/3", requests.get(3).name);
        assertEquals("GET /owners/:ownerId/4", requests.get(4).name);
    }
}