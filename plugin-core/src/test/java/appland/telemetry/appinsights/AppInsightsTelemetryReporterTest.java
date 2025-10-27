package appland.telemetry.appinsights;

import appland.AppMapBaseTest;
import appland.telemetry.TelemetryEvent;
import appland.testRules.MockServerSettingsRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonPathBody.jsonPath;

import java.util.Map;

public class AppInsightsTelemetryReporterTest extends AppMapBaseTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, false);
    @Rule
    public MockServerSettingsRule mockServerSettingsRule = new MockServerSettingsRule();

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    @Test
    public void telemetryEvent() {
        mockServerRule.getClient()
                .when(request().withMethod("POST").withPath("/v2/track"))
                .respond(response().withStatusCode(200));

        var commonProperties = Map.of("common.extname", "appmap-intellij-plugin");
        new AppInsightsTelemetryReporter("http://127.0.0.1:" + mockServerRule.getPort(), commonProperties).track(
                new TelemetryEvent("my-event")
                        .withProperty("property1", "propertyValue1")
                        .withProperty("property2", "propertyValue2")
                        .withMetric("metric1", 42.0)
                        .withMetric("metric2", 7.0));

        mockServerRule.getClient().verify(
                request().withMethod("POST").withPath("/v2/track").withContentType(MediaType.APPLICATION_JSON),
                VerificationTimes.once());

        // common properties
        mockServerRule.getClient()
                .verify(request().withBody(jsonPath("$.data.baseData.properties[?(@.'common.extname' != '')]")));

        // properties
        mockServerRule.getClient()
                .verify(request().withBody(jsonPath("$.data.baseData.properties[?(@.property1 == 'propertyValue1')]")))
                .verify(request().withBody(jsonPath("$.data.baseData.properties[?(@.property2 == 'propertyValue2')]")));

        // metrics
        mockServerRule.getClient()
                .verify(request().withBody(jsonPath("$.data.baseData.metrics[?(@.metric1 == 42.0)]")))
                .verify(request().withBody(jsonPath("$.data.baseData.metrics[?(@.metric2 == 7.0)]")));

        // tags
        mockServerRule.getClient()
                .verify(request().withBody(jsonPath("$.tags[?(@.'ai.device.osVersion' != '')]")))
                .verify(request().withBody(jsonPath("$.tags[?(@.'ai.user.id' != '')]")))
                .verify(request().withBody(jsonPath("$.tags[?(@.'ai.session.id' != '')]")));
    }
}