pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }

    val platformVersion = (extra["platformVersion"] as String).toInt()
    plugins {
        val kotlinVersion = when {
            platformVersion == 251 -> "2.1.0"
            else -> "1.9.24"
        }
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.lombok") version kotlinVersion
    }
}

rootProject.name = "intellij-appmap"

include("plugin-core")
include("plugin-copilot")
include("plugin-gradle")
include("plugin-java")
include("plugin-maven")

// module to tests without additional test dependencies
include("tests-integration")