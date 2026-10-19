plugins {
    id("ascension.mod-conventions")
}

description =
    "Removes Create's Copper/Netherite Backtank and Diving Helmet and Boots from the game -- our own " +
    "atmosphere tank already owns the wearable-pressurised-air idea, and two of them side by side is just " +
    "player confusion -- and makes raw helium a superheated blaze-burner fuel (which is also what " +
    "a thruster burns, so helium becomes rocket fuel)."

// No `dependencies {}` block: this jar never calls a line of Create's Java API. The integration is a datapack
// (recipe and advancement overrides, a hidden-from-recipe-viewers tag and a fuel data map) plus one listener that
// takes six items out of the creative menu by registry name, so there is nothing to compile against.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val createVersionRange: String = providers.gradleProperty("create_version_range").get()
modsTomlProperties["create_version_range"] = createVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("create_version_range", createVersionRange)
}
