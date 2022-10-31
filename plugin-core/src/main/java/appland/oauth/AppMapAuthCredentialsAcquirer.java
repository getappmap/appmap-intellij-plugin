package appland.oauth;

import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirer;
import org.jetbrains.annotations.NotNull;

class AppMapAuthCredentialsAcquirer implements OAuthCredentialsAcquirer<AppMapAuthCredentials> {
    @NotNull
    @Override
    public AcquireCredentialsResult<AppMapAuthCredentials> acquireCredentials(@NotNull String code) {
        // fixme
        return new AcquireCredentialsResult.Success<>(new AppMapAuthCredentials(code));
    }
}
