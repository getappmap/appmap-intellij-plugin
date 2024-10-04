import com.adarshr.gradle.testlogger.theme.ThemeType
import de.undercouch.gradle.tasks.download.Download
import groovy.json.JsonSlurper
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.InstrumentCodeTask
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.22.0")
    }

    repositories {
        mavenCentral()
    }
}

plugins {
    idea
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.changelog") version "1.3.1"
    id("com.adarshr.test-logger") version "3.2.0"
    id("de.undercouch.download") version "5.6.0"

    kotlin("plugin.lombok") version "1.9.24"
}

val pluginVersionString = prop("pluginVersion")
val lombokVersion = prop("lombokVersion")
val ideVersion = prop("ideVersion")

group = "appland.appmap"
version = pluginVersionString

val platformVersion = when {
    // e.g. '2024.1'
    ideVersion.length == 6 -> ideVersion.replace(".", "").substring(2).toInt()
    // e.g. '243.16718.32'
    else -> ideVersion.substringBefore(".").toInt()
}

val isCI = System.getenv("CI") == "true"
val agentOutputPath = rootProject.layout.buildDirectory.asFile.get()
    .resolve("appmap-java-agent")
    .resolve("appmap-agent.jar")
val githubToken = System.getenv("GITHUB_TOKEN").takeUnless(String::isNullOrEmpty)

allprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.lombok")
        plugin("idea")
        plugin("com.adarshr.test-logger")

        plugin("org.jetbrains.intellij.platform.module")
    }

    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }

    val testOutput = configurations.create("testOutput")
    dependencies {
        intellijPlatform {
            intellijIdeaCommunity(ideVersion)
            // using "Bundled" to gain access to the Java plugin's test classes
            testFramework(TestFrameworkType.Bundled)

            // org.jetbrains.intellij.platform requires to bundledModules for 2024.2+
            if (platformVersion >= 242) {
                bundledModule("intellij.platform.collaborationTools")
                bundledModule("intellij.platform.vcs.impl")
            }
            // 2024.3 extracted JSON support into a plugin
            if (platformVersion >= 243) {
                bundledPlugin("com.intellij.modules.json")
            }
        }

        // added because org.jetbrains.intellij.platform resolves to an older version bundled with the SDK
        compileOnly("org.jetbrains:annotations:24.1.0")

        compileOnly("com.google.code.findbugs:jsr305:3.0.2")

        // Jackson JSON is missing from 2023.1+
        implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.2")
        implementation("org.yaml:snakeyaml:1.33")

        // https://mvnrepository.com/artifact/junit/junit
        testImplementation("junit:junit:4.13.2")

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

        // workaround for https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1663
        testImplementation("org.opentest4j:opentest4j:1.3.0")

        // Subproject test-integration must not get our additional test dependencies
        // because mockserver is conflicting with the SDK's bundled library versions.
        // See https://github.com/getappmap/appmap-intellij-plugin/pull/794.
        if (!project.name.startsWith("tests-")) {
            // https://mvnrepository.com/artifact/org.mock-server/mockserver-junit-rule
            testImplementation("org.mock-server:mockserver-junit-rule:5.15.0")
        }
    }

    intellijPlatform {
        instrumentCode = false
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

        processTestResources {
            dependsOn(":copyPluginAssets")
            from(rootProject.layout.buildDirectory.dir("appmap-assets")) {
                into("")
                include("**/*")
            }
        }

        named("verifyPluginProjectConfiguration") {
            dependsOn(":copyPluginAssets")
            onlyIf { project == rootProject }
        }

        withType<PrepareSandboxTask>().configureEach {
            dependsOn(":copyPluginAssets")
            from(rootProject.layout.buildDirectory.dir("appmap-assets")) {
                into(rootProject.name)
                include("**/*")
            }
        }

        withType<InstrumentCodeTask>().configureEach {
            onlyIf { false }
        }

        // Target to execute tests, which are incompatible with the AppMap agent.
        // Only tests with category "appland.WithoutAppMapAgent" are executed.
        val testWithoutAgent by intellijPlatformTesting.testIde.registering {
            type = IntelliJPlatformType.IntellijIdeaCommunity
            version = ideVersion

            task {
                useJUnit {
                    includeCategories("appland.WithoutAppMapAgent")
                }
            }
        }

        named("check") {
            dependsOn(testWithoutAgent.name)
        }

        withType<Test>().configureEach {
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
            val logFileName = "appmap-agent-${System.currentTimeMillis()}.log"

            // attach AppMap agent, but only if Gradle is online
            jvmArgs(
                "-javaagent:$agentOutputPath",
                "-Dappmap.config.file=${rootProject.file("appmap.yml")}",
                "-Dappmap.debug.file=${project.layout.buildDirectory.asFile.get().resolve(logFileName)}",
                "-Dappmap.output.directory=${rootProject.file("tmp/appmap")}"
            )
            systemProperty("appmap.test.withAgent", "true")

            useJUnit {
                excludeCategories("appland.WithoutAppMapAgent")
            }
        }

        withType<Zip>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

project(":") {
    apply {
        plugin("org.jetbrains.intellij.platform")
    }

    dependencies {
        intellijPlatform {
            pluginModule(implementation(project(":plugin-core")))
            pluginModule(implementation(project(":plugin-gradle")))
            pluginModule(implementation(project(":plugin-java")))
            pluginModule(implementation(project(":plugin-maven")))

            pluginVerifier()
            zipSigner()
        }
    }

    intellijPlatform {
        pluginConfiguration {
            version = pluginVersionString
            description.set(provider {
                file("${rootDir}/description.md").readText().renderMarkdown()
            })

            ideaVersion {
                sinceBuild.set(prop("sinceBuild"))
                untilBuild.set(prop("untilBuild"))
            }

            changeNotes.set(provider {
                when {
                    pluginVersionString.endsWith("-SNAPSHOT") -> changelog.getUnreleased().toHTML()
                    else -> changelog.get(pluginVersionString).toHTML()
                }
            })
        }

        pluginVerification {
            ides {
                ides(prop("ideVersionVerifier").split(","))
            }

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
    }


    changelog {
        path.set("${project.projectDir}/CHANGELOG.md")
    }

    tasks {
        buildPlugin {
            dependsOn(":copyPluginAssets")
            from(rootProject.layout.buildDirectory.dir("appmap-assets")) {
                into("")
                include("**/*")
            }
        }

        runIde {
            systemProperty("appmap.sandbox", "true")
            jvmArgs("-Xmx2048m")
        }

        verifyPlugin {
            mustRunAfter("check")
        }

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
        intellijPlatform {
            bundledPlugin("com.intellij.java")
            bundledPlugin("com.intellij.properties")
        }
    }
}

project(":plugin-gradle") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-java"))
        intellijPlatform {
            bundledPlugin("com.intellij.java")
            bundledPlugin("com.intellij.gradle")
            bundledPlugin("com.intellij.properties")
        }
    }
}

project(":plugin-maven") {
    dependencies {
        implementation(project(":plugin-core"))
        implementation(project(":plugin-java"))
        intellijPlatform {
            bundledPlugin("com.intellij.java")
            bundledPlugin("org.jetbrains.idea.maven")
            bundledPlugin("com.intellij.properties")
        }
    }
}

project(":tests-integration") {
    dependencies {
        implementation(project(":plugin-core"))
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