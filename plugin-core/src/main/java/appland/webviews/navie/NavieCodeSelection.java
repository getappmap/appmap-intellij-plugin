package appland.webviews.navie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NavieCodeSelection {
    public @NotNull  final String code;
    public @Nullable final String language;
    public @Nullable final String path;
    public @Nullable final int lineStart;
    public @Nullable final int lineEnd;

    public NavieCodeSelection(@NotNull String code,
                              @Nullable String path,
                              @Nullable int lineStart,
                              @Nullable int lineEnd,
                              @Nullable String language) {
        this.code = code;
        this.language = language;
        this.path = path;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
    }
}
