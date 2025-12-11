AppMap Plugin Changelog

## [0.82.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.82.0...v0.82.1) (2025-12-11)


### Bug Fixes

-  **cli:** Correctly parse CLI tool versions from GitHub releases ([239bb4f]())-  **telemetry:** Skip usage notification when telemetry is always enabled ([0ae39c5]())

## [0.82.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.81.0...v0.82.0) (2025-11-17)


### Features

-  Use GitHub releases for CLI version checks ([3e07ae5]())

### Bug Fixes

-  cleanup after proxy test ([53220df]())-  update caching of AppMap CLI versions to only include the latest available version ([f53465a]())-  use GitHub API token when requesting latest releases in unit test mode ([fb01ac0]())

## [0.81.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.80.2...v0.81.0) (2025-10-29)


### Features

-  implement Splunk telemetry reporter ([459d1cc]())-  show the deployment settings in the AppMap status report ([728e3a7]())-  support binary assets bundled with the plugin package ([6354d7c]())-  support deployment settings via site-config.json and use it to configure splunk telemetry ([b76deec]())-  **telemetry:** Enhance telemetry with common properties and CLI support ([0a98488]())

### Bug Fixes

-  better error handling for webview-based editors ([45393f0]())-  incompatibilities of 2025.3 EAP ([448cb5c]())-  NPE generating AppMap status report with deployment settings ([23d500e]())-  tests with 2025.3 eap ([7a49d09]())-  the timeout of 20s to load AppMap stats prevents large AppMap to open ([65a4cd8]())

## [0.80.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.80.1...v0.80.2) (2025-08-25)


### Bug Fixes

-  don't show multiple notifications about Copilot after a project is opened ([f2f4a23]())-  don't show notification about Copilot not being authenticated ([7d5de47]())-  replace non-breaking hyphens with a regular dash to satisfy the JetBrains plugin verifier ([1767210]())-  the check for the "navie is now integrated with Copilot" notification was wrong ([5167f67]())

## [0.80.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.80.0...v0.80.1) (2025-05-01)


### Bug Fixes

-  Highlighted code selections should now be included in all cases ([c6e4ebc]())-  Improve render support for indented code blocks ([9ed7c00]())-  Pinned items can now be appended in all cases ([27fe7f4]())

## [0.80.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.79.0...v0.80.0) (2025-04-28)


### Features

-  apply model config values to the AppMap JSON-RPC service ([24fa653]())-  Integrate chat history ([2509c14]())-  Navie model selector ([425db71]())

## [0.79.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.78.0...v0.79.0) (2025-03-14)


### Features

-  Add some logging to github copilot proxy ([cd19a0b]())-  style AppMap webviews to match the IDE's theme, update Navie webview ([fcfabe6]())-  support "save as" in Navie webview ([189e6ac]())

### Bug Fixes

-  Actually send error responses to the client in the copilot proxy ([8a7c07f]())-  IDE styling was not always applied to webviews ([de1550f]())-  Retrieve service on demand to avoid a warning on 2025.1 about class initialization of services ([c0e6ce0]())-  Set timeout on chat completion requests, show log on io errors ([34098ed]())

## [0.78.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.77.0...v0.78.0) (2025-02-19)


### Features

-  Add logging for the AppMap RPC server ([3fc5562]())-  Download content exclusion lists from GitHub ([bedc434]())-  support restarting AppMap JSON-RPC without reopening the project ([b78a649]())

### Bug Fixes

-  don't delay Navie UI until IDE finished indexing ([c85c0a1]())

## [0.77.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.76.1...v0.77.0) (2025-01-24)


### Features

-  Show notification if the download of an AppMap CLI binary failed ([64db02b]())-  support 2025.1 ([aecef20]())-  support to choose the copilot model for Navie ([e5bb5fc]())

## [0.76.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.76.0...v0.76.1) (2025-01-21)


### Bug Fixes

-  in the integration with Copilot, loading the models failed ([d8a08e7]())

## [0.76.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.75.1...v0.76.0) (2025-01-15)


### Features

-  integrate Navie with GitHub Copilot ([f801960]())-  Open files for link embedded in Navie's content ([f026178]())-  update Navie ([40a2d3d]())-  update Navie webview ([42cca97]())-  Update the marketplace content ([0258d66]())-  update webview navie to the latest version ([f1027bf]())-  update webview navie to the latest version ([739c240]())

### Bug Fixes

-  Remove README.md content which mirrors description.md ([8d6b5fd]())-  Update @appland/components to v4.43.2 ([6e018d0]())

## [0.75.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.75.0...v0.75.1) (2024-11-01)


### Bug Fixes

-  debounce requests for a filesystem refresh ([464fa2f]())-  don't delay loading of AppMaps in the AppMap panel ([41113b0]())-  don't refresh AppMap panel after a filesystem refresh unrelated to AppMaps ([90f1a05]())-  Update the screenshots to png as jetbrains doesn't support webp rendering ([#804](https://github.com/getappmap/appmap-intellij-plugin/issues/804)) ([9e3a375]())-  Update the sign in to @appland/components v4.40.0 ([83912b4]())

## [0.75.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.74.1...v0.75.0) (2024-10-17)


### Features

-  Support 2024.3 eap ([f8f9696]())-  Support pinned files for Navie webview ([814f24b]())-  Upgrade the Navie frontend to v4.39.0 ([89aadd3]())

### Bug Fixes

-  show new file pinning UI ([5be1ebf]())-  Update @appland/components to v4.37.4 ([6d39706]())

## [0.74.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.74.0...v0.74.1) (2024-09-17)


### Bug Fixes

-  Update @appland/components to v4.37.3 ([c679884]())

## [0.74.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.73.0...v0.74.0) (2024-09-14)


### Features

-  Update @appland/components to v4.36.0 ([3bdc6c8]())

### Bug Fixes

-  don't show AppMap problems view and the Runtime Analysis if scanner is off ([231df0c]())-  make the AppMap viewer the default view for .appmap.json files ([756f0d4]())

## [0.73.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.72.0...v0.73.0) (2024-08-16)


### Features

-  add action to open the instructions from the AppMap toolwindow ([c65f55b]())-  Add command completions to Navie ([43e8e09]())

### Bug Fixes

-  "New chat" button opens a new chat ([2ab32d6]())-  avoid exception about slow operation when loading OpenAI key ([0e10a11]())-  don't refresh the Navie editor after a generic filesystem refresh ([855561e]())-  don't restart JSON-RPC service if the restart has been disabled ([6b261d9]())-  focus open install guide webview editor when it's invoked from the toolwindow ([886bda3]())-  obeye dumb mode when looking up appmap.yml files ([e3bdd79]())-  refactor refresh of findings to avoid concurrent updates and too many updates in a short time frame ([67625e7]())-  refresh Navie editor after change of indexing status ([a591a5f]())

## [0.72.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.71.0...v0.72.0) (2024-07-31)


### Features

-  Update @appland/components to v4.30.0 ([a3d79ee]())

## [0.71.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.70.0...v0.71.0) (2024-07-22)


### Features

-  Update @appland/components to v4.29.0 ([50ed4bf]())

## [0.70.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.69.0...v0.70.0) (2024-07-15)


### Features

-  support 2024.2 eap ([c64d5e2]())-  support prompt passed from install instructions to Navie ([d6f82db]())-  update webview install-guide to the latest version ([8f7efb7]())-  update webview navie to the latest version ([b9e3950]())

### Bug Fixes

-  Normalize casing of dependencies ([8501c27]())

## [0.69.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.68.0...v0.69.0) (2024-06-21)


### Features

-  notify when sign-in to AppMap is attempted with broken text input support ([f8a0d58]())-  update webview signin to the latest version ([2632f6c]())

## [0.68.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.67.2...v0.68.0) (2024-06-12)


### Features

-  add setting to control launch of AppMap scanner ([2a47d22]())-  fall back to bundled agent JAR if the download of the latest release failed ([688b081]())-  restore compatibility with 2023.1 ([b355cb9]())-  update webview install-guide to the latest version ([2740c23]())

### Bug Fixes

-  handle giant AppMaps in AppMaps webview ([d549dca]())-  handle timeout when terminating processes on Windows ([9d3722d]())-  NPE with 2024.1 for proxy server without exceptions ([f32199a]())-  show unsupported projects in Installation Guide webview ([64c44c5]())-  show warning for users of 2024.1 with proxy servers ([533f82d]())

## [0.67.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.67.1...v0.67.2) (2024-05-20)


### Bug Fixes

-  Drop token interpolation ([b380d79]())

## [0.67.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.67.0...v0.67.1) (2024-05-15)


### Bug Fixes

-  Update the context view on subsequent requests ([56ec416]())

## [0.67.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.66.0...v0.67.0) (2024-05-15)


### Features

-  pass IDE proxy settings to the AppMap CLI tools ([98c9954]())

### Bug Fixes

-  Drop mode buttons ([4900876]())-  only include source directories in config ([d5ba6c7]())-  pass code editor to Navie JSON-RPC server ([d3e9939]())-  update an opened Navie view when appmap.yml or new AppMap are created ([825134d]())

## [0.66.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.65.0...v0.66.0) (2024-05-09)


### Features

-  provide selection of current editor to Navie if it's opened from the tool window ([93a0969]())-  support to configure the model used by Navie ([e8c8374]())

### Bug Fixes

-  set language property in default config ([dab9b54]())

## [0.65.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.64.0...v0.65.0) (2024-05-01)


### Features

-  Add appmap status report ([#644](https://github.com/getappmap/appmap-intellij-plugin/issues/644)) ([35bf0c6]())-  Allow users to specify their own OpenAI key in the settings ([19a4230]())-  drop support for 2022.1, 2022.2, 2022.3 and 2023.1 ([4c756aa]())-  new Navie section in the AppMap toolwindow ([6240f56]())-  send project content roots to local AppLand JSON-RPC service ([8fb5d83]())-  support Navie webview message "open-location" ([bd35245]())-  Update Navie @appland/components to 4.22.0 ([2b71585]())

### Bug Fixes

-  append AppMap argline to an existing argline ([5f4c443]())-  do not remove the process reference if it's not the terminated process ([c760d1a]())-  don't show installation guide after first sign-in ([faa9fd5]())-  error "Project is already disposed" ([f6feb2e]())-  fix exceptions "already disposed" ([53e001b]())-  improve support for projects with multiple modules ([3ff6d6f]())

## [0.64.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.63.0...v0.64.0) (2024-04-10)


### Features

-  Support to open Navie from AppMap webview ([e2fac4b]())

### Bug Fixes

-  create appmap.yml in the correct directory when used with Gradle ([253852a]())-  log cause when AppMap failed to load ([7047f39]())-  Use configured AppMap service environment for RPC service ([190cbff]())

## [0.63.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.62.0...v0.63.0) (2024-04-05)


### Features

-  Allow configuring environment for AppMap services ([#648](https://github.com/getappmap/appmap-intellij-plugin/issues/648)) ([a0b5a31]()), closes [#646]()

## [0.62.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.61.0...v0.62.0) (2024-04-03)


### Features

-  download Java agent from Maven Central ([bb622c0]())-  launch AppMap JSON-RPC server and use it for Navie ([7673926]())

## [0.61.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.60.0...v0.61.0) (2024-03-22)


### Features

-  bundle the AppMap Java agent with plugin ([8206a39]())

### Bug Fixes

-  Bring back Travis for semantic release ([9a276ea]())-  don't terminate AppMap processes on the EDT, [#597](https://github.com/getappmap/appmap-intellij-plugin/issues/597) ([6bf9cb2]())-  properly restart crashed AppMap processes with a longer delay ([3964aa1]())-  Update @appland/components to v4.10.0 ([4d752cc]())

## [0.60.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.59.0...v0.60.0) (2024-03-14)


### Features

-  show most recent AppMaps in Navie webview ([70c4ef2]())-  support email sign-up ([82e271b]())-  update the instructions panel to allow opening Navie AI ([56687a8]())

## [0.59.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.58.0...v0.59.0) (2024-03-11)


### Features

-  notify user if Navie isn't fully functional ([#595](https://github.com/getappmap/appmap-intellij-plugin/issues/595)) ([9b5f184]())-  support to display the AppMap tool window from the Navie webview ([bb25a8b]())-  Update install guide to include new node instructions ([d7815d7]())

### Bug Fixes

-  compatibility with 2024.1 eap's backwards incompatible API of OAuthRequestHandlerBase ([5fc79c2]())-  open external links with a target attribute like plain external links ([4494a4b]())-  update actions to avoid exception "ActionUpdateThread.OLD_EDT is deprecated and going to be removed soon." with 2024.1 eap ([d6d18ca]())

## [0.58.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.57.0...v0.58.0) (2024-02-23)


### Features

-  Support the "Export JSON" feature and handle webview messages in Navie view. ([2b2dae2]())

### Bug Fixes

-  allow to choose a file or directory for "Export to SVG" ([a9645f2]())-  compatibility with 2024.1 eap5 ([b6e89a0]())-  Exception "java.lang.IllegalArgumentException: invalid hex byte" ([96b07fe]())-  polish "Export to SVG" ([0f1a05e]())-  possible freeze of UI if "Explain with Navie" is invoked during indexing ([e23775e]())-  show local file paths when exporting SVG from "Explain with Navie" ([3e9e2cb]())-  Update description ([e77b46a]())

## [0.57.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.56.0...v0.57.0) (2024-02-17)


### Features

-  terminate plugin verifier with failure if internal API is used ([a6794f6]())

### Bug Fixes

-  drop internal API usage of FileIndexFacade.isInProjectScope ([a0ef8b7]())-  internal API usage of JavaVersion. ([922b80d]())

## [0.56.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.55.0...v0.56.0) (2024-02-14)


### Features

-  drop support for JetBrains 2021.3 IDEs ([037c29e]())-  Integrate with AppMap Navie ([efec7db]())-  support AppMap CLI processes for nested AppMap directories ([e0a1bf7]())-  support JetBrains 2024.1 EAP releases ([ae517a8]())

### Bug Fixes

-  Add missing import so that dependency map is not blank ([57bae45]())-  Avoid 404 when loading an AppMap ([94760bd]())-  Don't locate indexer port based on open editor ([c434196]())-  retrieve Gradle JDK in a background thread ([15b9bd1]())-  workaround for exception "Read access is allowed from inside read-action or Event Dispatch Thread" ([a8a26c8]())

## [0.55.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.54.0...v0.55.0) (2024-02-05)


### Features

-  enable JSON-RPC of AppMap indexer and pass API key ([aeb279d]())-  Support Java 21 in the plugin ([08ab253]())

### Bug Fixes

-  add auth token query parameter to HTTP requests of webview assets ([d8ee3e0]())-  don't open links to a localhost URL in the external browser ([989dc3c]())-  exception about slow operations when the AppMaps of the AppMap tool window are loaded ([aba4917]())-  NPE when the AppMap is displayed for the first time ([9ae8a8e]())

## [0.54.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.53.0...v0.54.0) (2023-12-13)


### Features

-  Action to sign in with a text API key ([3199ed2]())

### Bug Fixes

-  Exception 'Read access is allowed from inside read-action' with 2023.3 ([2f412f6]())-  restart AppMap CLI processes when they terminate unexpectedly ([ab91c81]())

## [0.53.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.52.0...v0.53.0) (2023-11-29)


### Features

-  Better check for status of the "Runtime Anaylysis" step ([7e5b29e]())-  Update @appland/components to 3.13.2 for the appmap viewer ([0d3fcce]())

### Bug Fixes

-  reject that PsiFile is fetched on the EDT thread ([a61a9ee]())

## [0.52.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.51.0...v0.52.0) (2023-10-31)


### Features

-  remove corresponding AppMap index data directory when an AppMap JSON file is deleted ([c030e16]())-  sort AppMaps of request or remote recording by modification date ([7154a12]())-  support "Delete AppMaps" and "Delete all AppMaps" in the AppMap tool window ([7361a26]())-  Update @appland/components to 3.9.0 ([5667237]())-  when opening an AppMap for a finding, center on the source finding ([fc1602c]())

### Bug Fixes

-  open findings from overview, which don't have an attached source file ([26c318b]())-  remote recordings now always display the name given by the user ([e7970e6]())-  rendering of findings were different compared to VSCode ([a2ebf7a]())

## [0.51.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.50.0...v0.51.0) (2023-10-11)


### Features

-  remove telemetry except of messages, which may indicate a bug in our code ([19ac6b6]())-  show hierarchy of AppMaps in the AppMap tool window ([c477d4b]())-  support 2023.3 eap ([#461](https://github.com/getappmap/appmap-intellij-plugin/issues/461)) ([2fecbac]())-  Update @appland/components to 3.8.0 ([f0ba300]())-  update to latest AppLand JS components ([#463](https://github.com/getappmap/appmap-intellij-plugin/issues/463)) ([f911c59]())

### Bug Fixes

-  don't override properties of an existing appmap.yml file ([cefd915]())-  navigating to a query or request code object didn't show it in the AppMap view ([e4eef31]())-  properly unescape indexer file paths ([d4b106e]())-  refresh parent directory of new AppMap to enforce indexing of the AppMap metadata directory ([dcfe030]())-  show progress spinner when loading AppMaps ([147b334]())

## [0.50.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.49.0...v0.50.0) (2023-09-19)


### Features

-  show installation instructions after first sign in to AppMap ([7a21d83]())

## [0.49.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.48.0...v0.49.0) (2023-09-14)

### Features

- Default output directory for AppMaps now tmp for Java projects 
- Changed the "Apply" button to "Load" on the Filter dialog
- Findings are always enabled if the user is signed in
- OpenAPI removed from the onboarding flow
- Onboarding flow begins immediately after plugin installation
- Sequence diagram fixes for readability and usability

### Bug Fixes

- appmap.yml no longer overwritten by "Run with AppMap" if it already exists
- Simplified file change watching algorithm
- Findings now open to the correct map view
- Findings without associated source files are now shown in the Runtime Analysis tree
- No longer passing unnecessary appmap.output.directory to the AppMap agent

## [0.48.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.47.0...v0.48.0) (2023-09-01)

### Bug Fixes

-  don't test for local filesystem when indexing AppMap metadata files, extract indexer into field to follow the IntelliJ SDK's implementations, show more information about indexing and empty index values in debug mode ([6589559]())
-  make our custom extension points dynamic to not prevent dynamic loading of the AppMap plugin ([ff2b959]())
-  move text input to filter AppMaps into the AppMaps subpanel of the AppMap tool window ([9662509]())

## [0.47.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.46.0...v0.47.0) (2023-08-29)


### Features

-  display findings without known source file location in the "Runtime Analysis" panel

### Bug Fixes

-  update status of install guide step "Runtime Analysis" when the user navigates to the page for the first time
-  update "Runtime Analysis" step in the AppMap instructions panel when the user opens the page in the AppMap installation guide webview. Use the same logic as the update of the webview status.

## [0.46.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.7...v0.46.0) (2023-08-23)


### Features

-  group items in the "Runtime Analysis" panel ([4c10e57]())

## [0.45.7](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.6...v0.45.7) (2023-08-22)


### Bug Fixes

-  update instructions for java agent jar location ([3ccffdb]())

## [0.45.6](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.5...v0.45.6) (2023-08-18)


### Bug Fixes

-  exception "Read access is not allowed" when "Install Guide" view is displayed ([54ff60a]())-  mark "Install AppMap agent" as completed for Java projects ([84115d5]())

## [0.45.5](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.4...v0.45.5) (2023-08-11)


### Bug Fixes

-  fallback to "build/appmap" for AppMap output directory if a Gradle-based run configuration is executed ([bfb8acd]())-  fallback to "target/appmap" for AppMap output directory if a Maven-based run configuration is executed ([d223bd2]())

## [0.45.4](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.3...v0.45.4) (2023-08-11)


### Bug Fixes

-  restrict the search for top-level Java packages for "Execute with AppMap". ([999d9df]())

## [0.45.3](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.2...v0.45.3) (2023-08-11)


### Bug Fixes

-  don't check assertion on the EDT, which always has read access enabled ([d77cf14]())-  use read action to fix exception "Read action required" with 2023.2 ([9f1078b]())

## [0.45.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.1...v0.45.2) (2023-08-09)


### Bug Fixes

-  compilation with 2023.2 ([8d3a1cb]())-  Disable "Run with AppMap" while indexing is in progress. "Run with AppMap" needs index access to locate the top-level Java packages. ([6671735]())-  refresh local file system after a new appmap.yml file was created ([7d316b1]())-  When "Run with AppMap" is invoked, create a missing appmap with a modal task and outside a ReadAction to avoid dead-locks, e.g. when indexing is in progress. ([b6573b7]())

## [0.45.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.45.0...v0.45.1) (2023-08-09)


### Bug Fixes

-  sync the local filesystem after a run configuration was executed with the AppMap executor ([9f8737a]())-  watch AppMap output directories and add indexable file set contributor for 2023.2 ([a170d7e]())-  watch AppMap output directories and add indexable file set contributor for 2023.2 ([be04187]())

## [0.45.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.44.2...v0.45.0) (2023-08-02)


### Features

-  reorder panels in the AppMap toolwindow ([8a78cec]())

### Bug Fixes

-  correctly calculate maximum size of collapsed panels in the AppMap tool window ([a684a2e]())-  properly update maximum size of collapsed panels, restore collapsed state when the toolwindow is recreated ([78ce4bd]())-  use correct icons show show display the collapsed state of panels inside the AppMap toolwindow ([49314da]())

## [0.44.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.44.1...v0.44.2) (2023-08-01)


### Bug Fixes

-  always show icons of install guide links in the same color ([df5a991]())

## [0.44.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.44.0...v0.44.1) (2023-07-28)


### Bug Fixes

-  don't execute JDK validation on the EDT ([a9863ff]())-  improve empty message in the AppMap tool window ([33938d3]())

## [0.44.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.43.0...v0.44.0) (2023-07-19)


### Features

-  show user progress in the install guide webview ([51b88d1]())

### Bug Fixes

-  don't show "your project has not been scanned" if there's at least one appmap-findings.json file in a project ([615c3e9]())

## [0.43.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.42.1...v0.43.0) (2023-07-14)


### Features

-  detect JS frameworks in package-lock.json and yarn.lock files ([caf8222]())

## [0.42.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.42.0...v0.42.1) (2023-07-10)


### Bug Fixes

-  show AppMap icons for our webviews ([e14536c]())

## [0.42.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.41.0...v0.42.0) (2023-07-08)


### Features

-  show flame graphs in the AppMap webview ([25ff566]())

## [0.41.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.40.1...v0.41.0) (2023-07-08)


### Features

-  support AppMap filters in the AppMap webview ([6e1db53]())

## [0.40.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.40.0...v0.40.1) (2023-06-28)


### Bug Fixes

-  create appmap_dir of a appmap.yml file as system-independent path ([cd492c2]())-  use correct path for user home directory ([68ad869]())

## [0.40.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.39.0...v0.40.0) (2023-06-27)


### Features

-  set appmap_dir before executing a run configuration with "Execute with AppMap" ([e520af5]())

## [0.39.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.38.0...v0.39.0) (2023-06-26)


### Features

-  support 2023.2 EAP ([9928f24]())

## [0.38.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.37.0...v0.38.0) (2023-06-23)


### Features

-  download the latest AppMap Java agent at startup ([0333d46]())

## [0.37.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.36.4...v0.37.0) (2023-06-22)


### Features

-  index AppMap metadata instead of AppMap files itself to improve indexing performance ([3fe70c9]())

### Bug Fixes

-  avoid empty list of findings after a project is opened ([6914c1d]())-  don't show AppMaps, which are not inside a content root of the current project ([8f240e5]())-  update help link of the notification about failed remote recording ([fb58de3]())

## [0.36.4](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.36.3...v0.36.4) (2023-06-16)


### Bug Fixes

-  don't reload AppMap webview if it's not fully initialized yet ([3d5b5d0]())-  don't use a non-blocking ReadAction to apply changes to the findings manager ([0746237]())

## [0.36.3](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.36.2...v0.36.3) (2023-06-14)


### Bug Fixes

-  execute the AppMap install command with a PTY ([26bcbac]())-  stop AppMap recording: use appmap_dir defined in appmap.yml, don't use the most recently used location. Default to `target/appmap/remote` instead of `tmp/appmap/remote` as a fallback. ([322366f]())

## [0.36.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.36.1...v0.36.2) (2023-06-13)


### Bug Fixes

-  ensure to load up-to-date content of newly created AppMap files ([af24b19]())

## [0.36.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.36.0...v0.36.1) (2023-06-06)


### Bug Fixes

-  ensure reload findings files in a background thread with recent platform versions ([503e297]())-  improve performance to index and to look up AppMap files in the project ([88e55ce]())

## [0.36.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.35.3...v0.36.0) (2023-06-06)


### Features

-  bundle java-agent 1.17.2 ([dba03ff]())

### Bug Fixes

-  warn about missing support for JCEF webviews instead of causing an exception when a project is opened. ([ea1b597]())

## [0.35.3](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.35.2...v0.35.3) (2023-06-05)


### Bug Fixes

-  properly handle reload of finding files to avoid an empty list of findings in the UI ([329f37c]())-  restrict runtime of the AppMap stats command with a timeout (20s) ([7e93fde]())

## [0.35.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.35.1...v0.35.2) (2023-06-01)


### Bug Fixes

-  Update documentation links ([b4eaa15]())

## [0.35.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.35.0...v0.35.1) (2023-06-01)


### Bug Fixes

-  don't break index of AppMap code objects, exception "java.lang.IllegalStateException: Unexpected name: external-route" ([9794077]())

## [0.35.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.34.1...v0.35.0) (2023-05-12)


### Features

-  expand packages in sequence diagrams ([c39ba31]())-  remove not ready to sign-in toggle ([fed31e8]())

## [0.34.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.34.0...v0.34.1) (2023-05-06)


### Bug Fixes

-  compare with system independent paths to fix CI on Windows ([f84350d]())-  use GeneralCommandLine PtyCommandLine to avoid receiving ANSI escape sequences in the output of the AppMap CLI stats command ([4643e39]())-  wait for process termination to fix tests on Windows, which does not support deleting directories still being used ([81466df]())

## [0.34.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.33.1...v0.34.0) (2023-05-04)


### Features

-  show SignIn webview when a user is not authenticated ([92c1d2e]())

## [0.33.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.33.0...v0.33.1) (2023-05-04)


### Bug Fixes

-  properly handle clicks on "Clear selection" in the AppMap webview ([7023589]())

## [0.33.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.32.1...v0.33.0) (2023-04-28)


### Features

-  prune large AppMap using the AppMap CLI to support large and giant AppMaps in the editor ([57217b7]())

## [0.32.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.32.0...v0.32.1) (2023-04-21)


### Bug Fixes

-  send user id when signing in to AppMap server ([b60eed5]())

## [0.32.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.31.0...v0.32.0) (2023-04-18)


### Features

-  show sequence diagram view by default ([6a82d68]())

### Bug Fixes

-  exception "PSI element is provided on EDT" with 2023.1 ([ac5188c]())-  open AppMap for all types of code objects ([eae4c69]())-  select code object after opening it in the AppMap view ([5748fba]())

## [0.31.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.30.1...v0.31.0) (2023-03-29)


### Features

-  support exporting AppMap sequence diagrams as SVG file ([8b2d537]())-  support the new AppMap sequence diagrams ([3cf7e1e]())

### Bug Fixes

-  support building the JS dist file with latest webpack ([ea2f9c0]())

## [0.30.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.30.0...v0.30.1) (2023-03-22)


### Bug Fixes

-  properly detect findings, which have a Windows path in the stacktrace ([3097a9d]())

## [0.30.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.29.0...v0.30.0) (2023-03-22)


### Features

-  Support 2023.1 ([3b0c218]())

### Bug Fixes

-  avoid experimental API to remain compatible with 2023.1 ([1efc92b]())

## [0.29.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.28.0...v0.29.0) (2023-03-15)


### Features

-  new "Code objects" panel with a tree of packages, routes, and queries ([0061888]())

### Bug Fixes

-  bundle instrumented JAR files into the plugin to fix NPE using the remote recording actions ([ddcb211]())-  clean up code to build the tree hierarchy ([d8e3aef]())-  create watched AppMap directory before launching indexer or scanner ([5410de0]())-  improve layout of the AppMap tool window to handle space token by collapsed panels ([51ce161]())-  locate source files on Windows ([587ce9f]())-  open source location of function class map items instead of the source location of the related AppMap file ([ee3ab15]())-  show names of AppMaps if multiple files are available to navigate to a Code Objects node ([2fb6724]())-  truncate display string of class map item labels ([01345f5]())-  update AppMap status labels on EDT after AppMap API key change ([a05b07d]())

## [0.28.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.27.0...v0.28.0) (2023-02-14)


### Features

-  open the AppMap webview if a finding node without known source location is executed in the problems view ([72174d0]())-  support findings in the AppMap webview editor ([a2052a6]())

### Bug Fixes

-  open stack locations of findings at the right line. property "range" is an array, not a plain object ([9330fa6]())-  pass url property to the AppMap JSON application to fix the "learn more" link of finding details ([75193d4]())

## [0.27.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.26.1...v0.27.0) (2023-02-07)


### Features

-  add telemetry to docs links in tool window ([f5f3091]())-  send telemetry for appmap events ([9b849c2]())

## [0.26.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.26.0...v0.26.1) (2023-02-03)


### Bug Fixes

-  update supported Java versions in error message ([7949e9f]())

## [0.26.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.25.0...v0.26.0) (2023-01-27)


### Features

-  use the configured location from appmap.yml as default for the "Stop remote recording" dialog. If unavailable, fall back to "projectRootDir/tmp/appmap/remote". ([8066fb9]())

### Bug Fixes

-  accept directory locations for "Stop AppMap recording", which do not exist ([7ea38e7]())-  delete (partially) recorded AppMap file if "stop remote recording" response handling failed ([d0a6587]())-  don't abort "stop remote recording" if the update of the metadata failed ([3e3bb33]())-  move lookup of the AppMap default storage location into the background to allow for a time-consuming lookup without blocking the UI ([d171d17]())-  show complete AppMap server in debug message of "Stop remote recording" ([388e02a]())-  show properly formatted debug log message for "Stop remote recording" ([a764126]())

## [0.25.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.24.0...v0.25.0) (2023-01-19)


### Features

-  update @appland/components and @appland/diagrams to the latest versions ([d0e58b5]())

### Bug Fixes

-  don't access file name index on EDT ([8c6686f]())

## [0.24.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.23.0...v0.24.0) (2023-01-17)


### Features

-  add terms and conditions to description ([f879b5e]())

## [0.23.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.22.0...v0.23.0) (2023-01-11)


### Features

-  add a splitter between the list of AppMaps and the secondary panels (Runtime Analysis, etc.) to allow resizing of the tool window content ([82466c9]())-  implement tree view of "Runtime Analysis" panel ([723213e]())-  new webviews for "Findings Overview" and "Finding details" ([14f22ec]())-  send telemetry message for finding details view ([96915e1]())-  send telemetry messages for the findings overview editor ([cfec988]())

### Bug Fixes

-  add borders to minimum size of titles of collapsibles panels ([823c6aa]())-  handling of external links ([eb70e3a]())-  increase minimum height of "runtime analysis" panel to show all the message when the user is unauthenticated ([29b805a]())-  open AppMap centered on finding when clicking on an AppMap link in the "Finding details" view ([8336327]())-  render "Overview" with link attributes ([0541992]())-  reuse finding details webviews if the same list of findings is displayed ([d1b21d8]())-  show a more reasonable title for finding details webviews ([ef566c4]())-  sort finding nodes by file name ([dc5c1aa]())-  spelling in empty message of the runtime analysis panel ([1c6af9a]())

## [0.22.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.21.1...v0.22.0) (2023-01-05)


### Features

-  Enable jest support ([0e0d76b]())

## [0.21.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.21.0...v0.21.1) (2022-12-13)


### Bug Fixes

-  bundle appmap-java agent v1.15.4 ([27878f3]())

## [0.21.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.20.3...v0.21.0) (2022-11-29)


### Features

-  Implement "Generate OpenAPI" ([bd64a95]())-  send telemetry after successfully generating an OpenAPI file ([8ac3dbe]())

### Bug Fixes

-  avoid exception "Too many non-blocking read actions" ([f988ecf]())-  don't open a split editor ([f7ae26f]())-  properly enable the "Generate OpenAPI" step when the panel is shown the first time ([5ff6aff]())-  send telemetry message open_api:failure ([7815b2c]())

## [0.20.3](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.20.2...v0.20.3) (2022-11-22)


### Bug Fixes

-  Emit appmap:create events ([144f7f6]())-  Specify the authentication source as 'JetBrains' ([8db8f4a]())-  Upgrade AppMap agent to v1.15.2 ([3117a31]())

## [0.20.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.20.1...v0.20.2) (2022-11-21)


### Bug Fixes

-  ConcurrentModificationException opening a project ([813a7e9]())-  NPE when the project language could not be found ([7240aaa]())

## [0.20.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.20.0...v0.20.1) (2022-11-21)


### Bug Fixes

-  Use `/authn_provider` for authentication ([bbfea52]())

## [0.20.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.19.0...v0.20.0) (2022-11-18)


### Features

-  handle changes to the enableFindings settings without a restart ([1eaf822]())-  send analysis:enable and analysis:disable events ([2b8db9f]())-  send authentication:failed ([f74d381]())-  send authentication:success and authentication:sign_out ([c05aa1e]())-  send telemetry event analysis:cta_interaction ([516ec48]())-  support authentication against AppLand server ([dcddf42]())

### Bug Fixes

-  keep "Runtime Analysis" visible even when findings are not yet available ([dba3656]())-  labels of new auth actions ([6747b8a]())-  move refresh of CLI processes into background, fix notification when settings change ([199ed86]())-  only enable findings and scanner if both "isUserAuthenticated" and "isFindingsEnabled" are true ([31dd8ed]())

## [0.19.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.18.0...v0.19.0) (2022-10-26)


### Features

-  provide quick fix "Open in AppMap" for AppMap annotations in a text editor ([2c65f89]())

## [0.18.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.17.4...v0.18.0) (2022-10-24)


### Features

-  support execution of AppMap commands in the terminal ([d3b7973]())

## [0.17.4](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.17.3...v0.17.4) (2022-10-20)


### Bug Fixes

-  show success icon for "Runtime Analysis" even if only findings with unknown source files were found ([076856b]())

## [0.17.3](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.17.2...v0.17.3) (2022-10-20)


### Bug Fixes

-  Rename `appmap:record` to `appmap:create` ([bd481d9]())

## [0.17.2](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.17.1...v0.17.2) (2022-10-20)


### Bug Fixes

-  Add appland.appmap prefix to telemetry events ([2c80586]())

## [0.17.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.17.0...v0.17.1) (2022-10-19)


### Bug Fixes

-  telemetry when a Java run configuration is executed with the AppMap executor ([dd5f7c7]())

## [0.17.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.16.0...v0.17.0) (2022-10-19)


### Features

-  Support 2022.3 EAP ([5d46344]())

## [0.16.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.15.1...v0.16.0) (2022-10-14)


### Features

-  enable findings by default ([5b24355]())

## [0.15.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.15.0...v0.15.1) (2022-10-12)


### Reverts

-  Revert "ci: fix the publish command" ([4b1317c]())

## [0.15.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.14.1...v0.15.0) (2022-10-11)


### Features

-  add run configurations extensions for Java and Maven ([e2ebc21]())-  Allow to use an alternative Java 11 JDK to the Java integration test. AppMap is currently only compatible with Java <= 11 ([bdea1a5]())-  download appmap-java agent from Maven repository and bundle into the plugin zip ([505e42f]())-  integration test case to run a java program with the appmap agent ([bbb4ad3]())-  prefer the output directory of the build tool ([dacdf81]())-  show a link to configure the project JDK in the notification about an incompatible JDK ([43d6568]())-  validate that the JDK is supported by AppMap before executing the run configuration ([4881ba1]())

### Bug Fixes

-  add icon for the AppMap executor ([ad97895]())-  don't show a JDK settings link in 2021.3, because it's not supported everywhere ([fc2701a]())-  don't show a placeholder in the "Run with AppMap" tooltip on the taskbar icon ([6538d84]())-  gradle complains about wrong task dependencies ([b7497ae]())-  Tests executed via Maven were not recording tests ([846930e]())

## [0.14.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.14.0...v0.14.1) (2022-10-04)


### Bug Fixes

-  Update documentation URLs from appland.com -> appmap.io ([c100ec4]())

## [0.14.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.13.0...v0.14.0) (2022-09-27)


### Features

-  add new setting to store if an AppMap editor was opened ([0ab7ccc]())-  improvements to indexing and AppMap project metadata handling ([16ebc42]())-  update install guide icons when status changes, improve async tree model of AppMaps ([14cc8f0]())

### Bug Fixes

-  detect changes to appmap.yml files and refresh CLI services ([da59ecf]())-  refresh findings manager after deleting directory, which contains AppMap and findings data ([26bef42]())-  warning in log about output handling ([3b51c19]())

## [0.13.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.12.1...v0.13.0) (2022-09-23)


### Features

-  Add basic telemetry collection capabilities ([b49d5de]())-  Notify on AppMap open ([f694843]())-  Notify on first AppMap recording of the session ([36f3692]())-  Notify on install guide page view ([b00a15c]())-  Notify on plugin install ([2b4c45c]())

## [0.12.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.12.0...v0.12.1) (2022-09-20)

### Bug Fixes

-  Add a code object entry for external services ([2a342a4]())-  AppMap Java line's are 1-based ([05352db]())-  CLI download URL on Windows ([6a919c8]())-  make launching of CLI processes more robust ([b5d0e74]())-  project score of spring-petclinic and others was calculated incorrectly ([d930b31]())-  properly include AppMap files in directories excluded by the project setup ([3c2b6ba]())-  unittest framework wasn't properly detected ([3a89e76]())

## [0.12.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.11.0...v0.12.0) (2022-09-16)

### Features

-  implement install guide editor ([0d12a97]())-  remove code of old user-milestones panel and editor view ([f9b6a8d]())-  remove old user-milestones web application ([43894e5]())

### Bug Fixes

-  icon provider logic was wrong ([bbdbe89]())-  rename install guide steps to match VSCode ([46efc5e]())

## [0.11.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.10.0...v0.11.0) (2022-09-16)


### Features

-  indexing of classMap.json files to find sample queries and HTTP requests for the install-guide view ([6a8db2b]())-  support managing metadata for AppMap projects ([8412992]())

### Bug Fixes

-  don't break on IOOBE or other exceptions, which are possible with invalid JSON ([7429a55]())-  don't try to fetch class map items in dumb mode ([8abd5bc]())

## [0.10.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.9.0...v0.10.0) (2022-09-15)


### Features

-  add new tab to the problems view tool window, support AppMap findings ([aa0789c]())

## [0.9.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.8.1...v0.9.0) (2022-09-15)


### Features

-  add application setting to configure findings (off by default) ([2077dbb]())-  add services to control download and launching or AppMap CLI tools ([8819710]())-  parsing of appmap.yml files to retrieve the configured appmap directory ([bfc947f]())

## [0.8.1](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.8.0...v0.8.1) (2022-09-09)


### Bug Fixes

-  Make sure plugin.xml gets updated ([7019c8c]())

## [0.8.0](https://github.com/getappmap/appmap-intellij-plugin/compare/v0.7.10...v0.8.0) (2022-09-08)


### Features

* Various fixes ([006aac6](https://github.com/getappmap/appmap-intellij-plugin/commit/006aac623773817f59f38dd8f166c05fb96e1453))

## [0.7.10]
### Updated
- Compatibility with 2022.2 EAP

## [0.7.9]
- Update community links within the extension description

## [0.7.8]
### Changed
- Updated web components to the latest versions

## [0.7.7]
### Added
- Support 2022.1 EAP

## [0.7.6]
### Added
- Show an error page if the user attempts to load an AppMap that's too large.

## [0.7.5]
### Changed
- Express.js is now a supported web framework 

## [0.7.4]
### Changed
- appmap-js components v1.17.0

## [0.7.3]
### Changed
- Compatibility with 2021.3

## [0.7.2]
### Changed
- Project directory added to install command

## [0.7.1]
### Added
- Onboarding updates

## [0.7.0]
### Added
- New onboarding flow

### Changed
- AppMap diagrams updates

## [0.6.1]
### Added
-Support to open source files located in dependencies, e.g. from a sources jar file.

## [0.6.0]
### Added
- Uploading AppMaps to AppMap Cloud

### Changed
- Updated AppMap JavaScript components

## [0.5.1]
### Added
- Display a notification for AppMap JSON editors if the HTML application can't be displayed.
- New feature to upload AppMaps to a remote server.

### Changed
- Improved search in the AppMaps tool window

## [0.5.0]
### Added
- Install AppMap Agent view
- Links to AppMap documentation in AppMaps

## [0.4.1]
### Changed
- AppLand components updated
- IDE restart is not required when plugin is installed

## [0.4.0]
### Added
- AppMap remote recording controls. The AppMap tool window now displays actions to start and stop a remote recording.

## [0.3.1]
### Added
- Documentation link in the Help menu
- Quick guide now automatically opens for first-time users

## [0.3.0]
### Added
- AppMaps tool window for a quick navigation to all AppMaps in project folders
  
## [0.2.2]
### Changed
- Documentation update

## [0.2.1]
### Changed
- Documentation update

## [0.2.0]
### Changed
- First public release

## [0.1.0]
### Added
- Initial internal release
