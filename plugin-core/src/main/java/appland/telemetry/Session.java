package appland.telemetry;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.intellij.ide.util.PropertiesComponent;

public class Session {
  @NotNull protected static final String SessionIdKey = "appland.session.id";
  @NotNull protected static final String SessionExpirationKey = "appland.session.expiration";

  public static String getId() {
    if (isExpired()) {
      renew();
    }

    var id = PropertiesComponent.getInstance().getValue(SessionIdKey);
    if (id == null) {
      id = renew();
    }
    return id;
  }

  protected static boolean isExpired() {
    var expirationString = PropertiesComponent.getInstance().getValue(SessionExpirationKey);
    if (expirationString == null) {
      return true;
    }

    var expiration = LocalDateTime.parse(expirationString);
    return expiration.isBefore(LocalDateTime.now());
  }

  protected static void touch() {
    var expiration = LocalDateTime.now().plusMinutes(30);
    PropertiesComponent.getInstance().setValue(SessionExpirationKey, expiration.toString());
  }

  // This method is called when the session is expired or doesn't exist. It generates a new session
  // ID and sets the expiration to 30 minutes from now. Returns the new session ID.
  protected static String renew() {
    var id = UUID.randomUUID().toString();
    PropertiesComponent.getInstance().setValue(SessionIdKey, id);
    touch();
    return id;
  }
}
