package appland.telemetry;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;

import appland.AppMapPlugin;
import appland.telemetry.data.Tag;
import appland.telemetry.data.TelemetryEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;


/*
 * Based on:
 * https://github.com/Azure/azure-sdk-for-java/blob/4a53adf6274ced5af8243983042b5e32bac85bd7/sdk/
 * monitor/azure-monitor-opentelemetry-exporter/src/main/java/com/azure/monitor/opentelemetry/
 * exporter/AzureMonitorExporterBuilder.java
 */
class AppInsightsClient {
    private final @NotNull String ingestionEndpoint;
    private final @NotNull Gson gson = new GsonBuilder().create();

    AppInsightsClient(@NotNull String ingestionEndpoint) {
        this.ingestionEndpoint = ingestionEndpoint;
    }

    public void track(@NotNull TelemetryEvent event) {
        var userId = Identity.getMachineId();
        var osVersion = System.getProperty("os.version");

        event.property("common.os", System.getProperty("os.name"))
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
            var url = Urls.newFromEncoded(ingestionEndpoint).resolve("/v2/track").toExternalForm();
            var json = gson.toJson(event);

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
