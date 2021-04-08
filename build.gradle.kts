import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.intellij.tasks.PrepareSandboxTask

plugins {
    idea
    id("org.jetbrains.intellij") version "0.7.2"
}

repositories {
    mavenCentral()
}

intellij {
    version = "2021.1"
    downloadSources = true
    updateSinceUntilBuild = true
    instrumentCode = false
}

configure<JavaPluginConvention> {
    sourceCompatibility = VERSION_11
    targetCompatibility = VERSION_11
}

tasks {
    buildSearchableOptions.get().enabled = false

    buildPlugin {
        copyPluginAssets()
    }

    patchPluginXml {
        sinceBuild("211.0")
        untilBuild("211.*")
    }

    processTestResources {
        copyPluginAssets()
    }

    withType(PrepareSandboxTask::class.java).all {
        copyPluginAssets("${intellij.pluginName}/appmap")
    }
}

fun AbstractCopyTask.copyPluginAssets(targetDir: String = "appmap") {
    from("${project.rootDir}/appland") {
        into(targetDir)
        include("index.html")
        include("dist/**")
    }
}