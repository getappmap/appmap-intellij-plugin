package appland.validator;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class UrlInputValidatorTest {
    @Test
    public void validURLs() {
        assertValid("https://example.com");
        assertValid("https://example.com:8080");
        assertValid("https://example.com:8080/path");
    }

    @Test
    public void invalidURLs() {
        assertInvalid("");
        assertInvalid("file://test.html");
        assertInvalid("https://");
    }

    private void assertValid(@NotNull String url) {
        assertTrue(new UrlInputValidator().checkInput(url));
    }

    private void assertInvalid(@NotNull String url) {
        assertFalse(new UrlInputValidator().checkInput(url));
    }
}