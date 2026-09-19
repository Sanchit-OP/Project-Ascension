plugins {
    id("ascension.mod-conventions")
}

description = "Adds Armor and Magic Resist to Jade's entity tooltip."

val jadeVersion: String = providers.gradleProperty("jade_version").get()

dependencies {
    // The one Tier 1 module this integrates -- reads com.ascension.combat.api.CombatAttributes
    // (never internal, per ADR-0003 rule 4) to show a target's current stat values.
    implementation(project(":modules:combat"))
    // Compile-only: never bundled, and this jar's own neoforge.mods.toml declares Jade as a
    // required dependency, so NeoForge simply never loads this mod at all without the real thing
    // present. See AscensionCompatJade's javadoc.
    compileOnly("maven.modrinth:jade:$jadeVersion")
}

evaluationDependsOn(":modules:core")
evaluationDependsOn(":modules:combat")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        register("ascension_combat") {
            sourceSet(project(":modules:combat").sourceSets["main"])
        }
    }
}

// The shared neoforge.mods.toml interpolation in ascension.mod-conventions covers mod_id,
// mod_version and the platform ranges every module needs. This module also needs Jade's own
// version floor, same pattern chunky_version_range uses for Chunky.
@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val jadeVersionRange: String = providers.gradleProperty("jade_version_range").get()
modsTomlProperties["jade_version_range"] = jadeVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("jade_version_range", jadeVersionRange)
}
