plugins {
    java
    application
}

application {
    mainClass.set("com.adawriter.agent.DesktopAgentMain")
}

dependencies {
    implementation(project(":modules:writing-domain"))
    implementation(project(":modules:writing-application"))
    implementation(project(":modules:writing-adapter-ai"))
    implementation(project(":modules:writing-adapter-rest"))
    implementation(libs.logback.classic)
}
