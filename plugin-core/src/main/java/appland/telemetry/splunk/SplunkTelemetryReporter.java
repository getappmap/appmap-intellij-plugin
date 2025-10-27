package appland.telemetry.splunk;

import appland.telemetry.TelemetryEvent;
import appland.telemetry.TelemetryProperties;
import appland.telemetry.TelemetryReporter;
import appland.utils.GsonUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SplunkTelemetryReporter implements TelemetryReporter {
    public static final @NotNull String BASE_PATH = "services/collector/event/1.0";

    private final @NotNull String url;
    private final @NotNull String token;
    private final @NotNull Map<String, String> commonProperties;

    /**
     * Constructs a new SplunkTelemetryReporter.
     *
     * @param url              The URL of the Splunk collector.
     * @param token            The authentication token for the Splunk collector.
     * @param commonProperties The common telemetry properties, which are expected to have the "common." prefix.
     */
    public SplunkTelemetryReporter(@NotNull String url, @NotNull String token, @NotNull Map<String, String> commonProperties) {
        this.url = url;
        this.token = token;
        this.commonProperties = commonProperties;
    }

    @Override
    public boolean isAlwaysEnabled() {
        // Splunk telemetry must always be enabled, even if the user has disabled it in the settings.
        return true;
    }

    @Override
    public void track(@NotNull TelemetryEvent event) {
        if (StringUtil.isEmpty(url)) {
            return;
        }

        var properties = new HashMap<>(commonProperties);
        properties.putAll(event.getProperties());

        var splunkEvent = new SplunkTelemetryEvent(
                event.getName(),
                properties,
                event.getMetrics()
        );

        try {
            var url = Urls.parse(this.url, false);
            if (url == null) {
                Logger.getInstance(getClass()).warn("Invalid Splunk URL: " + this.url);
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
                        t.setRequestProperty("Authorization", "Splunk " + token);
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
