plugins {
    id("ascension.mod-conventions")
}

description = "Shows the combat items' build paths in JEI."

val jeiVersion: String = providers.gradleProperty("jei_version").get()

dependencies {
    // Reads only recipes and item ids, so it needs no combat classes at all; combat is a runtime
    // requirement in the mods.toml so the category is never built around items that do not exist.
    // Compile-only: never bundled, and a missing JEI means NeoForge never loads this jar.
    compileOnly("mezz.jei:jei-1.21.1-neoforge-api:$jeiVersion")
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val jeiVersionRange: String = providers.gradleProperty("jei_version_range").get()
modsTomlProperties["jei_version_range"] = jeiVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("jei_version_range", jeiVersionRange)
}
