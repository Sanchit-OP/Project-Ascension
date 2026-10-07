plugins {
    id("ascension.mod-conventions")
}

description =
    "Gives ascension-combat's melee weapons Better Combat's two-handed and one-handed holds and " +
    "swing animations, through its weapon_attributes data."

// No `dependencies {}` block: this jar never calls a line of Better Combat's Java (all rights
// reserved -- we reference it, never bundle or copy it). The whole integration is one
// weapon_attributes file per weapon, naming the item by registry id and Better Combat's own
// animations and poses by theirs.

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val bettercombatVersionRange: String = providers.gradleProperty("bettercombat_version_range").get()
modsTomlProperties["bettercombat_version_range"] = bettercombatVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("bettercombat_version_range", bettercombatVersionRange)
}
