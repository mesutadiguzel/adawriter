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
            "testImplementation"(rootProject.libs.assertj.core)
            "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
        }
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs formatting checks and all tests with coverage"
    dependsOn(
        subprojects.mapNotNull { sub ->
            sub.tasks.findByName("spotlessCheck")?.let { sub.tasks.named("spotlessCheck") }
        }
    )
    dependsOn(
        subprojects.mapNotNull { sub ->
            sub.tasks.findByName("test")?.let { sub.tasks.named("test") }
        }
    )
    dependsOn(
        subprojects.mapNotNull { sub ->
            sub.tasks.findByName("jacocoTestReport")?.let { sub.tasks.named("jacocoTestReport") }
        }
    )
}
