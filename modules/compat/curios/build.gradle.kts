plugins {
    id("ascension.mod-conventions")
}

description = "Two Curios slots: a tank slot that breathes from the tank worn there, and a relic slot where " +
    "Zonia's Hourglass and Sterak's Harness work as they do in the off hand."

val curiosVersion: String = providers.gradleProperty("curios_version").get()

dependencies {
    // Tier 1 modules this bridges, api packages only (ADR-0003 rule 1: never the reverse).
    implementation(project(":modules:atmosphere"))
    implementation(project(":modules:combat"))
    // Compile-only, never bundled: this jar's neoforge.mods.toml requires Curios, so NeoForge never loads it without.
    compileOnly("top.theillusivec4.curios:curios-neoforge:$curiosVersion") { isTransitive = false }
}

evaluationDependsOn(":modules:core")
evaluationDependsOn(":modules:atmosphere")
evaluationDependsOn(":modules:combat")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        register("ascension_atmosphere") {
            sourceSet(project(":modules:atmosphere").sourceSets["main"])
        }
        register("ascension_combat") {
            sourceSet(project(":modules:combat").sourceSets["main"])
        }
    }
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val curiosVersionRange: String = providers.gradleProperty("curios_version_range").get()
modsTomlProperties["curios_version_range"] = curiosVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("curios_version_range", curiosVersionRange)
}
