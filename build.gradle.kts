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
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
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

    jacoco {
        toolVersion = "0.8.13"
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    spotless {
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
        val testImplementation by configurations
        val testRuntimeOnly by configurations

        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.assertj.core)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs formatting checks and all tests with coverage"
    dependsOn(subprojects.map { it.tasks.named("spotlessCheck") })
    dependsOn(subprojects.map { it.tasks.named("test") })
    dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })
}
