plugins {
    id("ascension.mod-conventions")
}

description =
    "Takes Paragliders' goddess and horned statues out of world generation. The pack uses the glider and nothing else " +
    "of it (no Heart Containers, no Stamina Vessels): a tag override per statue, no Java of Paragliders ever called."

// No `dependencies {}` block: this jar never calls a line of Paragliders' Java. The whole integration is three
// empty biome tags in its namespace. The other half (no Spirit Orbs, no vessels, no village statues) is the pack's
// config, pack/config/paraglider-common.toml and paraglider-server.toml.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val paragliderVersionRange: String = providers.gradleProperty("paraglider_version_range").get()
modsTomlProperties["paraglider_version_range"] = paragliderVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("paraglider_version_range", paragliderVersionRange)
}
