package appland.problemsView.listener;

import com.intellij.util.messages.Topic;

/**
 * Listener to be notified about changes for appmap-findings.json files.
 */
public interface ScannerFindingsListener {
    @Topic.ProjectLevel
    Topic<ScannerFindingsListener> TOPIC = Topic.create("AppMap findings file", ScannerFindingsListener.class);

    void afterFindingsChanged();
}
