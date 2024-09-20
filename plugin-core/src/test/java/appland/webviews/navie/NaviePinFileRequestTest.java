package appland.webviews.navie;

import appland.AppMapBaseTest;
import appland.utils.GsonUtils;
import org.junit.Test;

public class NaviePinFileRequestTest extends AppMapBaseTest {
    @Test
    public void jsonSerialization() {
        // language=JSON
        var expected = "{\"name\":\"request-name\",\"uri\":\"file:///dir/subdir/file.txt\",\"content\":\"my content\"}";
        var request = new NaviePinFileRequest("request-name", "file:///dir/subdir/file.txt", "my content");
        assertEquals(expected, GsonUtils.GSON.toJson(request));
    }
}