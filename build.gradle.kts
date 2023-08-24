import com.adarshr.gradle.testlogger.theme.ThemeType
import de.undercouch.gradle.tasks.download.Download
import groovy.json.JsonSlurper
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.tasks.testing.logging.TestLogEvent
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
    id("org.jetbrains.intellij") version "1.13.2"
    id("org.jetbrains.changelog") version "1.3.1"
    id("com.adarshr.test-logger") version "3.2.0"
    id("de.undercouch.download") version "5.4.0"
}

val pluginVersion = prop("pluginVersion")
val lombokVersion = prop("lombokVersion")

group = "appland.appmap"
version = pluginVersion

val isCI = System.getenv("CI") == "true"
val agentOutputPath = rootProject.buildDir.resolve("appmap-agent.jar")

allprojects {
    repositories {
        mavenCentral()
        maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    }

    apply {
        plugin("idea")
        plugin("org.jetbrains.intellij")
        plugin("com.adarshr.test-logger")
    }

    val testOutput = configurations.create("testOutput")
    dependencies {
        // for compatibility with IntelliJ Ultimate, IU-2021.3.3 doesn't include this for unknown reasons
        compileOnly("com.google.code.findbugs:jsr305:3.0.2")

        // Jackson JSON is missing from 2023.1+
        implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2")

        // http://wiremock.org, Apache 2 license
        testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.1")

        // Project Lombok, only for compilation
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")

        // share test classes of plugin-core with other Gradle modules
        testOutput(sourceSets.getByName("test").output)
        if (project.name != "plugin-core") {
            testImplementation(project(":plugin-core", "testOutput"))
        }
    }

    intellij {
        version.set(prop("ideVersion"))
        downloadSources.set(!isCI)
        updateSinceUntilBuild.set(true)
        instrumentCode.set(true)
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = VERSION_11
        targetCompatibility = VERSION_11
    }

    tasks {
        buildSearchableOptions.get().enabled = false

        buildPlugin {
            dependsOn(":copyPluginAssets")
            from("${rootProject.buildDir}/appmap-assets") {
                into("")
                include("**/*")
            }
        }

        processTestResources {
            dependsOn(":copyPluginAssets")
            from("${rootProject.buildDir}/appmap-assets") {
                into("")
                include("**/*")
            }
        }

        runIde {
            onlyIf { this.project == rootProject }
            systemProperty("appmap.sandbox", "true")

        }

        verifyPlugin {
            dependsOn(":copyPluginAssets")
            onlyIf { this.project == rootProject }
        }

        withType<PrepareSandboxTask> {
            dependsOn(":copyPluginAssets")
            from("${rootProject.buildDir}/appmap-assets") {
                into(intellij.pluginName.get())
                include("**/*")
            }
        }

        withType<Test> {
            systemProperty("idea.test.execution.policy", "appland.AppLandTestExecutionPolicy")
            systemProperty("appland.testDataPath", rootProject.rootDir.resolve("src/test/data").path)
            if (isCI) {
                systemProperty("appland.github_token", System.getenv("GITHUB_TOKEN"))
            }

            // to allow tests to access the custom Java 11 JDK
            systemProperties["NO_FS_ROOTS_ACCESS_CHECK"] = true

            // always execute tests, don't skip by Gradle's up-to-date checks in development
            outputs.upToDateWhen { false }

            // attach AppMap agent, but only if Gradle is online
            if (!project.gradle.startParameter.isOffline) {
                dependsOn(":downloadAppMapAgent")
                jvmArgs("-javaagent:$agentOutputPath",
                        "-Dappmap.config.file=${rootProject.file("appmap.yml")}",
                        "-Dappmap.output.directory=${rootProject.buildDir.resolve("appmap")}")
                systemProperty("appmap.test.withAgent", "true")
            }

            // logging setup
            testLogging {
                setEvents(listOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR))
            }

            testlogger {
                theme = ThemeType.PLAIN
            }
        }

        withType<RunPluginVerifierTask> {
            onlyIf { this.project == rootProject }
            ideVersions.set(prop("ideVersionVerifier").split(","))
        }

        withType<Zip> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

project(":") {
    dependencies {
        implementation(project(":plugin-core", "instrumentedJar"))
        implementation(project(":plugin-gradle", "instrumentedJar"))
        implementation(project(":plugin-java", "instrumentedJar"))
        implementation(project(":plugin-maven", "instrumentedJar"))
    }

    changelog {
        path.set("${project.projectDir}/CHANGELOG.md")
    }

    tasks {
        task<Copy>("copyPluginAssets") {
            inputs.dir("${project.rootDir}/appland")
            inputs.dir("${project.rootDir}/appland-install-guide")
            inputs.dir("${project.rootDir}/appland-findings")
            inputs.dir("${project.rootDir}/appland-signin")

            destinationDir = project.buildDir
            from("${project.rootDir}/appland") {
                into("appmap-assets/appmap")
                include("index.html")
                include("dist/**")
            }
            from("${project.rootDir}/appland-install-guide") {
                into("appmap-assets/appland-install-guide")
                include("index.html")
                include("dist/**")
            }
            from("${project.rootDir}/appland-findings") {
                into("appmap-assets/appland-findings")
                include("index.html")
                include("dist/**")
            }
            from("${project.rootDir}/appland-signin") {
                into("appmap-assets/appland-signin")
                include("index.html")
                include("dist/**")
            }

            processResources {
                dependsOn("copyPluginAssets")
            }

            instrumentCode {
                dependsOn("copyPluginAssets")
            }

            jar {
                dependsOn("copyPluginAssets")
            }

            instrumentedJar {
                dependsOn("copyPluginAssets")
            }
        }

        patchPluginXml {
            dependsOn(":copyPluginAssets")

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

        @Suppress("UNCHECKED_CAST")
        task<Download>("downloadAppMapAgent") {
            src("https://api.github.com/repos/getappmap/appmap-java/releases/latest")
            dest(project.buildDir.resolve("appmap-java.json"))
            overwrite(true)
            quiet(true)

            doLast {
                val json = JsonSlurper().parseText(dest.readText()) as Map<*, *>
                val jarAsset = (json["assets"] as List<Map<*, *>>)
                        .filter { (it["name"] as? String)?.endsWith(".jar") == true }
                        .map { it["browser_download_url"]!! }
                        .firstOrNull()

                download.run {
                    src(jarAsset)
                    dest(agentOutputPath)
                    overwrite(false)
                }
            }
        }
    }
}

project(":plugin-java") {
    dependencies {
        implementation(project(":plugin-core", "instrumentedJar"))
    }

    intellij {
        plugins.set(listOf("java"))
    }
}

project(":plugin-gradle") {
    dependencies {
        implementation(project(":plugin-core", "instrumentedJar"))
        implementation(project(":plugin-java", "instrumentedJar"))
    }

    intellij {
        plugins.set(listOf("java", "gradle"))
    }
}

project(":plugin-maven") {
    dependencies {
        implementation(project(":plugin-core", "instrumentedJar"))
        implementation(project(":plugin-java", "instrumentedJar"))
    }

    intellij {
        plugins.set(listOf("java", "maven"))
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