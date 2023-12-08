package appland.oauth;

import appland.AppMapBaseTest;
import org.junit.Assert;
import org.junit.Test;

public class AppMapKeyRemoteValidatorTest extends AppMapBaseTest {
    @Test
    public void invalidKey() {
        Assert.assertFalse(new AppMapKeyRemoteValidator(getProject()).checkInput("invalid-key"));
    }
}