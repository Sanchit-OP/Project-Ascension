plugins {
    id("ascension.mod-conventions")
}

description = "Planets, interplanetary space and gateways. Data-driven."

dependencies {
    // Tier 0, and the only Ascension module a Tier 1 module may depend on (ADR-0011).
    implementation(project(":modules:core"))
}

// Core has to be loaded as a mod in dev runs, not merely on the classpath -- on the classpath
// alone its @Mod constructor never runs and nothing freezes the shared registry.
evaluationDependsOn(":modules:core")

// Atmosphere is deliberately NOT a dependency (ADR-0011) -- this module must work without it,
// and does: with nothing reading environments, planets exist and nobody suffocates. But the dev
// run wants it present, because the whole point of M2.2 is watching the two meet.
evaluationDependsOn(":modules:atmosphere")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        register("ascension_atmosphere") {
            sourceSet(project(":modules:atmosphere").sourceSets["main"])
        }
    }
}
