package appland.files;

import com.intellij.openapi.vfs.VirtualFileManagerListener;

public class VirtualFileManagerLister implements VirtualFileManagerListener {
    @Override
    public void afterRefreshFinish(boolean asynchronous) {
        AppMapFileChangeListener.sendNotification();
    }
}
