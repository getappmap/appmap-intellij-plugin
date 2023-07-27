package appland.installGuide;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for Install Guide navigation events.
 */
public interface InstallGuideListener {
    @Topic.ProjectLevel
    Topic<InstallGuideListener> TOPIC = Topic.create("appmap.installGuide", InstallGuideListener.class);

    /**
     * @param page The installation guide page, which was executed/opened
     */
    void afterInstallGuidePageOpened(@NotNull InstallGuideViewPage page);
}
