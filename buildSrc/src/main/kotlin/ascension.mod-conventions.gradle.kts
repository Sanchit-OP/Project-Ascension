// Configuration shared by every Ascension *mod* module.
//
// Applying this plugin is normally the only thing a module build script does.
// Everything below is deliberately derived rather than declared, so that adding
// a module cannot drift from the conventions in ADR-0003.

import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("ascension.java-conventions")
    id("ascension.dev-runtime-conventions")
    id("net.neoforged.moddev")
}

// --- Mod id is derived from the project path, never hand-written -------------
//
//   :modules:core           -> ascension_core
//   :modules:atmosphere     -> ascension_atmosphere
//   :modules:compat:sable   -> ascension_compat_sable
//
// This makes the ADR-0003 tier structure visible in the mod id itself, and
// removes a class of copy-paste mistakes.
val modId: String = generateSequence(project) { it.parent }
    .map { it.name }
    .takeWhile { it != "modules" }
    .toList()
    .reversed()
    .joinToString("_", prefix = "ascension_")

// Jar is named by mod id, not by the (short) Gradle project name, so distributed
// artifacts are self-identifying: ascension_core-0.1.0.jar, not core-0.1.0.jar.
base {
    archivesName = modId
}

val neoForgeVersion: String = providers.gradleProperty("neoforge_version").get()

configure<NeoForgeExtension> {
    version = neoForgeVersion

    parchment {
        minecraftVersion = providers.gradleProperty("parchment_minecraft_version").get()
        mappingsVersion = providers.gradleProperty("parchment_mappings_version").get()
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("${rootProject.projectDir}/run/client")
        }
        register("server") {
            server()
            gameDirectory = file("${rootProject.projectDir}/run/server")
            programArgument("--nogui")
        }
        register("data") {
            data()
            gameDirectory = file("${rootProject.projectDir}/run/data")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

// --- neoforge.mods.toml interpolation ---------------------------------------
//
// Only version constraints are injected. Human-readable fields (display name,
// description, credits) live literally in each module's own mods.toml, because
// they are module-specific documentation rather than build configuration.
val modsTomlProperties = mapOf(
    "mod_id" to modId,
    "mod_version" to version.toString(),
    "mod_license" to providers.gradleProperty("mod_license").get(),
    "mod_authors" to providers.gradleProperty("mod_authors").get(),
    "minecraft_version_range" to providers.gradleProperty("minecraft_version_range").get(),
    "neoforge_version_range" to providers.gradleProperty("neoforge_version_range").get(),
    "loader_version_range" to providers.gradleProperty("loader_version_range").get(),
)

tasks.named<ProcessResources>("processResources") {
    inputs.properties(modsTomlProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(modsTomlProperties)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Vendor" to providers.gradleProperty("mod_authors").get(),
            "Specification-Version" to "1",
            "Implementation-Title" to modId,
            "Implementation-Version" to version,
            "Implementation-Vendor" to providers.gradleProperty("mod_authors").get(),
        )
    }
}
