plugins {
    id("ascension.mod-conventions")
}

description = "Breathable zones and oxygen. Usable standalone by any mod."

dependencies {
    // Tier 0. The only Ascension module a Tier 1 module may depend on (ADR-0011): shared
    // concepts live in `core` so that Tier 1 modules never depend on each other.
    implementation(project(":modules:core"))
}

// Core has to be *loaded as a mod* in dev runs, not merely on the classpath -- the same
// distinction that cost an evening with Curios. On the classpath alone its @Mod constructor never
// runs, so nothing ever freezes the world-environment registry and every query quietly answers
// "unknown".
//
// Registering it here puts it into -Dfml.modFolders, which is how FML finds our own code in a dev
// run. Needs the other project evaluated first, since we read its source set.
evaluationDependsOn(":modules:core")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
    }
}
