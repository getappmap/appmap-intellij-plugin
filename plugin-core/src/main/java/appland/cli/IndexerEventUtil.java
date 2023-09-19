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
        var result = new StringBuilder(value.length());
        var escaped = false;
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);

            if (!escaped) {
                if (c == '\\') {
                    escaped = true;
                } else {
                    result.append(c);
                }
            } else {
                switch (c) {
                    case '\\':
                        result.append('\\');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case '0':
                        result.append('\0');
                        break;
                    case '"':
                        result.append('"');
                        break;
                }

                escaped = false;
            }
        }
        return result.toString();
    }
}
