plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.adawriter.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adawriter.keyboard"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":modules:writing-domain"))
    implementation(project(":modules:writing-application"))
    implementation(project(":modules:writing-adapter-ai"))
    implementation(project(":modules:privacy-domain"))
    implementation(project(":modules:privacy-application"))
    implementation(libs.slf4j.api)
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
