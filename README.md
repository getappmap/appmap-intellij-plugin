# AppLand for JetBrains IDEs

## System Requirements
**IntelliJ IDEA 2021.1 or later** is required to use this plugin.

Only installations, which use the bundled JetBrains Java runtime, support the JCEF engine for rendering.

## Build
Please make sure that a **Java JDK, version 11 to 15** is installed. Building the plugin is only supported with Java 11 or later.

```bash
./gradlew clean build
```

After executing these commands the plugin is available at `./build/distributions/intellij-appmap-<versin>.zip`. 

### Releases
The content of `plugin-description.md` is converted into HTML as part of the build process. The HTML content is displayed as description on the JetBrains marketplace and in plugin lists in the IDEs.

The content of `CHANGELOG.md` is converted into HTML as part of the build process and used as content for the plugin's change notes.

1. Optional: Update content of `description.md`.
1. Optional: Update section `[Unreleased]` in file `CHANGELOG.md`. This content will be used for the change notes.
1. Update the `pluginVersion` property in `gradle.properties`, e.g. change `pluginVersion=0.1.0-SNAPSHOT` to `pluginVersion=0.1.0`.
1. Build the plugin ZIP file
    ```bash
    ./gradlew clean build verifyPlugin
    ```
   The build output is located at `./build/distributions/intellij-appmap-<version>.zip`.
1. Optional: Install and test the plugin with one or more IDEs.
1. Upload the file `./build/distributions/intellij-appmap-<version>.zip` as an update on the JetBrains Marketplace.
1. Patch the changelog file:
    ```bash
   ./gradlew patchChangelog
    ```
1. Update the version in `gradle.properties` to include a SNAPSHOT version, e.g. change `pluginVersion=0.1.0` to `pluginVersion=0.2.0-SNAPSHOT`.

## Installation
After building the plugin you can drag & drop the file `./build/distributions/intellij-appmap.zip` onto the main window of IntelliJ to install the plugin. A restart of your IDE is required to use the plugin.

## Usage
A custom file editor is opened when a `.appmap.json` file is opened.
If JCEF is unsupported on your system, then an error message will be shown instead.

## Status
The current JCEF integration is only to test the capabilities of JCEF and if it provides the features to render an AppMap.

For a complete implementation the following needs to be verified and implemented:
- <s>Someone with deeper knowledge of the JS AppLand application should test it to identify possible issues.</s> DONE
- Reload the AppMap when the file on disk changes.
- Open a file inside the IDE when "Show source" inside the AppMap is clicked.
- Workaround JCEF limitations. Even JCEF of 2021.1 has its limitations, e.g. tiny fonts on certain Linux system, scrollbars which don't follow the theme, etc.
- Decide if IntelliJ 2020.3 should be supported. 2021.1 is the current major version. As far as I know previous major versions won't receive new builds of JCEF as it's bundled into the Java JRE.  
- Test with all of Windows, Linux, macOS Intel, macOS M1 and each of IntelliJ Ultimate, IntelliJ Community.   
- Theme handling. If possible, the theme of the AppMap should follow IntelliJ's theme (light / dark)
- ...

## MVP Goals
- Open .json.appmap files in the project FS in the AppMap viewer
- Detect and reload AppMaps when files get changed on disk
- Open source files referenced from AppMaps in right split [(see screenshot)](https://user-images.githubusercontent.com/42008542/114739490-ec0c4300-9d16-11eb-86f7-1eb0d3a81926.png)
- "Open the most recently modified AppMap" action
- Show error message when JCEF not available
- Dark theme
- Tested on Windows, macOS, Linux with the JDK shipped with the IDE
- Address rendering issues on Linux
- Version: 2021.1
- Marketplace plugin description and docs
- Published in JetBrains plugin repo so the plugin is easy to find and install from the IDE and online marketplace

### not critical for MVP
- light theme
- optionally open .appmap.json files in a text editor

## Screenshot

[Open screenshot](./appmap-intellij.png)

<img alt="Screenshot of AppLand with IntelliJ" src="appmap-intellij.png" width="100%">
