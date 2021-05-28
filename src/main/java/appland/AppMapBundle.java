package appland;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

public final class AppMapBundle extends AbstractBundle {
    public static String get(@PropertyKey(resourceBundle = "messages.appland") String key) {
        return INSTANCE.getMessage(key);
    }

    public static final AppMapBundle INSTANCE = new AppMapBundle();

    private AppMapBundle() {
        super("messages.appland");
    }
}
