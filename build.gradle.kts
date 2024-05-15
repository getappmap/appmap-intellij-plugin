import com.adarshr.gradle.testlogger.theme.ThemeType
import de.undercouch.gradle.tasks.download.Download
import groovy.json.JsonSlurper
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.22.0")
    }

    repositories {
        mavenCentral()
        maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    }
}

plugins {
    idea
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
    id("org.jetbrains.changelog") version "1.3.1"
    id("com.adarshr.test-logger") version "3.2.0"
    id("de.undercouch.download") version "5.6.0"

    kotlin("plugin.lombok") version "1.9.24"
}

val pluginVersion = prop("pluginVersion")
val lombokVersion = prop("lombokVersion")

group = "appland.appmap"
version = pluginVersion

val isCI = System.getenv("CI") == "true"
val agentOutputPath = rootProject.layout.buildDirectory.asFile.get().resolve("appmap-java-agent").resolve("appmap-agent.jar")
val githubToken = System.getenv("GITHUB_TOKEN").takeUnless { it.isNullOrEmpty() }

allprojects {
    repositories {
        mavenCentral()
        maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.lombok")
        plugin("idea")
        plugin("org.jetbrains.intellij")
        plugin("com.adarshr.test-logger")
    }

    val testOutput = configurations.create("testOutput")
    dependencies {
        compileOnly("com.google.code.findbugs:jsr305:3.0.2")

        // Jackson JSON is missing from 2023.1+
        implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2")
        implementation("org.yaml:snakeyaml:1.33")

        // http://wiremock.org, Apache 2 license
        testImplementation("org.wiremock:wiremock:3.5.4")

        // Project Lombok, only for compilation
        compileOnly("org.projectlombok:lombok:$lombokVersion")
        annotationProcessor("org.projectlombok:lombok:$lombokVersion")

        // share test classes of plugin-core with other Gradle modules
        testOutput(sourceSets.getByName("test").output)
        if (project.name != "plugin-core") {
            testImplementation(project(":plugin-core", "testOutput"))
        }

        // https://mvnrepository.com/artifact/org.mock-server/mockserver-netty
        testImplementation("org.mock-server:mockserver-netty:5.15.0")
        // https://mvnrepository.com/artifact/org.mock-server/mockserver-junit-rule
        testImplementation("org.mock-server:mockserver-junit-rule:5.15.0")
    }

    intellij {
        version.set(prop("ideVersion"))
        downloadSources.set(!isCI)
        updateSinceUntilBuild.set(true)
        instrumentCode.set(false)
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "17"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "17"
        }

        buildSearchableOptions.get().enabled = false

        buildPlugin {
            dependsOn(":copyPluginAssets")
            from(rootProject.layout.buildDirectory.dir("appmap-assets")) {
                into("")
                include("**/*")
            }
        }

        processTestResources {
            dependsOn(":copyPluginAssets")
            from(rootProject.layout.buildDirectory.dir("appmap-assets")) {
                into("")
                include("**/*")
            }
        }

        runIde {
            onlyIf { this.project == rootProject }
            systemProperty("appmap.sandbox", "true")
            jvmArgs("-Xmx2048m")
        }

        verifyPlugin {
            dependsOn(":copyPluginAssets")
            onlyIf { this.project == rootProject }
        }

        withType<PrepareSandboxTask> {
            dependsOn(":copyPluginAssets")
            from(rootProject.layout.buildDirectory.dir("appmap-assets")) {
                into(intellij.pluginName.get())
                include("**/*")
            }
        }

        // Target to execute tests, which are incompatible with the AppMap agent.
        // Only tests with category "appland.WithoutAppMapAgent" are executed.
        create<Test>("testWithoutAgent") {
            useJUnit {
                includeCategories("appland.WithoutAppMapAgent")
            }
        }

        named("check") {
            dependsOn(named("testWithoutAgent"))
        }

        withType<Test> {
            // all our tests need the jar, even if the agent is disabled
            dependsOn(":downloadAppMapAgent")

            systemProperty("idea.test.execution.policy", "appland.AppLandTestExecutionPolicy")
            systemProperty("appland.testDataPath", rootProject.rootDir.resolve("src/test/data").path)
            if (isCI && githubToken != null) {
                systemProperty("appland.github_token", githubToken)
            }

            // to allow tests to access the custom Java 11 JDK
            systemProperties["NO_FS_ROOTS_ACCESS_CHECK"] = true

            // always execute tests, don't skip by Gradle's up-to-date checks in development
            outputs.upToDateWhen { false }

            // logging setup
            testLogging {
                setEvents(listOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR))
            }

            testlogger {
                theme = ThemeType.PLAIN
            }
        }

        // only run the default test target with the AppMap agent
        named<Test>("test") {
            // attach AppMap agent, but only if Gradle is online
            jvmArgs(
                "-javaagent:$agentOutputPath",
                "-Dappmap.config.file=${rootProject.file("appmap.yml")}",
                "-Dappmap.debug.file=${project.layout.buildDirectory.asFile.get().resolve("appmap-agent-${System.currentTimeMillis()}.log")}",
                "-Dappmap.output.directory=${rootProject.file("tmp/appmap")}"
            )
            systemProperty("appmap.test.withAgent", "true")

            useJUnit {
                excludeCategories("appland.WithoutAppMapAgent")
            }
        }

        withType<RunPluginVerifierTask> {
            onlyIf { this.project == rootProject }
            mustRunAfter("check")

            // 1.365 is broken,
            // remove this version as soon as https://youtrack.jetbrains.com/issue/MP-6438 is fixed.
            verifierVersion.set("1.364")
            ideVersions.set(prop("ideVersionVerifier").split(","))
            failureLevel.set(
                listOf(
                    FailureLevel.INTERNAL_API_USAGES,
                    FailureLevel.COMPATIBILITY_PROBLEMS,
                    FailureLevel.OVERRIDE_ONLY_API_USAGES,
                    FailureLevel.NON_EXTENDABLE_API_USAGES,
                    FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
                )
            )
        }

        withType<Zip> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

project(":") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-gradle"))
        implementation(project(":plugin-java"))
        implementation(project(":plugin-maven"))
    }

    changelog {
        path.set("${project.projectDir}/CHANGELOG.md")
    }

    tasks {
        task<Copy>("copyPluginAssets") {
            dependsOn(":downloadAppMapAgent")

            inputs.file("${project.rootDir}/NOTICE.txt")
            inputs.file(agentOutputPath)
            inputs.dir("${project.rootDir}/appland")
            inputs.dir("${project.rootDir}/appland-install-guide")
            inputs.dir("${project.rootDir}/appland-findings")
            inputs.dir("${project.rootDir}/appland-signin")

            destinationDir = project.layout.buildDirectory.asFile.get()
            from(project.rootDir) {
                into("appmap-assets")
                include("NOTICE.txt")
            }
            from(agentOutputPath.parentFile) {
                into("appmap-assets/appmap-java-agent")
                include("*.jar")
            }
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
            from("${project.rootDir}/appland-navie") {
                into("appmap-assets/appland-navie")
                include("index.html")
                include("dist/**")
            }

            processResources {
                dependsOn("copyPluginAssets")
            }

            jar {
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
            dest(project.layout.buildDirectory.file("appmap-java.json"))
            overwrite(true)
            quiet(true)
            if (isCI && githubToken != null) {
                header("Authorization", "Bearer $githubToken")
            }

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
                    if (isCI && githubToken != null) {
                        header("Authorization", "Bearer $githubToken")
                    }
                }
            }
        }
    }
}

project(":plugin-java") {
    dependencies {
        implementation(project(":plugin-core"))
    }

    intellij {
        plugins.set(listOf("java", "com.intellij.properties"))
    }
}

project(":plugin-gradle") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-java"))
    }

    intellij {
        plugins.set(listOf("java", "gradle", "com.intellij.properties"))
    }
}

project(":plugin-maven") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-java"))
    }

    intellij {
        plugins.set(listOf("java", "maven", "com.intellij.properties"))
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