package appland.utils;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataContextWrapper;
import com.intellij.openapi.actionSystem.DataProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides factory methods to create customized {@link DataContext} implementations, which are compatible with all our supported IDEs.
 * <p>
 * 2024.2 is rejecting custom implementations and is only accepting DataContext implementations provided by the SDK itself.
 */
@SuppressWarnings("removal")
public final class DataContexts {
    private DataContexts() {
    }

    public static @NotNull DataContext createCustomContext(@NotNull DataProvider provider) {
        return createCustomContext(null, provider);
    }

    public static @NotNull DataContext createCustomContext(@Nullable DataContext parentContext, @NotNull DataProvider provider) {
        return DataContextWrapper.create(parentContext == null ? DataContext.EMPTY_CONTEXT : parentContext, provider);
    }
}
