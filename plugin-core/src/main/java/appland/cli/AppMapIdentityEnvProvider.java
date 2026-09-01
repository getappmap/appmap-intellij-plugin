package appland.cli;

import appland.deployment.Entitlement;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.util.text.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Tells the CLI who the user is: a getappmap.com session token, an administrator-set customer ID, or both.
 * <p>
 * The two are independent axes. {@code APPMAP_API_KEY} identifies an individual user and is what
 * non-enterprise usage is tracked against; {@code APPMAP_CUSTOMER_ID} attributes usage to a customer whose
 * licensing is settled by a B2B agreement, and is passed whenever it is set, regardless of whether anyone is
 * signed in. Neither is faked in the absence of the other — a variable is omitted rather than sent empty, so
 * the CLI can tell "no session" from "empty session".
 * <p>
 * Registered {@code order="last"} so no other environment provider can override a credential.
 */
public class AppMapIdentityEnvProvider implements AppLandCliEnvProvider {
    @Override
    public Map<String, String> getEnvironment() {
        var environment = new HashMap<String, String>();

        var apiKey = AppMapApplicationSettingsService.getInstance().getApiKey();
        if (StringUtil.isNotEmpty(apiKey)) {
            environment.put("APPMAP_API_KEY", apiKey);
        }

        // When both are present the CLI receives both. Implementing "the real key authenticates, the customer
        // ID is attribution only" is the CLI's job; in practice both-present is a migration artefact, from
        // users who authenticated before entitlement existed.
        var customerId = Entitlement.getCustomerId();
        if (customerId != null) {
            environment.put("APPMAP_CUSTOMER_ID", customerId);
        }

        return Map.copyOf(environment);
    }
}
