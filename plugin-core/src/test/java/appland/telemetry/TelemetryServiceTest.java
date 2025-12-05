package appland.telemetry;

import appland.AppMapBaseTest;
import appland.AppMapDeploymentTestUtils;
import appland.deployment.AppMapDeploymentSettings;
import appland.deployment.AppMapDeploymentTelemetrySettings;
import appland.notifications.AppMapNotifications;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationsManager;
import com.intellij.testFramework.PlatformTestUtil;
import org.junit.Test;

import java.util.Arrays;

public class TelemetryServiceTest extends AppMapBaseTest {
    @Test
    public void notificationShownByDefault() {
        var service = new TelemetryService();
        service.notifyTelemetryUsage(getProject());

        assertTelemetryNotificationShown(true);
    }

    @Test
    public void notificationSkippedForSplunk() throws Exception {
        var splunk = new AppMapDeploymentTelemetrySettings("splunk", "https://splunk.example.com", "token", null);
        var settings = new AppMapDeploymentSettings(splunk);

        AppMapDeploymentTestUtils.withSiteConfigFile(settings, () -> {
            var service = new TelemetryService();
            service.notifyTelemetryUsage(getProject());

            assertTelemetryNotificationShown(false);
        });
    }

    private void assertTelemetryNotificationShown(boolean expected) {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        var notifications = NotificationsManager.getNotificationsManager()
                .getNotificationsOfType(Notification.class, getProject());

        var found = Arrays.stream(notifications)
                .anyMatch(n -> AppMapNotifications.TELEMETRY_ID.equals(n.getGroupId()));

        if (expected) {
            assertTrue("Telemetry notification should be shown", found);
        } else {
            assertFalse("Telemetry notification should not be shown", found);
        }
    }
}
