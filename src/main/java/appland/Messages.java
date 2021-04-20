package appland;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

public class Messages extends AbstractBundle {
    public static String get(@PropertyKey(resourceBundle = "messages.appland") String key) {
        return INSTANCE.getMessage(key);
    }

    public static final Messages INSTANCE = new Messages();

    private Messages() {
        super("/messages/appland.properties");
    }
}
