package appland.settings;

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
    private boolean appMapUploadConfirmed;

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

    public synchronized boolean isAppMapUploadConfirmed() {
        return appMapUploadConfirmed;
    }

    public synchronized void setAppMapUploadConfirmed(boolean appMapUploadConfirmed) {
        this.appMapUploadConfirmed = appMapUploadConfirmed;
    }

    @Override
    public String toString() {
        return "AppMapProjectSettings{" +
                "recentRemoteRecordingURLs=" + recentRemoteRecordingURLs +
                ", activeRecordingURL='" + activeRecordingURL + '\'' +
                ", recentAppMapStorageLocation='" + recentAppMapStorageLocation + '\'' +
                ", appMapUploadConfirmed=" + appMapUploadConfirmed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapProjectSettings that = (AppMapProjectSettings) o;
        return appMapUploadConfirmed == that.appMapUploadConfirmed && Objects.equals(recentRemoteRecordingURLs, that.recentRemoteRecordingURLs) && Objects.equals(activeRecordingURL, that.activeRecordingURL) && Objects.equals(recentAppMapStorageLocation, that.recentAppMapStorageLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recentRemoteRecordingURLs, activeRecordingURL, recentAppMapStorageLocation, appMapUploadConfirmed);
    }
}
