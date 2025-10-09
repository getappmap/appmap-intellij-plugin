[![GitHub Stars](https://img.shields.io/github/stars/getappmap/appmap-intellij-plugin?style=social)](https://github.com/getappmap/appmap-intellij-plugin)
[![Slack](https://img.shields.io/badge/Slack-Join%20the%20community-green)](https://appmap.io/slack)

# AppMap for JetBrains IDEs

Please see [./description.md](./description.md) for a detailed description of the AppMap plugin for JetBrains IDEs.

## Deployment

By default, the plugin ZIP file only bundles the JAR file of the AppMap Java agent.

It's possible to customize settings and/or to bundle AppMap binaries into the plugin ZIP afterward to simplify the
deployment.

### Customizing Deployment

By modifying the plugin ZIP archive, the deployment can be customized.

The current status of the deployment is shown with action `Tools > AppMap > Plugin Status Report`.

Settings can be customized by adding a `site-config.json` file at `./intellij-appmap/site-config.json` inside the ZIP file.

The file must be a valid JSON file.
It allows configuring telemetry and auto-update of AppMap binaries.
By default, no custom telemetry is configured and auto-update is enabled.

Default settings:

```json
{
    "appMap.telemetry": null,
    "appMap.autoUpdateTools": true
}
```

#### Splunk Telemetry

For telemetry deployment, only `splunk` is supported as backend.
If Splunk is configured, then the user-setting to turn off telemetry is ignored.

```json
{
    "appMap.telemetry": {
        "backend": "splunk",
        "url": "https://splunk.example.com",
        "token": "my-hec-token",
        "ca": "optional-ca-certificate"
    },
    "appMap.autoUpdateTools": true
}
```

### Bundling AppMap binaries

The following directory inside the plugin ZIP file is searched for AppMap binaries:

- `./intellij-appmap/resources/`

A bundled binary must follow this pattern to be found: `{type}-{os}-{arch}-v{version}`.
OS-specific file extensions like `.exe` must be appended.

If more than one compatible, bundled binary is found, then the binary with the latest version is preferred.

If a downloaded binary has a higher version than the bundled binary, then the downloaded binary is used.
If a downloaded binary and the best-matching bundled binary have the same version, then the bundled binary is used.

Possible values:

- `type`:
    - `appmap`
    - `scanner`
- `os`:
    - `win`
    - `macos`
    - `linux`
- `arch`:
    - `x64`
    - `arm64`
- `version`: a version in the SemVer format, e.g. `1.2.3`.

Examples:

- `./intellij-appmap/resources/appmap-win-x64-v3.9.1.exe`
- `./intellij-appmap/resources/appmap-macos-arm64-v3.9.1.exe`
- `./intellij-appmap/resources/appmap-linux-arm64-v3.9.1.exe`
