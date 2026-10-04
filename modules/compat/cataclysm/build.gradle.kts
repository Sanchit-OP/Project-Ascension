plugins {
    id("ascension.mod-conventions")
}

description =
    "Places L_Ender's Cataclysm's Ruined Citadel on the Moon at a chosen spot, and points its " +
    "Ender Golem's drop at our own gateway key instead of Cataclysm's own reward."

// No `dependencies {}` block: this jar never calls a line of Cataclysm's Java API. The whole
// integration is a datapack (a structure biome override, a structure_set placement override
// using ascension-worlds' own fixed_position placement type, and a loot table override), so
// there is nothing to compile against.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val cataclysmVersionRange: String = providers.gradleProperty("cataclysm_version_range").get()
modsTomlProperties["cataclysm_version_range"] = cataclysmVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("cataclysm_version_range", cataclysmVersionRange)
}
