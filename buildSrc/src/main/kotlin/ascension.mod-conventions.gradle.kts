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

// --- Java Flight Recorder, on demand ----------------------------------------
//
// ADR-0007 rule 12 needs allocation attributed to a call site and a live set read across
// reload cycles. performance-log.md currently asks for both to be squinted at on the F3
// screen, which is why the M0.5 baseline is still missing them.
//
// JFR is in the JDK we already pin, so this costs no dependency and no mod. Off unless asked
// for, because a recording is overhead and a file on disk that nobody wanted.
//
//   ./gradlew :modules:atmosphere:runServer -Pjfr
//   jfr summary run/server/ascension-server.jfr
//   jfr print --events jdk.ObjectAllocationSample run/server/ascension-server.jfr
//
// or open the file in JDK Mission Control for the flame graph.
val jfrRequested: Boolean = providers.gradleProperty("jfr").isPresent

fun jfrArguments(which: String): List<String> {
    // Forward slashes deliberately. Java accepts them on Windows, and the alternative is a
    // Windows path full of backslashes going into an @argfile, where a backslash is the escape
    // character -- so the correctness of the path would depend on someone else's escaping.
    val projectRoot = rootProject.projectDir.path.replace('\\', '/')
    val recording = "$projectRoot/run/$which/ascension-$which.jfr"
    return listOf(
        // dumponexit is what makes this usable: quitting the game writes the file, so a
        // measurement run is "start, play, quit" rather than "remember to dump it".
        "-XX:StartFlightRecording=name=ascension,settings=profile,dumponexit=true,"
            + "filename=$recording",
        // The default 64 frames is not enough. Minecraft's own call stacks are deep, so an
        // allocation inside our code truncates before it reaches a com.ascension frame --
        // which turns "who allocated this" into "something, somewhere in Minecraft".
        "-XX:FlightRecorderOptions=stackdepth=256",
    )
}

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
            if (jfrRequested) {
                jvmArguments.addAll(jfrArguments("client"))
            }
        }
        register("server") {
            server()
            gameDirectory = file("${rootProject.projectDir}/run/server")
            programArgument("--nogui")
            if (jfrRequested) {
                jvmArguments.addAll(jfrArguments("server"))
            }
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
