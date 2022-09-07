package appland.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a `appmap.yml` file.
 */
@Data
public class AppMapConfigFile {
    @Nullable final String appMapDir;

    public static @Nullable AppMapConfigFile parseConfigFile(@Nullable VirtualFile configFile) {
        if (configFile == null) {
            return null;
        }

        try {
            var nioPath = configFile.toNioPath();
            return parseConfigFile(nioPath);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * A simple implementation to parse the needed config data from an appmap.yml file.
     * This is not using A YAML parse, but attempts line-matching. There's no YAML parser available in the SDK.
     */
    public static @Nullable AppMapConfigFile parseConfigFile(@Nullable Path configFile) {
        if (configFile == null) {
            return null;
        }

        try {
            var lines = Files.readAllLines(configFile);
            var appMapDir = lines.stream()
                    .filter(line -> line.trim().startsWith("appmap_dir:"))
                    .findFirst()
                    .map(line -> StringUtil.trimStart(line.trim(), "appmap_dir:").trim());
            return appMapDir.map(AppMapConfigFile::new).orElse(null);
        } catch (IOException e) {
            Logger.getInstance(AppMapConfigFile.class).debug("Error reading appmap.config file: " + configFile, e);
            return null;
        }
    }
}
