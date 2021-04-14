# App.Land for JetBrains IDEs
**Note: this is an experimental plugin to verify the technical feasibility**

## System Requirements
**IntelliJ IDEA 2021.1** is currently required to use this experimental plugin.

Only installations, which use the bundled JetBrains Java runtime, support the JCEF engine for rendering.

## Build
Please make sure that a **Java JDK, version 11 to 15** is installed. Building the plugin is only supported with Java 11 or later.

```bash
./gradlew clean build
```

After executing these commands the plugin is available at `./build/distributions/intellij-appmap.zip`. 

## Installation
After building the plugin you can drag & drop the file  `./build/distributions/intellij-appmap.zip` onto the main window of IntelliJ to install the plugin. A restart of your IDE is required to use the plugin.

## Usage
The plugin adds a new action to open a sample AppMap inside of IntelliJ. This action is not visible in any menu (yet).

The easiest way to invoke it is:
1. Invoke `Search everywhere` by pressing `Shift` twice
1. Enter `AppLand: Open JCEF page`
1. Press `Enter` to execute the action

Now a new tab should open, which displays a sample AppMap.
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
- Open source files referenced from AppMaps in split tab/window
- "Open the most recently modified AppMap" action
- Show error message when JCEF not available
- Dark theme
- Tested on Windows, macOS, Linux with the JDK shipped with the IDE
- Address rendering issues on Linux
- Version: 2021.3
- Marketplace plugin description and docs
- Published in JetBrains plugin repo so the plugin is easy to find and install from the IDE and online marketplace

### not critical for MVP
- light theme
- optionally open .appmap.json files in a text editor

## Screenshot

[Open screenshot](./appmap-intellij.png)

<img alt="Screenshot of AppLand with IntelliJ" src="appmap-intellij.png" width="100%">
