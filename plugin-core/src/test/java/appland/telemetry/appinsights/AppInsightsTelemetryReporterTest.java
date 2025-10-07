package appland.telemetry.appinsights;

import appland.AppMapBaseTest;
import appland.telemetry.TelemetryEvent;
import appland.testRules.MockServerSettingsRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.MediaType;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonPathBody.jsonPath;

public class AppInsightsTelemetryReporterTest extends AppMapBaseTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, false);
    @Rule
    public MockServerSettingsRule mockServerSettingsRule = new MockServerSettingsRule();

    @Test
    public void telemetryEvent() {
        mockServerRule.getClient()
                .when(request().withMethod("POST").withPath("/v2/track"))
                .respond(response().withStatusCode(200));

        new AppInsightsTelemetryReporter("http://127.0.0.1:" + mockServerRule.getPort()).track(
                new TelemetryEvent("my-event")
                        .withProperty("property1", "propertyValue1")
                        .withProperty("property2", "propertyValue2")
                        .withMetric("metric1", 42.0)
                        .withMetric("metric2", 7.0)
        );

        mockServerRule.getClient().verify(request()
                .withMethod("POST")
                .withPath("/v2/track")
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(jsonPath("$.data.baseData.properties[?(@.property1 == 'propertyValue1')]")));
        mockServerRule.getClient().verify(request()
                .withBody(jsonPath("$.data.baseData.properties[?(@.property2 == 'propertyValue2')]"))
        );
        mockServerRule.getClient().verify(request()
                .withBody(jsonPath("$.data.baseData.metrics[?(@.metric1 == 42.0)]"))
        );
        mockServerRule.getClient().verify(request()
                .withBody(jsonPath("$.data.baseData.metrics[?(@.metric2 == 7.0)]"))
        );
    }
}