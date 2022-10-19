package appland.index;

import appland.AppMapBaseTest;
import org.junit.Test;

import java.util.Comparator;

public class AppMapMetadataSampleFileTest extends AppMapBaseTest {
    @Test
    public void index() {
        assertEquals(0, AppMapMetadataIndex.findAppMaps(getProject(), null).size());

        myFixture.copyDirectoryToProject("appmap", "appmap");

        assertEquals(5, AppMapMetadataIndex.findAppMaps(getProject(), null).size());

        // misago_categories_admin_tests_test_permissions_admin_views_CategoryRoleAdminViewsTests_test_editing_role_invalidates_acl_cache.appmap.json
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "Category role admin views tests").size());

        // misago_threads_tests_test_threadview_ThreadPollViewTests_test_poll_unvoted_display.appmap.json
        assertEquals(1, AppMapMetadataIndex.findAppMaps(getProject(), "Thread poll view tests poll unvoted display").size());
    }
}