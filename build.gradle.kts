import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.changelog.closure
import org.jetbrains.changelog.date
import org.jetbrains.intellij.tasks.PrepareSandboxTask

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.17.1")
    }
}

plugins {
    idea
    id("org.jetbrains.intellij") version "0.7.3"
    id("org.jetbrains.changelog") version "1.1.1"
}

val pluginVersion = prop("pluginVersion")
group = "appland.appmap"
version = pluginVersion

repositories {
    mavenCentral()
}

intellij {
    version = prop("ideVersion")
    downloadSources = true
    updateSinceUntilBuild = true
    instrumentCode = false
}

changelog {
    version = pluginVersion
    path = "${project.projectDir}/CHANGELOG.md"
    header = closure { "[$version]" }
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Changes")
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
        sinceBuild(prop("sinceBuild"))
        untilBuild(prop("untilBuild"))
        pluginDescription(file("${rootDir}/description.md").readText().renderMarkdown())

        changeNotes(closure {
            if (pluginVersion.endsWith("-SNAPSHOT")) {
                changelog.getUnreleased().toHTML()
            } else {
                changelog.get(pluginVersion).toHTML()
            }
        })
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

// https://github.com/commonmark/commonmark-java
fun String.renderMarkdown(): String {
    val parser = Parser.builder().build()
    val document = parser.parse(this)
    val renderer = HtmlRenderer.builder().build()
    return renderer.render(document).trim()
}

fun prop(name: String): String {
    return extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")
}