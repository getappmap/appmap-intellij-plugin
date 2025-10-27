package appland.telemetry.splunk;

import appland.AppMapBaseTest;
import appland.telemetry.TelemetryEvent;
import appland.testRules.MockServerSettingsRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.Map;

public class SplunkTelemetryReporterTest extends AppMapBaseTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, false);
    @Rule
    public MockServerSettingsRule mockServerSettingsRule = new MockServerSettingsRule();

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void alwaysEnabled() {
        assertTrue(new SplunkTelemetryReporter("https://my-splunk.example.com", "some-token", Map.of()).isAlwaysEnabled());
    }

    @Test
    public void telemetryEvent() {
        mockServerRule.getClient()
                .when(request().withMethod("POST").withPath("/services/collector/event/1.0"))
                .respond(response().withStatusCode(200).withBody(new JsonBody("{okay: true}")));

        var commonProperties = Map.of("common.a", "b", "common.c", "d");
        new SplunkTelemetryReporter("http://127.0.0.1:" + mockServerRule.getPort(), "my-splunk-token", commonProperties).track(
                new TelemetryEvent("my-event")
                        .withProperty("property1", "propertyValue1")
                        .withProperty("property2", "propertyValue2")
                        .withMetric("metric1", 42.0)
                        .withMetric("metric2", 7.0));

        var expectedBody = """
                {
                  "event" : {
                    "name" : "my-event",
                    "properties" : {
                      "common.a" : "b",
                      "common.c" : "d",
                      "property2" : "propertyValue2",
                      "property1" : "propertyValue1"
                    },
                    "measurements" : {
                      "metric1" : 42.0,
                      "metric2" : 7.0
                    }
                  }
                }
                """;

        mockServerRule.getClient().verify(
                request().withMethod("POST")
                        .withPath("/services/collector/event/1.0")
                        .withHeader("Authorization", "Splunk my-splunk-token")
                        .withBody(new JsonBody(expectedBody)),
                VerificationTimes.once()
        );
    }
}
