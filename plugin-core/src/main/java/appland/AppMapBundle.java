package appland;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class AppMapBundle extends AbstractBundle {
    public static final AppMapBundle INSTANCE = new AppMapBundle();

    private AppMapBundle() {
        super("messages.appland");
    }

    public static String get(@PropertyKey(resourceBundle = "messages.appland") String key) {
        return INSTANCE.getMessage(key);
    }

    public static String get(@PropertyKey(resourceBundle = "messages.appland") String key, @NotNull Object param1) {
        return INSTANCE.getMessage(key, param1);
    }

    public static String get(@PropertyKey(resourceBundle = "messages.appland") String key, @NotNull Object param1, @NotNull Object param2) {
        return INSTANCE.getMessage(key, param1, param2);
    }

    public static Supplier<String> lazy(@PropertyKey(resourceBundle = "messages.appland") String key) {
        return () -> INSTANCE.getMessage(key);
    }
}
