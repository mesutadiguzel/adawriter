plugins {
    java
}

dependencies {
    implementation(project(":modules:writing-domain"))
    implementation(project(":modules:writing-application"))
    implementation(project(":modules:privacy-domain"))
    implementation(project(":modules:privacy-application"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)

    testImplementation(project(":modules:writing-adapter-ai"))
}
