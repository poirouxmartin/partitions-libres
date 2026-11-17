import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// arm64-v8a for phones, x86_64 for the emulator; -PvrvAbis=x86_64 for faster local builds.
val vrvAbis = (findProperty("vrvAbis") as String? ?: "arm64-v8a,x86_64").split(",").map { it.trim() }
val updateRepo = findProperty("updateRepo") as String? ?: "poirouxmartin/partitions-libres"

// Release key lives outside the repo; losing it means users can no longer update in place.
val signingFile = file(
    findProperty("releaseSigning") as String?
        ?: "${System.getProperty("user.home")}/.android/keys/partitions-libres/signing.properties",
)
val releaseSigning = Properties().takeIf { signingFile.isFile }?.apply { signingFile.inputStream().use(::load) }

android {
    namespace = "com.musique.partitions"
    compileSdk = 36
    // Same NDK as :render, so packaging can strip libverovio.so (unstripped, the APK is ~250 MB).
    ndkVersion = "27.1.12297006"

    defaultConfig {
        applicationId = "com.musique.partitions"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += vrvAbis }
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
    }

    signingConfigs {
        if (releaseSigning != null) create("release") {
            storeFile = signingFile.resolveSibling(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseSigning != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("No release key at $signingFile: release APK signed with the debug key, do not publish it.")
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":catalog"))
    implementation(project(":render"))

    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
