## Webview JavaScript Bundle

The webviews in `appland-webview` are bundled with [tsup](https://tsup.egoist.dev) into
`appland-webview/dist`. That directory is generated output and is not checked in.

Building the plugin therefore requires **Node.js on `PATH`**, at the version in `.nvmrc` — which
CI also uses, so `nvm use` (or any `.nvmrc`-aware version manager) matches it. Yarn does not need to be
installed: the pinned release is vendored in `appland-webview/.yarn/releases` and selected through
`yarnPath` in `.yarnrc.yml`, so the build invokes it directly with Node.

Gradle owns the bundle. `copyPluginAssets` depends on `bundleWebview`, so any build producing
plugin resources — `buildPlugin`, `runIde`, `test` — rebuilds it when the webview sources or
`yarn.lock` change, and skips it otherwise. To build only the bundle, run:

```shell
./gradlew bundleWebview
```

Dependencies are installed by a separate `yarnInstall` task, which re-runs only when
`package.json`, `yarn.lock`, `.yarnrc.yml` or the vendored Yarn release change. On CI it runs with
`--immutable`, making the lockfile authoritative; locally it may update `yarn.lock` in place, so a
dependency bump is just an edit to `package.json` followed by a build.

## Indexing of AppMap Data

### Developing or Testing For A Different Major Version

Release builds must be created with the earliest support version.
But testing or fixing API compatibility can be done for a different major version.

Property `platformVersion` in file `gradle.properties` defines which IDE platform to use.
File `gradle-$platformVersion.properties` contains the properties for the configured versin.

Use `./gradlew -PplatformVersion=...` to override the version on the command line.
For example, use `./gradlew -PplatformVersion=251` to run a sandbox IDE for 2025.1.

### Indexed `appmap.yml` Files

`appmap.yml` files, which are located in a project, are indexed.
The `.appmap.json` files located in the referenced `appmap_dir` directories are indexed automatically,
as long as these folders are not excluded.

### Excluded `appmap.yml` Files

In the IDE, excluded folders prevent indexing of any data.
Therefore, we implement `AppMapIndexedRootsSetContributor` to prevent this.
This class searches for `appmap.yml` files in the content roots of a project.
Values `appmap_dir` of these files are marked to be indexed even when such directory is excluded.

Content roots are usually the top-level directories of a project.
Therefore, `appmap.yml` files located in a nested, excluded folder won't be found by this implementation.
We're not collecting such files because iterating the complete directory tree is expensive without an index.

### Deployment Settings

To launch with automatically created deployment settings for testing, use
`./gradlew runIdeWithDeploymentSettings`.