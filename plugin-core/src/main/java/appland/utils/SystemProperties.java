package appland.utils;

import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

public class SystemProperties {
    public static final String USER_HOME = "user.home";

    public static @NotNull String getUserHome() {
        return System.getProperty(USER_HOME);
    }

    public static <T extends Throwable> void withPropertyValue(@NotNull String propertyName,
                                                               @NotNull String value,
                                                               @NotNull ThrowableRunnable<T> block) throws T {
        var oldValue = System.getProperty(propertyName);
        try {
            System.setProperty(propertyName, value);
            block.run();
        } finally {
            System.setProperty(propertyName, oldValue);
        }
    }
}
