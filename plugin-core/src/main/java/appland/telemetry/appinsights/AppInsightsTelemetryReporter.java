package appland.telemetry.appinsights;

import appland.telemetry.*;
import appland.utils.GsonUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Urls;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


/*
 * Based on:
 * https://github.com/Azure/azure-sdk-for-java/blob/4a53adf6274ced5af8243983042b5e32bac85bd7/sdk/monitor/azure-monitor-opentelemetry-exporter/src/main/java/com/azure/monitor/opentelemetry/exporter/AzureMonitorExporterBuilder.java
 */
public class AppInsightsTelemetryReporter implements TelemetryReporter {
    private static final @NotNull String EVENT_PREFIX = "appland.appmap/";
    private static final @NotNull String INSTRUMENTATION_KEY = "50c1a5c2-49b4-4913-b7cc-86a78d40745f";
    private static final @NotNull String DEFAULT_INGESTION_ENDPOINT = "https://centralus-0.in.applicationinsights.azure.com";

    private final @NotNull String ingestionEndpoint;
    private final @NotNull Map<String, String> commonProperties;

    /**
     * Constructs a new AppInsightsTelemetryReporter.
     *
     * @param commonProperties The common telemetry properties, which are expected to have the "common." prefix.
     */
    public AppInsightsTelemetryReporter(@NotNull Map<String, String> commonProperties) {
        this(DEFAULT_INGESTION_ENDPOINT, commonProperties);
    }

    AppInsightsTelemetryReporter(@NotNull String ingestionEndpoint, @NotNull Map<String, String> commonProperties) {
        this.ingestionEndpoint = ingestionEndpoint;
        this.commonProperties = commonProperties;
    }

    @Override
    @RequiresBackgroundThread
    public void track(@NotNull TelemetryEvent event) {
        var userId = Identity.getOrCreateMachineId();
        var osVersion = System.getProperty("os.version");

        var data = new BaseData(EVENT_PREFIX + event.getName(), this.commonProperties, event);

        var appInsightsEvent = new AppInsightsTelemetryEvent(INSTRUMENTATION_KEY, data)
                .tag(Tag.OsVersion, osVersion)
                .tag(Tag.UserId, userId)
                .tag(Tag.SessionId, Session.getId());

        try {
            var url = Urls.newFromEncoded(ingestionEndpoint).resolve("v2/track").toExternalForm();
            var json = GsonUtils.GSON.toJson(appInsightsEvent);

            HttpRequests.post(url, "application/json")
                    .tuner(t -> t.setRequestProperty("Accept", "application/json"))
                    .connect(req -> {
                        req.write(json);
                        return req;
                    });
        } catch (Exception e) {
            Logger.getInstance(getClass()).warn(e);
        }
    }
}
