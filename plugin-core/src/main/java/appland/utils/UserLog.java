package appland.utils;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class UserLog implements Disposable {
    private final @Nullable FileOutputStream logFile;
    private static final Logger LOG = Logger.getInstance(UserLog.class);

    public UserLog(String name) {
        FileOutputStream logFile = null;
        try {
            File logDir = new File(PathManager.getLogPath());
            if (logDir.exists() || logDir.mkdirs()) {
                logFile = new FileOutputStream(new File(logDir, name), false);
            }
        } catch (FileNotFoundException e) {
            LOG.error("Failed to open log file", e);
        }
        this.logFile = logFile;
    }

    public void log(@NotNull String message) {
        if (logFile == null) {
            return;
        }
        try {
            synchronized (logFile) {
                logFile.write(message.getBytes());
                if (!message.endsWith("\n")) {
                    logFile.write("\n".getBytes());
                }
                logFile.flush();
            }
        } catch (Exception e) {
            LOG.error("Failed to write to log file", e);
        }
    }

    @Override
    public void dispose() {
        try {
            if (logFile != null) {
                logFile.close();
            }
        } catch (Exception e) {
            LOG.error("Failed to close log file", e);
        }
    }
}
