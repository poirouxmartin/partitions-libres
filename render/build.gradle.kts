plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val vrvDir = project.file("third_party/verovio")
val vrvAbis = (findProperty("vrvAbis") as String? ?: "arm64-v8a,x86_64").split(",").map { it.trim() }

// Verovio's runtime resources (SMuFL font metrics, text fonts) are shipped as assets
// under verovio-data/ and unpacked to filesDir by VerovioResources.
val vrvAssets = layout.buildDirectory.dir("generated/vrv-assets")
val copyVerovioData by tasks.registering(Sync::class) {
    from(File(vrvDir, "data")) { into("verovio-data") }
    into(vrvAssets)
}

android {
    namespace = "com.musique.render"
    compileSdk = 36
    ndkVersion = "27.1.12297006"

    defaultConfig {
        minSdk = 24 // the app floor is set per flavor in :app
        ndk { abiFilters += vrvAbis }
        externalNativeBuild {
            cmake {
                // Native code is always built optimised, even in debug.
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DVRV_DIR=${vrvDir.absolutePath.replace('\\', '/')}")
                cppFlags += "-std=c++20"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets["main"].assets.srcDir(vrvAssets)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

tasks.named("preBuild") { dependsOn(copyVerovioData) }

dependencies {
    // SVG -> android.graphics.Picture (Apache-2.0); exposed because SvgPages returns Picture/uses its types.
    api("com.caverock:androidsvg-aar:1.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}
