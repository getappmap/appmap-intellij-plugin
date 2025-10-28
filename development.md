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