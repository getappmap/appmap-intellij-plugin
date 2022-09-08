package appland.problemsView.listener;

import com.intellij.util.messages.Topic;

/**
 * Listener to be notified about changes for appmap-findings.json files.
 */
public interface ScannerFindingsListener {
    Topic<ScannerFindingsListener> TOPIC = Topic.create("appmap.findingsListener", ScannerFindingsListener.class);

    void afterFindingsChanged();
}
