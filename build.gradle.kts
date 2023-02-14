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
    id("org.jetbrains.intellij") version "1.13.0"
    id("org.jetbrains.changelog") version "1.3.1"
}

val pluginVersion = prop("pluginVersion")
val lombokVersion = prop("lombokVersion")
val appMapJvmAgentVersion = prop("appMapJvmAgent")

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

    val testOutput = configurations.create("testOutput")
    dependencies {
        // for compatibility with IntelliJ Ultimate, IU-2021.3.3 doesn't include this for unknown reasons
        compileOnly("com.google.code.findbugs:jsr305:3.0.2")

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
            // to allow tests to access the custom Java 11 JDK
            systemProperties["NO_FS_ROOTS_ACCESS_CHECK"] = true

            if (isCI) {
                testLogging {
                    setEvents(listOf("failed", "standardError"))
                }
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
    val appMapJavaAgent = configurations.create("appMapJavaAgent")
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-gradle"))
        implementation(project(":plugin-java"))
        implementation(project(":plugin-maven"))

        // to copy the appmap-java jar file into the plugin zip
        appMapJavaAgent("com.appland:appmap-agent:$appMapJvmAgentVersion")
    }

    changelog {
        path.set("${project.projectDir}/CHANGELOG.md")
    }

    tasks {
        task<Copy>("copyPluginAssets") {
            inputs.files("${project.rootDir}/appland")
            inputs.files("${project.rootDir}/appland-install-guide")
            inputs.files(appMapJavaAgent)

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
            from(appMapJavaAgent) {
                into("appmap-assets/appmap-agents")
                rename {
                    when (it.startsWith("appmap-agent-")) {
                        true -> "appmap-agent.jar"
                        false -> name
                    }
                }
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
        }

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