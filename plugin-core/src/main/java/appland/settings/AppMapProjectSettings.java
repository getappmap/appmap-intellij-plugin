package appland.settings;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EqualsAndHashCode
@ToString
@Getter(onMethod_ = {@Synchronized})
@Setter(onMethod_ = {@Synchronized})
public class AppMapProjectSettings {
    private @Nullable List<String> recentRemoteRecordingURLs;
    private @Nullable String activeRecordingURL;
    private @Nullable String cloudServerUrl;
    private boolean confirmAppMapUpload = true;
    private boolean openedAppMapEditor = false;
    private boolean createdOpenAPI = false;

    @SuppressWarnings("unused")
    public AppMapProjectSettings() {
    }

    /**
     * Copy constructor.
     *
     * @param settings The settings to copy.
     */
    public AppMapProjectSettings(@NotNull AppMapProjectSettings settings) {
        this.recentRemoteRecordingURLs = settings.recentRemoteRecordingURLs == null ? null : Lists.newLinkedList(settings.recentRemoteRecordingURLs);
        this.activeRecordingURL = settings.activeRecordingURL;
        this.cloudServerUrl = settings.cloudServerUrl;
        this.confirmAppMapUpload = settings.confirmAppMapUpload;
        this.openedAppMapEditor = settings.openedAppMapEditor;
        this.createdOpenAPI = settings.createdOpenAPI;
    }

    @NotNull
    public synchronized List<String> getRecentRemoteRecordingURLs() {
        var urls = recentRemoteRecordingURLs;
        return urls == null || urls.isEmpty() ? Collections.emptyList() : recentRemoteRecordingURLs;
    }

    public synchronized void setRecentRemoteRecordingURLs(@NotNull List<String> urls) {
        this.recentRemoteRecordingURLs = new CopyOnWriteArrayList<>(urls);
    }

    public synchronized void addRecentRemoteRecordingURLs(@NotNull String url) {
        if (url.isBlank()) {
            return;
        }

        if (this.recentRemoteRecordingURLs == null) {
            this.recentRemoteRecordingURLs = new CopyOnWriteArrayList<>();
        }
        this.recentRemoteRecordingURLs.remove(url);
        this.recentRemoteRecordingURLs.add(0, url);
    }

    public void setCreatedOpenAPI(boolean createdOpenAPI) {
        boolean changed;
        synchronized (this) {
            changed = this.createdOpenAPI != createdOpenAPI;
            this.createdOpenAPI = createdOpenAPI;
        }

        if (changed) {
            settingsPublisher().createOpenApiChanged();
        }
    }

    @NotNull
    private AppMapSettingsListener settingsPublisher() {
        return ApplicationManager.getApplication().getMessageBus().syncPublisher(AppMapSettingsListener.TOPIC);
    }
}
