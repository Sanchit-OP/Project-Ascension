plugins {
    id("ascension.mod-conventions")
}

description =
    "Disables Create's Copper/Netherite Backtank and Diving Helmet -- our own atmosphere tank " +
    "already owns the wearable-pressurised-air idea, and two of them side by side is just " +
    "player confusion."

// No `dependencies {}` block: this jar never calls a line of Create's Java API. The whole
// integration is a datapack (recipe overrides + a hidden-from-recipe-viewers tag), so there is
// nothing to compile against.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val createVersionRange: String = providers.gradleProperty("create_version_range").get()
modsTomlProperties["create_version_range"] = createVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("create_version_range", createVersionRange)
}
