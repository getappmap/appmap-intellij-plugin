package appland.oauth;

import com.intellij.collaboration.auth.services.OAuthServiceBase;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AppMapOAuthService extends OAuthServiceBase<AppMapAuthCredentials> {
    public static final String SERVICE_NAME = "appmap/oauth";

    public static @NotNull AppMapOAuthService getInstance() {
        return ApplicationManager.getApplication().getService(AppMapOAuthService.class);
    }

    @NotNull
    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    /**
     * Initiate OAuth authentication with the AppMap server.
     *
     * @return A new OAuth request
     */
    @NotNull
    public CompletableFuture<AppMapAuthCredentials> authorize() {
        getCurrentRequest().set(null);

        return authorize(new AppMapAuthRequest());
    }

    @Override
    public void revokeToken(@NotNull String s) {
        throw new IllegalStateException("Unsupported");
    }

    @Override
    public @Nullable OAuthResult<AppMapAuthCredentials> handleOAuthServerCallback(@NotNull String path, @NotNull Map<String, ? extends List<String>> parameters) {
        var request = getCurrentRequest().get();
        if (request == null) {
            return null;
        }

        var nonceParameter = parameters.get("nonce");
        if (nonceParameter == null || nonceParameter.size() != 1) {
            return new OAuthResult<>(request.getRequest(), false);
        }

        var appMapRequest = (AppMapAuthRequest) request.getRequest();
        if (!appMapRequest.getNonce().equals(nonceParameter.get(0))) {
            return new OAuthResult<>(request.getRequest(), false);
        }

        return super.handleOAuthServerCallback(path, parameters);
    }
}
