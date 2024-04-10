package appland.webviews.navie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NavieCodeSelection {
    public @NotNull  final String code;
    public @Nullable final String language;
    public @Nullable final String path;
    public final int lineStart;
    public final int lineEnd;

    public NavieCodeSelection(@NotNull String code,
                              @Nullable String path,
                              int lineStart,
                              int lineEnd,
                              @Nullable String language) {
        this.code = code;
        this.language = language;
        this.path = path;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
    }
}
