plugins {
    id("ascension.mod-conventions")
}

description = "The HUD: layout, theme and frame. Other modules contribute elements through core."

dependencies {
    // Tier 0, and the only Ascension module a Tier 1 module may depend on (ADR-0011, ADR-0024).
    implementation(project(":modules:core"))
}

// Core has to be loaded as a mod in dev runs, not merely on the classpath -- on the classpath
// alone its @Mod constructor never runs and nothing freezes the shared registries.
evaluationDependsOn(":modules:core")

// The dev client shows the whole HUD, so it also loads the modules that contribute elements.
//
// **This does not weaken ADR-0003 or ADR-0011.** They are on the *runtime* classpath of a dev run
// only, never the compile classpath, so no class in this module can name one of theirs, exactly as
// the third-party mods in dev runs. `-PhudOnly` runs the HUD with nothing but core, which is the
// standalone case ADR-0003 rule 5 requires to keep working.
val withContributors = !providers.gradleProperty("hudOnly").isPresent
if (withContributors) {
    evaluationDependsOn(":modules:combat")
    evaluationDependsOn(":modules:atmosphere")
    dependencies {
        runtimeOnly(project(":modules:combat"))
        runtimeOnly(project(":modules:atmosphere"))
    }
}

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        if (withContributors) {
            register("ascension_combat") {
                sourceSet(project(":modules:combat").sourceSets["main"])
            }
            register("ascension_atmosphere") {
                sourceSet(project(":modules:atmosphere").sourceSets["main"])
            }
        }
    }
}
