plugins {
    java
}

dependencies {
    implementation(project(":modules:writing-domain"))
    implementation(libs.slf4j.api)
}
