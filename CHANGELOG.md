AppMap Plugin Changelog

## [0.8.1](https://github.com/applandinc/appmap-intellij-plugin/compare/v0.8.0...v0.8.1) (2022-09-09)


### Bug Fixes

-  Make sure plugin.xml gets updated ([7019c8c]())

## [0.8.0](https://github.com/applandinc/appmap-intellij-plugin/compare/v0.7.10...v0.8.0) (2022-09-08)


### Features

* Various fixes ([006aac6](https://github.com/applandinc/appmap-intellij-plugin/commit/006aac623773817f59f38dd8f166c05fb96e1453))

## [0.8.0-6]
- Detect external changes to AppMap files and AppMap index data, e.g. by the install guide steps.
- Properly handle AppMap data in excluded directories like `target/appmap`.

## [0.8.0-5]
- Support to show the problems view tab (link "Open the PROBLEMS tab" in the JS application).
- Download CLI tools even if "enableFindings" is off. 
- Don't break findings when JSON parsing fails.
- Fix for `ArrayIndexOutOfBoundsException` during AppMap indexing.

## [0.8.0-4]
- Indexing of classMap.json files to provide sample HTTP requests and database queries for the install guide.
- Fixed names of install guide steps.
- Fixed warning about CLI background process listening.
- Bug fixes

## [0.8.0-3]
- Refresh table of AppMaps in install guide webview after changes to `.appmap.json` files on disk.
- Add link to page "Runtime Analysis".
- Fix to locate `appmap-findings.json` files in excluded parent folders, e.g. in `target/appmap/...`.
- Updated wording of Java and JavaScript language analyzers to match VSCode.
- Download CLI binaries to a temp location first to avoid execution of partially downloaded files at startup.

## [0.8.0-2]
- Check for `appmap.yml` before launching CLI processes.
- Remove old versions of download CLI binaries.
- Misc fixes

## [0.8.0-1]
- Support .gitignore and other VCS ignored files if supported by the IDE
- Disabled OpenAPI page

## [0.8.0]
### Added
- Support for AppLand scanner findings

### Changed
- Dropped support for 2021.1 and 2021.2

## [0.8.0-5]
- Support to show the problems view tab (link "Open the PROBLEMS tab" in the JS application).
- Download CLI tools even if "enableFindings" is off. 
- Don't break findings when JSON parsing fails.
- Fix for `ArrayIndexOutOfBoundsException` during AppMap indexing.

## [0.8.0-4]
- Indexing of classMap.json files to provide sample HTTP requests and database queries for the install guide.
- Fixed names of install guide steps.
- Fixed warning about CLI background process listening.
- Bug fixes

## [0.8.0-3]
- Refresh table of AppMaps in install guide webview after changes to `.appmap.json` files on disk.
- Add link to page "Runtime Analysis".
- Fix to locate `appmap-findings.json` files in excluded parent folders, e.g. in `target/appmap/...`.
- Updated wording of Java and JavaScript language analyzers to match VSCode.
- Download CLI binaries to a temp location first to avoid execution of partially downloaded files at startup.

## [0.8.0-2]
- Check for `appmap.yml` before launching CLI processes.
- Remove old versions of download CLI binaries.
- Misc fixes

## [0.8.0-1]
- Support .gitignore and other VCS ignored files if supported by the IDE
- Disabled OpenAPI page

## [0.8.0]
### Added
- Support for AppLand scanner findings

### Changed
- Dropped support for 2021.1 and 2021.2

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
