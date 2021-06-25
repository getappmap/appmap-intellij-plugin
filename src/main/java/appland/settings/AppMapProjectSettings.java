package appland.settings;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppMapProjectSettings {
    @Nullable
    private List<String> recentRemoteRecordingURLs;
    @Nullable
    private String activeRecordingURL;
    @Nullable
    private String recentAppMapStorageLocation;
    @Nullable
    private String cloudServerUrl;
    @Nullable
    private Boolean confirmAppMapUpload;

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

    @Nullable
    public synchronized Boolean getConfirmAppMapUpload() {
        return confirmAppMapUpload;
    }

    public synchronized void setConfirmAppMapUpload(@Nullable Boolean confirmAppMapUpload) {
        this.confirmAppMapUpload = confirmAppMapUpload;
    }

    @Nullable
    public synchronized String getCloudServerUrl() {
        return cloudServerUrl;
    }

    public synchronized void setCloudServerUrl(@Nullable String cloudServerUrl) {
        this.cloudServerUrl = cloudServerUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapProjectSettings that = (AppMapProjectSettings) o;
        return Objects.equals(recentRemoteRecordingURLs, that.recentRemoteRecordingURLs) && Objects.equals(activeRecordingURL, that.activeRecordingURL) && Objects.equals(recentAppMapStorageLocation, that.recentAppMapStorageLocation) && Objects.equals(cloudServerUrl, that.cloudServerUrl) && Objects.equals(confirmAppMapUpload, that.confirmAppMapUpload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recentRemoteRecordingURLs, activeRecordingURL, recentAppMapStorageLocation, cloudServerUrl, confirmAppMapUpload);
    }

    @Override
    public String toString() {
        return "AppMapProjectSettings{" +
                "recentRemoteRecordingURLs=" + recentRemoteRecordingURLs +
                ", activeRecordingURL='" + activeRecordingURL + '\'' +
                ", recentAppMapStorageLocation='" + recentAppMapStorageLocation + '\'' +
                ", cloudServerUrl='" + cloudServerUrl + '\'' +
                ", confirmAppMapUpload=" + confirmAppMapUpload +
                '}';
    }
}
