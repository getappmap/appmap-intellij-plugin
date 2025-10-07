package appland.telemetry.appinsights;

import appland.AppMapPlugin;
import appland.telemetry.Identity;
import appland.telemetry.Session;
import appland.telemetry.TelemetryEvent;
import appland.telemetry.TelemetryReporter;
import appland.utils.GsonUtils;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Urls;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;


/*
 * Based on:
 * https://github.com/Azure/azure-sdk-for-java/blob/4a53adf6274ced5af8243983042b5e32bac85bd7/sdk/monitor/azure-monitor-opentelemetry-exporter/src/main/java/com/azure/monitor/opentelemetry/exporter/AzureMonitorExporterBuilder.java
 */
public class AppInsightsTelemetryReporter implements TelemetryReporter {
    private static final @NotNull String EVENT_PREFIX = "appland.appmap/";
    private static final @NotNull String INSTRUMENTATION_KEY = "50c1a5c2-49b4-4913-b7cc-86a78d40745f";
    private static final @NotNull String DEFAULT_INGESTION_ENDPOINT = "https://centralus-0.in.applicationinsights.azure.com";

    private final @NotNull String ingestionEndpoint;

    public AppInsightsTelemetryReporter() {
        this(DEFAULT_INGESTION_ENDPOINT);
    }

    AppInsightsTelemetryReporter(@NotNull String ingestionEndpoint) {
        this.ingestionEndpoint = ingestionEndpoint;
    }

    @Override
    @RequiresBackgroundThread
    public void track(@NotNull TelemetryEvent event) {
        var userId = Identity.getOrCreateMachineId();
        var osVersion = System.getProperty("os.version");

        var appInsightsEvent = new AppInsightsTelemetryEvent(INSTRUMENTATION_KEY, new BaseData(EVENT_PREFIX + event.getName(), event))
                .property("common.os", System.getProperty("os.name"))
                .property("common.platformversion", System.getProperty("os.version"))
                .property("common.jvmversion", System.getProperty("java.version"))
                .property("common.extversion", AppMapPlugin.getDescriptor().getVersion())
                .property("common.intellijversion", ApplicationInfo.getInstance().getFullVersion())
                .property("common.product", ApplicationInfo.getInstance().getBuild().getProductCode())
                .property("common.source", "JetBrains")
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
