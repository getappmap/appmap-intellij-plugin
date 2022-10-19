package appland.settings;

import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EqualsAndHashCode
@ToString
public class AppMapProjectSettings {
    @Nullable
    private List<String> recentRemoteRecordingURLs;
    @Nullable
    private String activeRecordingURL;
    @Nullable
    private String recentAppMapStorageLocation;
    @Nullable
    private String cloudServerUrl;
    private boolean confirmAppMapUpload = true;
    private boolean openedAppMapEditor = false;

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
        this.recentAppMapStorageLocation = settings.recentAppMapStorageLocation;
        this.cloudServerUrl = settings.cloudServerUrl;
        this.confirmAppMapUpload = settings.confirmAppMapUpload;
        this.openedAppMapEditor = settings.openedAppMapEditor;
    }

    @NotNull
    public synchronized String getRecentAppMapStorageLocation() {
        return recentAppMapStorageLocation == null ? "" : recentAppMapStorageLocation;
    }

    public synchronized void setRecentAppMapStorageLocation(@NotNull String recentAppMapStorageLocation) {
        this.recentAppMapStorageLocation = recentAppMapStorageLocation;
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

    @Nullable
    public synchronized String getActiveRecordingURL() {
        return activeRecordingURL;
    }

    public synchronized void setActiveRecordingURL(@Nullable String activeRecordingURL) {
        this.activeRecordingURL = activeRecordingURL;
    }

    public synchronized boolean getConfirmAppMapUpload() {
        return confirmAppMapUpload;
    }

    public synchronized void setConfirmAppMapUpload(boolean confirmAppMapUpload) {
        this.confirmAppMapUpload = confirmAppMapUpload;
    }

    @Nullable
    public synchronized String getCloudServerUrl() {
        return cloudServerUrl;
    }

    public synchronized void setCloudServerUrl(@Nullable String cloudServerUrl) {
        this.cloudServerUrl = cloudServerUrl;
    }

    public synchronized boolean isOpenedAppMapEditor() {
        return openedAppMapEditor;
    }

    public synchronized void setOpenedAppMapEditor(boolean openedAppMapEditor) {
        this.openedAppMapEditor = openedAppMapEditor;
    }
}
