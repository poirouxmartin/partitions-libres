pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "partitions-libres"
listOf(":catalog", ":render", ":app")
    .filter { file(it.drop(1) + "/build.gradle.kts").exists() }
    .forEach { include(it) }
