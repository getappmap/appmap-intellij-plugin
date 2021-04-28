import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.intellij.tasks.PrepareSandboxTask

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.17.1")
    }
}

plugins {
    idea
    id("org.jetbrains.intellij") version "0.7.3"
}

group = "appland.appmap"
version = prop("pluginVersion")

repositories {
    mavenCentral()
}

intellij {
    version = prop("ideVersion")
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
        sinceBuild(prop("sinceBuild"))
        untilBuild(prop("untilBuild"))
        pluginDescription(file("${rootDir}/plugin-description.md").readText().renderMarkdown())
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