pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "adawriter"

include(
    "modules:writing-domain",
    "modules:writing-application",
    "modules:writing-adapter-ai",
    "modules:writing-adapter-rest",
    "modules:privacy-domain",
    "modules:privacy-application",
    "apps:desktop-agent",
    "apps:desktop-shell",
    "mobile:keyboard"
)
