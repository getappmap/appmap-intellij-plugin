package appland.cli;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Notifies when a download of a CLI binary was finished.
 */
public interface AppLandDownloadListener {
    Topic<AppLandDownloadListener> TOPIC = Topic.create("appland.cliDownload", AppLandDownloadListener.class);

    void downloadFinished(@NotNull CliTool type, boolean success);
}
