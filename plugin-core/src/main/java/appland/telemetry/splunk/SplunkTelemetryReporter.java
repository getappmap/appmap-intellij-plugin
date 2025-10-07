package appland.telemetry.splunk;

import appland.AppMapPlugin;
import appland.telemetry.TelemetryEvent;
import appland.telemetry.TelemetryReporter;
import appland.utils.GsonUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class SplunkTelemetryReporter implements TelemetryReporter {
    private static final @NotNull String EXTENSION_IDId = "appland.appmap";
    public static final @NotNull String BASE_PATH = "services/collector/event/1.0";

    private final @NotNull SplunkSettings settings;

    public SplunkTelemetryReporter(@NotNull SplunkSettings settings) {
        this.settings = settings;
    }

    @Override
    public void track(@NotNull TelemetryEvent event) {
        if (StringUtil.isEmpty(settings.getUrl())) {
            return;
        }

        var splunkEvent = new SplunkTelemetryEvent(
                EXTENSION_IDId,
                AppMapPlugin.getDescriptor().getVersion(),
                event.getName(),
                event.getProperties(),
                event.getMetrics()
        );

        try {
            var url = Urls.parse(settings.getUrl(), false);
            if (url == null) {
                Logger.getInstance(getClass()).warn("Invalid Splunk URL: " + settings.getUrl());
                return;
            }

            if (url.getPath().isEmpty() || "/".equals(url.getPath())) {
                url = url.resolve(BASE_PATH);
            }

            // the JSON contains the event data nested at "event", same as in the VSCode plugin
            var json = GsonUtils.GSON.toJson(Map.of("event", splunkEvent));

            HttpRequests.post(url.toExternalForm(), "application/json")
                    .throwStatusCodeException(true)
                    .tuner(t -> {
                        t.setRequestProperty("Accept", "application/json");
                        t.setRequestProperty("Authorization", "Splunk " + settings.getToken());
                    })
                    .connect(req -> {
                        req.write(json);
                        return req;
                    });
        } catch (HttpRequests.HttpStatusException e) {
            Logger.getInstance(getClass()).warn("Error sending telemetry to Splunk: " + e.getStatusCode(), e);
        } catch (IOException e) {
            Logger.getInstance(getClass()).warn("Error sending telemetry to Splunk", e);
        }
    }
}
