AppMap Plugin Changelog

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
