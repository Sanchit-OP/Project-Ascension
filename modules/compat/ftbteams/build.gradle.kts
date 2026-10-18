plugins {
    id("ascension.mod-conventions")
}

description = "Tells Ascension which FTB Teams party a player is on, so a boss's points, allies and friendly fire follow it."

val ftbTeamsVersion: String = providers.gradleProperty("ftbteams_version").get()
val ftbLibraryVersion: String = providers.gradleProperty("ftblibrary_version").get()
val architecturyVersion: String = providers.gradleProperty("architectury_version").get()

dependencies {
    // The one Ascension module this needs: TeamRegistry is a contract in core (ADR-0011, ADR-0023).
    // No Tier 1 module is depended on, and none depends on this.
    implementation(project(":modules:core"))
    // Compile-only: never bundled, and this jar's own neoforge.mods.toml declares each as a required
    // dependency, so NeoForge never loads this mod without the real thing present. Not transitive:
    // Teams' own pom pulls in a tree this jar has no use for. Library is here only because Teams'
    // signatures name its types; Architectury because Teams' events are Architectury events.
    compileOnly("dev.ftb.mods:ftb-teams-neoforge:$ftbTeamsVersion") { isTransitive = false }
    compileOnly("dev.ftb.mods:ftb-library-neoforge:$ftbLibraryVersion") { isTransitive = false }
    compileOnly("dev.architectury:architectury-neoforge:$architecturyVersion") { isTransitive = false }
}

evaluationDependsOn(":modules:core")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
    }
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val ftbTeamsVersionRange: String = providers.gradleProperty("ftbteams_version_range").get()
val architecturyVersionRange: String = providers.gradleProperty("architectury_version_range").get()
modsTomlProperties["ftbteams_version_range"] = ftbTeamsVersionRange
modsTomlProperties["architectury_version_range"] = architecturyVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("ftbteams_version_range", ftbTeamsVersionRange)
    inputs.property("architectury_version_range", architecturyVersionRange)
}
