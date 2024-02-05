package appland.webviews;

import appland.AppMapBaseTest;
import org.junit.Test;

import java.util.Map;

import static appland.webviews.OpenExternalLinksHandler.isExternalUrl;

public class OpenExternalLinksHandlerTest extends AppMapBaseTest {
    @Test
    public void isExternalUrlTest() {
        var testData = Map.of(
                // external
                "http://example.com", true,
                "https://example.com", true,
                // not external
                "http://localhost", false,
                "https://127.0.0.1", false
        );

        testData.forEach((url, isExternal) -> {
            assertEquals(isExternal.booleanValue(), isExternalUrl(url));
            assertEquals(isExternal.booleanValue(), isExternalUrl(url + ":1234"));
            assertEquals(isExternal.booleanValue(), isExternalUrl(url + ":1234/index.html"));
            assertEquals(isExternal.booleanValue(), isExternalUrl(url + ":1234/index.html?param=value"));
            assertEquals(isExternal.booleanValue(), isExternalUrl(url + ":1234/index.html?param=value#anchor"));
        });

        assertFalse(isExternalUrl("file:///home/user/test.html"));
    }
}