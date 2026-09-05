pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
    }
}

plugins {
    // Lets Gradle provision the Java 21 toolchain if it is not already present.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "project-ascension"

// --- Tier 0 -----------------------------------------------------------------
include(":modules:core")

// --- Tier 1 (added as each milestone reaches them) ---------------------------
include(":modules:atmosphere")
include(":modules:worlds")
// include(":modules:progression")    // M3
// include(":modules:gear")           // M4

// --- Tier 2: optional integration jars, never depended on by Tier 1 ----------
// include(":modules:compat:curios")  // next, once Curios is installed in the test instance
// include(":modules:compat:sable")   // M5
// include(":modules:compat:create")
