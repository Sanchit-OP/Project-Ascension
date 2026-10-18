plugins {
    id("ascension.mod-conventions")
}

description = "The quest book's look (theme, shapes, backdrops) and a live 'you are here' marker on its Planets chart."

// No dependency on Quests, on any Ascension module, or on anything else: the theme, shapes and pictures
// are plain resources FTB Quests reads from every pack, and the marker is a texture drawn with Minecraft
// and NeoForge alone. That is deliberate: a Tier-2 jar that compiles against a mod breaks when the mod's
// internals move, and this one never touches them. This jar's own neoforge.mods.toml declares Quests as a
// required dependency, so NeoForge never loads it into a game without the book.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val ftbQuestsVersionRange: String = providers.gradleProperty("ftbquests_version_range").get()
modsTomlProperties["ftbquests_version_range"] = ftbQuestsVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("ftbquests_version_range", ftbQuestsVersionRange)
}
