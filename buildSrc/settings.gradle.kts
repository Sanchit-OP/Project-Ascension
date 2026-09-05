dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
    }
    // Reuse the root version catalog so plugin versions have a single home.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
