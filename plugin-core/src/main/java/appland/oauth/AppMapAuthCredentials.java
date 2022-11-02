package appland.oauth;

import com.intellij.collaboration.auth.credentials.Credentials;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Data
public class AppMapAuthCredentials implements Credentials {
    @Getter(AccessLevel.NONE)
    private final String token;

    @NotNull
    @Override
    public String getAccessToken() {
        return token;
    }
}
