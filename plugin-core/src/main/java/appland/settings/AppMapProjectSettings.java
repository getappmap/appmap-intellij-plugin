package appland.settings;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@EqualsAndHashCode
@ToString
@Getter(onMethod_ = {@Synchronized})
@Setter(onMethod_ = {@Synchronized})
public class AppMapProjectSettings {
    private @Nullable List<String> recentRemoteRecordingURLs;
    private @Nullable String activeRecordingURL;
    private boolean openedAppMapEditor = false;
    private boolean createdOpenAPI = false;
    private boolean investigatedFindings = false;

    /**
     * Maps filter name to the actual filter.
     */
    private @NotNull Map<String, AppMapWebViewFilter> appMapFilters = new HashMap<>();

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
        this.openedAppMapEditor = settings.openedAppMapEditor;
        this.createdOpenAPI = settings.createdOpenAPI;
        this.investigatedFindings = settings.investigatedFindings;
        this.appMapFilters = new HashMap<>(settings.appMapFilters);
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

    public synchronized @NotNull Map<String, AppMapWebViewFilter> getAppMapFilters() {
        return appMapFilters;
    }

    /**
     * Add the given filter to the stored AppMap filters.
     *
     * @param filter Filter to add.
     */
    public synchronized void saveAppMapWebViewFilter(@NotNull AppMapWebViewFilter filter) {
        // if the new filter is the default filter, remove the default flag from all existing filers
        if (filter.isDefault()) {
            for (var value : appMapFilters.values()) {
                value.setDefault(false);
            }
        }

        appMapFilters.put(filter.filterName, filter);
        settingsPublisher().appMapWebViewFiltersChanged();
    }

    /**
     * Marks the given filter as default filter.
     * If the filter already exists, then the existing value is marked as default filter.
     * If the filter does not yet exist, then the supplied filter is added as new filter.
     *
     * @param filter Filter to mark as default filter.
     */
    public synchronized void saveDefaultFilter(@NotNull AppMapWebViewFilter filter) {
        var storedFilter = appMapFilters.get(filter.filterName);
        if (storedFilter == null) {
            // save as new filter and properly update default flag of existing filters
            saveAppMapWebViewFilter(filter);
        } else {
            // mark all others as "not default" and then mark the requested filter as default
            for (var value : appMapFilters.values()) {
                value.setDefault(false);
            }
            storedFilter.setDefault(true);
            settingsPublisher().appMapWebViewFiltersChanged();
        }
    }

    /**
     * Remove the filter from the stored list of filters. The filter is identified by name.
     *
     * @param filter Filter to remove
     */
    public synchronized void removeAppMapWebViewFilter(@NotNull AppMapWebViewFilter filter) {
        if (appMapFilters.remove(filter.filterName) != null) {
            settingsPublisher().appMapWebViewFiltersChanged();
        }
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

    public void setOpenedAppMapEditor(boolean openedAppMapEditor) {
        boolean changed;
        synchronized (this) {
            changed = this.openedAppMapEditor != openedAppMapEditor;
            this.openedAppMapEditor = openedAppMapEditor;
        }

        if (changed) {
            settingsPublisher().openedAppMapChanged();
        }
    }

    public void setInvestigatedFindings(boolean investigatedFindings) {
        boolean changed;
        synchronized (this) {
            changed = this.investigatedFindings != investigatedFindings;
            this.investigatedFindings = investigatedFindings;
        }

        if (changed) {
            settingsPublisher().investigatedFindingsChanged();
        }
    }

    @NotNull
    private AppMapSettingsListener settingsPublisher() {
        return ApplicationManager.getApplication().getMessageBus().syncPublisher(AppMapSettingsListener.TOPIC);
    }
}
