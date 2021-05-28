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
    private String recentAppMapStorageLocation;

    @NotNull
    public String getRecentAppMapStorageLocation() {
        return recentAppMapStorageLocation == null ? "" : recentAppMapStorageLocation;
    }

    public void setRecentAppMapStorageLocation(@NotNull String recentAppMapStorageLocation) {
        this.recentAppMapStorageLocation = recentAppMapStorageLocation;
    }

    @NotNull
    public List<String> getRecentRemoteRecordingURLs() {
        var urls = recentRemoteRecordingURLs;
        return urls == null || urls.isEmpty() ? Collections.emptyList() : recentRemoteRecordingURLs;
    }

    public void setRecentRemoteRecordingURLs(@NotNull List<String> urls) {
        this.recentRemoteRecordingURLs = new CopyOnWriteArrayList<>(urls);
    }

    public void addRecentRemoteRecordingURLs(@NotNull String url) {
        if (url.isBlank()) {
            return;
        }

        if (this.recentRemoteRecordingURLs == null) {
            this.recentRemoteRecordingURLs = new CopyOnWriteArrayList<>();
        }
        this.recentRemoteRecordingURLs.remove(url);
        this.recentRemoteRecordingURLs.add(0, url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapProjectSettings that = (AppMapProjectSettings) o;
        return Objects.equals(recentRemoteRecordingURLs, that.recentRemoteRecordingURLs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recentRemoteRecordingURLs);
    }

    @Override
    public String toString() {
        return "AppMapProjectSettings{" +
                "lastRemoteRecordingURLs=" + recentRemoteRecordingURLs +
                '}';
    }
}
