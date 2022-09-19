pluginManagement {
    repositories {
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "intellij-appmap"

include("plugin-core")
include("plugin-gradle")
include("plugin-java")
include("plugin-maven")