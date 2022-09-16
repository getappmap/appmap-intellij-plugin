package appland.problemsView.listener;

import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsListener;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Listener to be notified about changes for appmap-findings.json files.
 */
public interface ScannerFindingsListener extends ProblemsListener {
    @Topic.ProjectLevel
    Topic<ScannerFindingsListener> TOPIC = Topic.create("AppMap findings file", ScannerFindingsListener.class);

    @Override
    default void problemAppeared(@NotNull Problem problem) {
    }

    @Override
    default void problemDisappeared(@NotNull Problem problem) {
    }

    @Override
    default void problemUpdated(@NotNull Problem problem) {
    }

    default void afterUnknownFileProblemsChange() {
    }

    /**
     * Sent after changes to findings. It's sent after {@link #problemAppeared(Problem)},
     * {@link #problemDisappeared(Problem)}, and {@link #problemUpdated(Problem)}.
     * <p>
     * It's not sent after a {@link #afterFindingsReloaded()}.
     */
    default void afterFindingsChanged() {
    }

    /**
     * Sent after reload of all findings by the findings manager.
     * {@link #afterFindingsChanged()} is not sent after reload.
     */
    default void afterFindingsReloaded() {
    }
}
