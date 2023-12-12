package appland.oauth;

import appland.AppMapBaseTest;
import com.intellij.util.Base64;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class AppMapLocalKeyValidatorTest extends AppMapBaseTest {
    @Test
    public void invalidKey() {
        var validator = new AppMapLocalKeyValidator();
        Assert.assertNotNull(validator.getErrorText("invalid-key"));
        Assert.assertTrue(validator.canClose("invalid-key"));
        Assert.assertTrue(validator.checkInput("invalid-key"));
    }

    @Test
    public void validKey() {
        var validKey = Base64.encode("user@example.com:unique-id".getBytes(StandardCharsets.UTF_8));
        var validator = new AppMapKeyRemoteValidator(getProject());
        Assert.assertFalse(validator.canClose(validKey));
        Assert.assertFalse(validator.checkInput(validKey));
    }
}