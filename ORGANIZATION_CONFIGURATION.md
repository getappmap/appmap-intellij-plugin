# Organization Configuration

This document is for IT administrators and enterprise deployers who want to centrally
configure the AppMap JetBrains plugin for their users — controlling telemetry routing,
AppMap CLI auto-update, and download sources.

It covers the **organization configuration** that is applied at runtime from a URL or a
local file. For baking settings into the plugin ZIP at build time instead, see the
[*Deployment* section of the README](./README.md#deployment); both use the same JSON schema.

## What you can configure

The configuration is a single JSON object. Every key is optional; omit a key to leave the
plugin's default (or the user's own setting) in effect.

```json
{
    "appMap.telemetry": {
        "backend": "splunk",
        "url": "https://splunk.example.com:443",
        "token": "my-hec-token",
        "ca": "system"
    },
    "appMap.autoUpdateTools": true,
    "appMap.manifest.appmapUrl": "https://artifacts.example.com/appmap-manifest.json",
    "appMap.manifest.scannerUrl": "https://artifacts.example.com/scanner-manifest.json"
}
```

| Key | Type | Effect |
| --- | --- | --- |
| `appMap.telemetry` | object \| `null` | Routes telemetry to your own backend. Only `splunk` is supported. See below. |
| `appMap.autoUpdateTools` | boolean | `true` (the default) lets the plugin download/update the AppMap & Scanner CLI binaries; `false` disables it. See the caveat below. |
| `appMap.manifest.appmapUrl` | string | Overrides the URL of the AppMap CLI [release manifest](https://github.com/getappmap/appmap-js/blob/main/architecture/release-manifests.md) (e.g. point it at an internal mirror). |
| `appMap.manifest.scannerUrl` | string | Overrides the URL of the Scanner CLI [release manifest](https://github.com/getappmap/appmap-js/blob/main/architecture/release-manifests.md). |

### Recommended setup: keep auto-update on, mirror the manifests

For enterprise deployment via organization configuration, **leave `appMap.autoUpdateTools` enabled
(`true`)** and point the manifest URLs at an internal mirror you control.

`appMap.autoUpdateTools: false` exists mainly for the case where the CLI binaries are *bundled into
the plugin ZIP* (see the README's *Bundling AppMap binaries* section), to avoid contacting upstream
at all. With organization configuration and **no** bundled binaries, disabling auto-update means
even the *initial* download is skipped — the binaries are never fetched and the plugin won't
function. So unless you bundle the binaries, keep auto-update on.

To keep downloads off the public internet, host the binaries and their manifests on an internal
mirror and set `appMap.manifest.appmapUrl` / `appMap.manifest.scannerUrl` accordingly. See the
[release manifest documentation](https://github.com/getappmap/appmap-js/blob/main/architecture/release-manifests.md)
for the manifest format and how mirroring works.

### Telemetry (`appMap.telemetry`)

| Field | Description |
| --- | --- |
| `backend` | Telemetry backend. Only `splunk` is currently supported. |
| `url` | Your Splunk HTTP Event Collector (HEC) endpoint. Include the port (usually `8088` or `443`). |
| `token` | Your Splunk HEC token. |
| `ca` | CA certificate for verifying the HEC server. `system` uses the OS trust store; a value starting with `@` is a path to a certificate file; any other value is treated as a literal PEM certificate. If omitted, the server certificate is **not** verified. |

When a Splunk backend is configured, telemetry is **mandatory**: the user's "disable telemetry"
setting is ignored and events are routed to your Splunk endpoint. With no `appMap.telemetry`,
the user's telemetry preference applies as usual.

## How to apply it

There are three ways to apply an organization configuration. All of them accept the same JSON.

1. **Environment variable (recommended for fleet deployment).**
   Set `APPMAP_CONFIG_URL` to an `http(s)://` or `file://` URL pointing at the JSON. The plugin
   fetches it at startup and re-fetches on later startups, so updating the file at that URL rolls
   out to everyone automatically. This takes effect only when no in-IDE URL has been set.

2. **In the IDE, by URL.**
   **Tools > AppMap > Apply Organization Configuration…** (also reachable from
   **Settings > Tools > AppMap > Advanced** and from the sign-in screen) → **Set URL**, then enter an
   `http(s)://` or `file://` URL. The plugin fetches it immediately and re-fetches on startup, so
   changes at the URL propagate.

3. **In the IDE, from a local file.**
   The same action → **Local File**, then choose a `.json` file. This applies a one-shot snapshot of
   the file's current contents (it is *not* re-read later).

A bundled `site-config.json` inside the plugin ZIP is a fourth, build-time option — see the README.

## How it interacts with user settings

Settings resolve in three layers, lowest to highest precedence:

1. **Bundled defaults** (plugin `site-config.json`, or built-in defaults)
2. **Organization configuration** (this document)
3. **User settings** (what the user sets in **Settings > Tools > AppMap**)

So a user can normally override an organization default. However, **applying an organization
configuration supersedes the user's setting for each key the configuration specifies**, so the
configuration actually takes effect rather than being silently masked:

- Applying a **new URL** or a **local file** supersedes the user's value for every key the
  configuration specifies.
- A repeat fetch from the **same URL** supersedes only the keys whose value **changed** since the
  last fetch — so a user can re-override a setting the organization isn't actively changing.

After an organization configuration is applied, the user can still change the setting again; the
configuration sets the effective default, not a hard lock.

## Behavior on failure / offline

- The last successfully applied configuration is cached. On startup, if the URL is unreachable, the
  cached configuration is used so users keep working offline.
- If a user **interactively** applies a URL that is unreachable or returns invalid content, the
  previously applied organization configuration is **cleared** and the user is warned — stale
  settings are not silently kept.
- A local file that fails to parse shows an error and leaves the current configuration untouched.

## Clearing an organization configuration

In **Settings > Tools > AppMap > Advanced**, when a configuration is active a **Clear** button
removes it (URL, cached configuration, applied settings). User settings are left as they are.

> If the configuration came from the `APPMAP_CONFIG_URL` environment variable, Clear cannot remove
> it permanently — it will be re-applied on the next startup until the variable is unset.

## Verifying what's applied

Run **Tools > AppMap > Plugin Status Report**. Under *Deployment Settings* it shows whether an
organization configuration is active, where it came from (URL / local file / environment), and the
full raw JSON that was applied — useful for confirming a rollout or debugging a user's machine.

## Guiding your users

A short note you can adapt for your internal docs:

> Your AppMap plugin is configured by **<your team>**. To (re)apply it, use
> **Tools > AppMap > Apply Organization Configuration…** and enter the URL
> `https://config.example.com/appmap/site-config.json` (or the path your team provides).
> To check what's currently applied, run **Tools > AppMap > Plugin Status Report**.
