package appland.deployment;

import org.jetbrains.annotations.Nullable;

/**
 * The single read path for the managed customer ID, which entitles a deployment to use the plugin without a
 * getappmap.com sign-in.
 * <p>
 * In enterprise deployments where licensing is settled by a B2B agreement, sending every developer through an
 * interactive sign-in is pure friction: it fails outright under network restrictions, and it creates audit
 * burden for a flow that grants nothing the contract hasn't already granted. An administrator sets
 * {@code appMap.customerId} through the channels they already use — the bundled {@code site-config.json} or an
 * organization configuration — and the plugin behaves as if signed in.
 * <p>
 * This is deliberately <em>not</em> a licence key and must never be described as an enforcement mechanism: the
 * plugin is open source, so any client-side check is a business-process boundary. The value is unverified, and
 * there is deliberately no user-settable equivalent in the settings UI.
 * <p>
 * Resolution is just the existing configuration merge — the organization configuration wins over the bundled
 * one, and {@link AppMapDeploymentSettings#getCustomerId()} collapses blank to absent so a blank organization
 * value can't de-entitle a bundled build.
 */
public final class Entitlement {
    private Entitlement() {
    }

    /**
     * @return The effective customer ID, trimmed, or {@code null} if none is configured.
     */
    public static @Nullable String getCustomerId() {
        return AppMapDeploymentSettingsService.getCachedDeploymentSettings().getCustomerId();
    }

    public static boolean isEntitled() {
        return getCustomerId() != null;
    }
}
