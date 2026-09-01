package appland.settings;

import appland.AppMapBaseTest;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentSettingsService;
import com.intellij.configurationStore.XmlSerializer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.JDOMUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppMapApplicationSettingsTest extends AppMapBaseTest {
    @After
    public void resetDeploymentSettings() {
        AppMapDeploymentSettingsService.reset();
    }

    // --- isSignedInOrEntitled: a real session and a managed entitlement are separate axes ---

    @Test
    public void signedInOrEntitled_withASessionOnly() {
        var settings = new AppMapApplicationSettings();
        settings.setApiKey("api-key");

        assertTrue(settings.isSignedInOrEntitled());
    }

    @Test
    public void signedInOrEntitled_withAnEntitlementOnly() {
        entitle("acme-corp");

        var settings = new AppMapApplicationSettings();
        assertNull("no session", settings.getApiKey());
        assertTrue("An entitled deployment must count as signed in for UI and service purposes",
                settings.isSignedInOrEntitled());
    }

    @Test
    public void signedInOrEntitled_withNeither() {
        assertFalse(new AppMapApplicationSettings().isSignedInOrEntitled());
    }

    @Test
    public void signedInOrEntitled_withBoth() {
        entitle("acme-corp");

        var settings = new AppMapApplicationSettings();
        settings.setApiKey("api-key");
        assertTrue(settings.isSignedInOrEntitled());
        assertTrue("Entitlement must not disturb the real credential", settings.hasAppMapKey());
    }

    private static void entitle(@NotNull String customerId) {
        AppMapDeploymentSettingsService.getInstance().setEnterpriseDeploymentSettings(
                AppMapDeploymentSettings.builder().customerId(customerId).build());
    }

    @Test
    public void xmlSerialization() {
        var settings = createSettings();

        var serialized = XmlSerializer.serialize(settings);
        Assert.assertNotNull(serialized);

        var deserialized = XmlSerializer.deserialize(serialized, AppMapApplicationSettings.class);
        Assert.assertEquals(settings, deserialized);

        var expectedXML = """
                <AppMapApplicationSettings>
                  <option name="apiKey" value="my-appmap-api-key" />
                  <option name="cliEnvironment">
                    <map>
                      <entry key="name1" value="value1" />
                      <entry key="name2" value="value2" />
                    </map>
                  </option>
                  <option name="installInstructionsViewed" value="true" />
                  <option name="maxPinnedFileSizeKB" value="40" />
                </AppMapApplicationSettings>""";
        Assert.assertEquals(expectedXML, JDOMUtil.write(serialized));
    }

    @Test
    public void copy() {
        var settings = createSettings();
        var copiedSettings = new AppMapApplicationSettings(settings);
        Assert.assertEquals("Copy constructor must copy all settings", settings, copiedSettings);
    }

    @Test
    public void modelConfigListenerForNewKey() throws Exception {
        var condition = new CountDownLatch(1);
        var listener = new AppMapSettingsListener() {
            @Override
            public void modelConfigChange() {
                ApplicationManager.getApplication().assertIsNonDispatchThread();
                condition.countDown();
            }
        };

        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, listener);

        AppMapApplicationSettingsService.getInstance().setModelConfigItemNotifying("first_key", "first_value");
        assertTrue(condition.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void modelConfigListenerForUpdatedValue() throws Exception {
        AppMapApplicationSettingsService.getInstance().setModelConfigItem("first_key", "first_value");

        var condition = new CountDownLatch(1);
        var listener = new AppMapSettingsListener() {
            @Override
            public void modelConfigChange() {
                ApplicationManager.getApplication().assertIsNonDispatchThread();
                condition.countDown();
            }
        };

        ApplicationManager.getApplication().getMessageBus()
                .connect(getTestRootDisposable())
                .subscribe(AppMapSettingsListener.TOPIC, listener);

        AppMapApplicationSettingsService.getInstance().setModelConfigItemNotifying("first_key", "updated_value");
        assertTrue(condition.await(15, TimeUnit.SECONDS));
    }

    @NotNull
    private static AppMapApplicationSettings createSettings() {
        var settings = new AppMapApplicationSettings();
        settings.setCliEnvironmentNotifying(Map.of("name1", "value1", "name2", "value2"));
        settings.setApiKey("my-appmap-api-key");
        settings.setInstallInstructionsViewed(true);
        settings.setFirstStart(true);
        settings.setCliPassParentEnv(true);
        settings.setMaxPinnedFileSizeKB(40);
        return settings;
    }
}