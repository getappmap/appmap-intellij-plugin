package appland.oauth;

import com.intellij.collaboration.auth.credentials.Credentials;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class AppMapAuthCredentials implements Credentials {
    String token;

    @NotNull
    @Override
    public String getAccessToken() {
        return token;
    }
}
