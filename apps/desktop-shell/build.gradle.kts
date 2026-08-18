plugins {
    java
    application
}

application {
    mainClass.set("com.adawriter.desktop.DesktopShellMain")
}

dependencies {
    implementation(project(":modules:writing-domain"))
    implementation(project(":modules:writing-application"))
    implementation(project(":modules:writing-adapter-ai"))
    implementation(project(":modules:writing-adapter-rest"))
    implementation(project(":modules:privacy-domain"))
    implementation(project(":modules:privacy-application"))
    implementation(libs.logback.classic)
}

tasks.named<Test>("test") {
    // Tray unit tests use fakes; keep CI/Linux runners headless-safe.
    systemProperty("java.awt.headless", "true")
}
