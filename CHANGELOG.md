AppMap Plugin Changelog

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
