package appland.files;

import com.intellij.openapi.vfs.VirtualFileManagerListener;

import java.util.Set;

public class VirtualFileManagerLister implements VirtualFileManagerListener {
    @Override
    public void afterRefreshFinish(boolean asynchronous) {
        AppMapFileChangeListener.sendNotification(Set.of(AppMapFileEventType.Refresh), true);
    }
}
