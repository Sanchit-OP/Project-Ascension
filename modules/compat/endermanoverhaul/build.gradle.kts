plugins {
    id("ascension.mod-conventions")
}

description =
    "Populates the Moon with Enderman Overhaul's End Islands Enderman via a NeoForge biome " +
    "spawn modifier -- ambient/lightly-hostile presence for a world that otherwise has none."

// No `dependencies {}` block: this jar never calls a line of Enderman Overhaul's Java. The whole
// integration is one NeoForge add_spawns biome modifier referencing its entity by registry id.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val endermanoverhaulVersionRange: String =
    providers.gradleProperty("endermanoverhaul_version_range").get()
modsTomlProperties["endermanoverhaul_version_range"] = endermanoverhaulVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("endermanoverhaul_version_range", endermanoverhaulVersionRange)
}
