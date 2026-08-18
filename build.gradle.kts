plugins {
    java
    jacoco
    alias(libs.plugins.spotless)
}

allprojects {
    group = "com.adawriter"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    repositories {
        google()
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        apply(plugin = "jacoco")
        apply(plugin = "com.diffplug.spotless")

        the<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(
                listOf(
                    "-parameters",
                    "-Xlint:all",
                    "-Xlint:-processing",
                    "-Werror"
                )
            )
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("failed", "skipped")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }

        the<JacocoPluginExtension>().toolVersion = "0.8.13"

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                target("src/*/java/**/*.java")
                removeUnusedImports()
                trimTrailingWhitespace()
                endWithNewline()
                palantirJavaFormat("2.58.0")
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint()
            }
        }

        dependencies {
            "testImplementation"(rootProject.libs.junit.jupiter)
            "testImplementation"(rootProject.libs.junit.jupiter.params)
            "testImplementation"(rootProject.libs.assertj.core)
            "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
        }
    }
}

tasks.register("desktopQualityCheck") {
    group = "verification"
    description = "Desktop-focused quality gates (excludes mobile)"
    val desktopProjects = subprojects.filter { !it.path.startsWith(":mobile") }
    dependsOn(desktopProjects.mapNotNull { it.tasks.findByName("spotlessCheck")?.let { t -> it.tasks.named("spotlessCheck") } })
    dependsOn(desktopProjects.mapNotNull { it.tasks.findByName("test")?.let { t -> it.tasks.named("test") } })
    dependsOn(desktopProjects.mapNotNull { it.tasks.findByName("jacocoTestReport")?.let { t -> it.tasks.named("jacocoTestReport") } })
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Desktop-focused quality gates (mobile deferred)"
    dependsOn("desktopQualityCheck")
}
