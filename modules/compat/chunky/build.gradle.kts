plugins {
    id("ascension.mod-conventions")
}

description = "Pre-generates terrain around a planet's landing spot on first arrival, via Chunky."

val chunkyVersion: String = providers.gradleProperty("chunky_version").get()

dependencies {
    // The only Tier 1 module this integrates -- ADR-0003 rule 1 (never the reverse).
    implementation(project(":modules:worlds"))
    // Compile-only: never bundled, and this jar's own neoforge.mods.toml declares Chunky as a
    // required dependency, so NeoForge simply never loads this mod at all without the real
    // thing present. See ChunkyPregen's javadoc.
    compileOnly("maven.modrinth:chunky:$chunkyVersion")
}

// Core and worlds both have to be loaded as mods in dev runs, not merely on the classpath --
// same reasoning as worlds's own build script for core.
evaluationDependsOn(":modules:core")
evaluationDependsOn(":modules:worlds")

// Atmosphere is deliberately NOT a dependency of this module, or of worlds (ADR-0011) -- but
// this is `devRunHost` (see the root build script), so `./gradlew runDevServer` has to load the
// whole stack from here now, the same way worlds's own build script carries atmosphere for
// exactly this reason. Compiling and running fine without atmosphere present is still the real
// test (ADR-0003 rule 6); this only affects what the "everything on" dev run loads.
evaluationDependsOn(":modules:atmosphere")

// Same reasoning again, one Tier further: ascension-compat-create has no runtime relationship
// with chunky at all -- it is carried here purely because this is devRunHost, and a Tier 2 jar
// left out of the shared dev run would never get exercised by `runDevServer` even though its
// entire job is reacting to another mod (Create) that only that run has installed.
evaluationDependsOn(":modules:compat:create")

// ascension-combat, same reasoning as atmosphere above: no runtime relationship with chunky or
// worlds (ADR-0011), carried here purely so `runDevServer` exercises it. Its own module-alone
// check is `:modules:combat:runServer -PnoDevMods` (ADR-0003 rule 6), not this run.
evaluationDependsOn(":modules:combat")

// Same reasoning as compat:create -- server-side (unlike compat:jade/immersivedamageindicators,
// both CLIENT-only per their own neoforge.mods.toml and so absent from this list), so
// `runDevServer` needs it here to actually exercise the Ruined Citadel relocation.
evaluationDependsOn(":modules:compat:cataclysm")

// Same reasoning again -- server-side spawn population, so `runDevServer` needs it here to
// actually exercise the Moon's new biome spawn modifier.
evaluationDependsOn(":modules:compat:endermanoverhaul")

// Same reasoning again -- server-side (it moves ships and their crews), so `runDevServer` needs it
// here to exercise ship travel at all.
evaluationDependsOn(":modules:compat:sable")

// Same reasoning again -- server-side (it answers "who is on which team" for growth and allies), so
// `runDevServer` and the dev client need it here to exercise parties at all.
evaluationDependsOn(":modules:compat:ftbteams")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        register("ascension_atmosphere") {
            sourceSet(project(":modules:atmosphere").sourceSets["main"])
        }
        register("ascension_worlds") {
            sourceSet(project(":modules:worlds").sourceSets["main"])
        }
        register("ascension_compat_create") {
            sourceSet(project(":modules:compat:create").sourceSets["main"])
        }
        register("ascension_combat") {
            sourceSet(project(":modules:combat").sourceSets["main"])
        }
        register("ascension_compat_cataclysm") {
            sourceSet(project(":modules:compat:cataclysm").sourceSets["main"])
        }
        register("ascension_compat_endermanoverhaul") {
            sourceSet(project(":modules:compat:endermanoverhaul").sourceSets["main"])
        }
        register("ascension_compat_sable") {
            sourceSet(project(":modules:compat:sable").sourceSets["main"])
        }
        register("ascension_compat_ftbteams") {
            sourceSet(project(":modules:compat:ftbteams").sourceSets["main"])
        }
    }
}

// The shared neoforge.mods.toml interpolation in ascension.mod-conventions covers mod_id,
// mod_version and the platform ranges every module needs. This module also needs the Chunky
// version floor, which nothing else does -- interpolated the same way Curios's own version range
// is meant to be once that compat jar exists (see gradle.properties). Added to the shared map
// rather than a second independent expand() call: Groovy's template engine requires every
// ${...} in the file to resolve in one pass, so a second filesMatching on the same file does not
// see tokens the first one already needed to resolve.
@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val chunkyVersionRange: String = providers.gradleProperty("chunky_version_range").get()
modsTomlProperties["chunky_version_range"] = chunkyVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("chunky_version_range", chunkyVersionRange)
}
