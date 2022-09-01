package appland.problemsView;

import com.intellij.util.messages.Topic;

/**
 * Similar to the IDE's {@link com.intellij.analysis.problemsView.ProblemsListener},
 * but for problems without a known source file.
 */
public interface UnknownFileProblemListener {
    Topic<UnknownFileProblemListener> TOPIC = Topic.create("AppMap Unknown File Problem", UnknownFileProblemListener.class);

    void afterUnknownFileProblemsChange();
}
