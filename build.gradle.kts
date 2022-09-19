import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.17.2")
    }

    repositories {
        mavenCentral()
        maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    }
}

plugins {
    idea
    id("org.jetbrains.intellij") version "1.8.1"
    id("org.jetbrains.changelog") version "1.3.1"
}

val pluginVersion = prop("pluginVersion")
val lombokVersion = prop("lombokVersion")

group = "appland.appmap"
version = pluginVersion

val isCI = System.getenv("CI") == "true"

allprojects {
    repositories {
        mavenCentral()
        maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    }

    apply {
        plugin("idea")
        plugin("org.jetbrains.intellij")
    }

    dependencies {
        // http://wiremock.org, Apache 2 license
        testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.1")

        // Project Lombok, only for compilation
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    }

    intellij {
        version.set(prop("ideVersion"))
        downloadSources.set(!isCI)
        updateSinceUntilBuild.set(true)
        instrumentCode.set(true)
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = VERSION_11
        targetCompatibility = VERSION_11
    }

    tasks {
        buildSearchableOptions.get().enabled = false

        buildPlugin {
            copyPluginAssets("")
        }

        processTestResources {
            copyPluginAssets("")
        }
    }
}

changelog {
    path.set("${project.projectDir}/CHANGELOG.md")
}

tasks {
    patchPluginXml {
        sinceBuild.set(prop("sinceBuild"))
        untilBuild.set(prop("untilBuild"))
        pluginDescription.set(file("${rootDir}/description.md").readText().renderMarkdown())

        changeNotes.set(provider {
            if (pluginVersion.endsWith("-SNAPSHOT")) {
                changelog.getUnreleased().toHTML()
            } else {
                changelog.get(pluginVersion).toHTML()
            }
        })
    }

    withType<RunPluginVerifierTask> {
        onlyIf { this.project == rootProject }
        ideVersions.set(prop("ideVersionVerifier").split(","))
    }

    withType<PrepareSandboxTask> {
        copyPluginAssets(intellij.pluginName.get())
    }

    runIde {
        onlyIf { this.project == rootProject }
        systemProperty("appmap.sandbox", "true")
    }

    withType<Test> {
        systemProperty("idea.test.execution.policy", "appland.AppLandTestExecutionPolicy")
        systemProperty("appland.testDataPath", file("src/test/data").path)
    }

    withType<Zip> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

project(":") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-gradle"))
        implementation(project(":plugin-java"))
        implementation(project(":plugin-maven"))
    }
}

project(":plugin-java") {
    dependencies {
        implementation(project(":plugin-core"))
    }

    intellij {
        plugins.set(listOf("java"))
    }
}

project(":plugin-gradle") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-java"))
    }

    intellij {
        plugins.set(listOf("java", "gradle"))
    }
}

project(":plugin-maven") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-java"))
    }

    intellij {
        plugins.set(listOf("java", "maven"))
    }
}

fun AbstractCopyTask.copyPluginAssets(rootDir: String) {
    val rootPath = if (rootDir.isEmpty()) "" else "${rootDir.removeSuffix("/")}/"
    from("${project.rootDir}/appland") {
        into("${rootPath}appmap")
        include("index.html")
        include("dist/**")
    }
    from("${project.rootDir}/appland-install-guide") {
        into("${rootPath}appland-install-guide")
        include("index.html")
        include("dist/**")
    }
    from("${project.rootDir}/appmap-agents") {
        into("${rootPath}appmap-agents")
        include("*.jar")
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