plugins {
    id("ascension.mod-conventions")
}

description =
    "Places L_Ender's Cataclysm's Ruined Citadel on the Moon at a chosen spot, and adds our own " +
    "gateway key to its Ender Guardian's drops."

// No `dependencies {}` block: nothing here compiles against Cataclysm. Most of the integration is
// datapack (structure biome + structure_set overrides, a loot table override). The two ADR-0013
// mixins name their targets by string (`@Mixin(targets = "...")`), and GuardianAwakening looks the
// Guardian up by registry id -- so a Cataclysm rename fails at mod load, not at compile time.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val cataclysmVersionRange: String = providers.gradleProperty("cataclysm_version_range").get()
modsTomlProperties["cataclysm_version_range"] = cataclysmVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("cataclysm_version_range", cataclysmVersionRange)
}
