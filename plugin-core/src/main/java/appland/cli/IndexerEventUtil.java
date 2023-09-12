package appland.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the "Indexed" messages documented at
 * <a href="https://github.com/getappmap/appmap-js/blob/main/packages/cli/doc/index-verbose.md">index-verbose.md</a>.
 */
class IndexerEventUtil {
    private IndexerEventUtil() {
    }

    static boolean isIndexedEvent(@NotNull String message) {
        return message.startsWith("Indexed ");
    }

    static @Nullable String extractIndexedFilePath(@NotNull String message) {
        if (!isIndexedEvent(message)) {
            return null;
        }

        var value = message.substring("Indexed ".length()).trim();
        return value.length() >= 3 && value.startsWith("\"") && value.endsWith("\"")
                ? unescapeFilePath(value.substring(1, value.length() - 1))
                : value;
    }

    private static @NotNull String unescapeFilePath(@NotNull String value) {
        return value
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\0", "\0")
                .replace("\\\"", "\"");
    }
}
