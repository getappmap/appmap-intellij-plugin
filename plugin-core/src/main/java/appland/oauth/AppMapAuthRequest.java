package appland.oauth;

import appland.telemetry.Identity;
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirer;
import com.intellij.collaboration.auth.services.OAuthRequest;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.RestService;

import java.util.Map;
import java.util.UUID;

public class AppMapAuthRequest implements OAuthRequest<AppMapAuthCredentials> {
    private static final Url SERVER_URL = Urls.newFromEncoded("https://app.land/authn_provider");

    @Getter
    private final String nonce = UUID.randomUUID().toString();

    @NotNull
    @Override
    public Url getAuthUrlWithParameters() {
        var callbackUrl = getAuthorizationCodeUrl().addParameters(Map.of("nonce", nonce)).toExternalForm();
        return SERVER_URL.addParameters(Map.of(
                "redirect_url", callbackUrl,
                "source", "JetBrains",
                "azure_user_id", StringUtil.defaultIfEmpty(Identity.getOrCreateMachineId(), "")));
    }

    @NotNull
    @Override
    public Url getAuthorizationCodeUrl() {
        var port = BuiltInServerManager.getInstance().getPort();
        var path = "/" + AppMapOAuthService.SERVICE_NAME + "/authorization_code";
        return Urls.newFromEncoded("http://localhost:" + port + "/" + RestService.PREFIX + path);
    }

    @NotNull
    @Override
    public OAuthCredentialsAcquirer<AppMapAuthCredentials> getCredentialsAcquirer() {
        return new AppMapAuthCredentialsAcquirer();
    }
}
