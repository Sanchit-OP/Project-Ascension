plugins {
    id("ascension.mod-conventions")
}

description = "Makes Distant Horizons' LOD clouds obey a dimension's own no-clouds setting."

val distantHorizonsVersion: String = providers.gradleProperty("distanthorizons_version").get()

dependencies {
    // No Ascension module involved: the rule is vanilla's own (a dimension whose special effects
    // have a NaN cloud height has no clouds), so it holds for the Moon, space, and any other mod's
    // airless world alike. Compile-only, same reasoning as ascension-compat-chunky: never bundled,
    // and this jar's own neoforge.mods.toml requires Distant Horizons, so NeoForge never loads
    // this mod without the real thing present.
    compileOnly("maven.modrinth:distanthorizons:$distantHorizonsVersion")
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val distantHorizonsVersionRange: String = providers.gradleProperty("distanthorizons_version_range").get()
modsTomlProperties["distanthorizons_version_range"] = distantHorizonsVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("distanthorizons_version_range", distantHorizonsVersionRange)
}
