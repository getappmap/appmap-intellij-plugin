package appland.cli;

import com.intellij.util.messages.Topic;

public interface AppLandCommandLineListener {
    Topic<AppLandCommandLineListener> TOPIC = Topic.create("appland.cli", AppLandCommandLineListener.class);

    void afterRefreshForProjects();
}
