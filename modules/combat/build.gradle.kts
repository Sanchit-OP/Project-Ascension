plugins {
    id("ascension.mod-conventions")
}

description = "League-inspired stats, items and rune paths. Data-driven."

dependencies {
    // Tier 0, and the only Ascension module a Tier 1 module may depend on (ADR-0011).
    implementation(project(":modules:core"))
}

// Core has to be loaded as a mod in dev runs, not merely on the classpath -- on the classpath
// alone its @Mod constructor never runs and nothing freezes the shared registries.
evaluationDependsOn(":modules:core")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
    }
}
