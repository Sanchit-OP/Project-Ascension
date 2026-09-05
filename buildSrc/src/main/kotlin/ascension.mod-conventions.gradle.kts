// Configuration shared by every Ascension *mod* module.
//
// Applying this plugin is normally the only thing a module build script does.
// Everything below is deliberately derived rather than declared, so that adding
// a module cannot drift from the conventions in ADR-0003.

import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import java.util.zip.ZipFile

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

// --- Third-party mods in dev runs -------------------------------------------
//
// The dev server has to carry the same mods as the CurseForge test instance. Curios registers a
// network channel, so a client that has it will not join a server that does not, and the test
// loop simply stops working.
//
// **This does not weaken ADR-0003 rule 6.** What enforces "atmosphere works with only core
// present" is the *compile* classpath, and Curios is not on it -- Tier 1 cannot reference a
// class it cannot see. A jar present at dev runtime cannot create a compile dependency. To
// check the standalone case explicitly, run with -PnoDevMods.
val devModsEnabled: Boolean = !providers.gradleProperty("noDevMods").isPresent

// Each repository is scoped to the group it actually serves, so a typo'd coordinate fails at
// the one place that was supposed to have it instead of being hunted for across every
// repository in the build. Only compat modules use these; nothing here is on a Tier 1 compile
// classpath.
repositories {
    maven("https://maven.theillusivec4.top") {
        name = "TheIllusiveC4"
        content { includeGroup("top.theillusivec4.curios") }
    }
    maven("https://maven.blamejared.com") {
        name = "BlameJared"
        content { includeGroup("mezz.jei") }
    }
}

/**
 * Third-party mod jars to load into dev runs, taken from the client test instance.
 *
 * Read from the instance rather than declared per mod. A hand-maintained list of fourteen
 * coordinates would drift the moment anyone installed something in CurseForge, and the symptom
 * of that drift is a connection refused with a channel-mismatch message, which does not look
 * like a stale build file at all.
 *
 * Reads the filesystem during configuration. That is fine while the configuration cache is off
 * (see gradle.properties) and is the thing to revisit if it is ever turned on.
 */
fun instanceMods(): List<File> {
    val configured = providers.gradleProperty("dev_mods_dir").orNull?.trim()
    if (configured.isNullOrEmpty()) {
        return emptyList()
    }
    val dir = File(configured)
    if (!dir.isDirectory) {
        logger.warn(
            "dev_mods_dir does not exist, so no third-party mods will be loaded into dev runs: {}",
            configured,
        )
        return emptyList()
    }
    return (dir.listFiles() ?: emptyArray())
        .filter { it.isFile && it.name.endsWith(".jar") }
        // Our own jars: the dev run already has this code from source, and loading the built
        // jar alongside it is a duplicate mod id and an immediate crash.
        .filterNot { it.name.startsWith("ascension_") }
        .sortedBy { it.name }
}

/**
 * Whether a mod jar says it only has anything to do on the client.
 *
 * NeoForge has no mod-level "side" field in neoforge.mods.toml -- the real mechanism is
 * @Mod(dist = Dist.CLIENT) in the class file. What the toml does carry is a `side` on every
 * dependency, and a mod whose every declared dependency is CLIENT is asserting that it needs
 * nothing on a server. That reads across this instance exactly right: it picks out Sodium,
 * ImmediatelyFast and BadOptimizations while correctly leaving ModernFix alone, which has one
 * client-side soft dependency but genuinely runs on both.
 *
 * Sodium is why this exists rather than a hand-written list. It registers an early-bootstrap
 * window service, which FML loads through ServiceLoader *before* mod loading -- so
 * @Mod(dist = Dist.CLIENT) cannot save it, and the dedicated server dies on
 * NoClassDefFoundError: org/lwjgl/Version before printing a single mod name.
 *
 * A heuristic, so it is reported rather than applied silently, and
 * `dev_mods_server_exclude` exists for anything it misses.
 */
fun declaresClientOnly(jar: File): Boolean = try {
    ZipFile(jar).use { zip ->
        val entry = zip.getEntry("META-INF/neoforge.mods.toml")
        if (entry == null) {
            false
        } else {
            val toml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            val sides = Regex("(?im)^\\s*side\\s*=\\s*\"([A-Za-z]+)\"")
                .findAll(toml)
                .map { it.groupValues[1].uppercase() }
                .toList()
            sides.isNotEmpty() && sides.all { it == "CLIENT" }
        }
    }
} catch (e: Exception) {
    // Unreadable jar: assume it belongs on both and let the server say otherwise.
    false
}

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

// Declared *after* the NeoForge extension, because ModDevGradle creates these
// configurations while configuring it -- they do not exist earlier in this script.
//
// Client and server named explicitly rather than using the run-wide
// `additionalRuntimeClasspath`: the data run generates resources and has no business loading
// somebody else's mod.
/** Mods to skip on the server run, and why -- ordered so the reason is reportable. */
fun serverSkips(mods: List<File>): Map<File, String> {
    val manual = providers.gradleProperty("dev_mods_server_exclude").orNull
        ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    val skips = LinkedHashMap<File, String>()
    mods.forEach { jar ->
        val named = manual.firstOrNull { jar.name.contains(it, ignoreCase = true) }
        when {
            named != null -> skips[jar] = "excluded by dev_mods_server_exclude ($named)"
            declaresClientOnly(jar) -> skips[jar] = "declares client-only dependencies"
        }
    }
    return skips
}

/**
 * Copy the instance's mods into the run directories' own `mods` folders.
 *
 * **Not the runtime classpath.** An earlier version added these to
 * `clientAdditionalRuntimeClasspath` / `serverAdditionalRuntimeClasspath`, which put the jars
 * on the classpath and looked entirely correct from the build's side -- the coordinates
 * resolved, the jars appeared in the generated classpath file, and the server booted clean.
 * None of them loaded. FML does not discover mods by scanning the runtime classpath in a dev
 * run, and a mod that never loads breaks nothing at startup, so the failure was completely
 * silent until a client tried to connect and was refused over a missing channel.
 *
 * `<gameDir>/mods` is the folder FML genuinely scans. It has the side benefit of being
 * inspectable: what the run will load is a directory you can list.
 */
val syncDevMods = tasks.register("syncDevMods") {
    group = "ascension"
    description = "Mirrors the test instance's mods into run/client/mods and run/server/mods."

    val runRoot = rootProject.layout.projectDirectory.dir("run").asFile
    val enabled = devModsEnabled

    doLast {
        val mods = if (enabled) instanceMods() else emptyList()
        val skips = serverSkips(mods)
        val perRun = mapOf(
            "client" to mods,
            "server" to mods.filterNot { skips.containsKey(it) },
        )

        perRun.forEach { (which, jars) ->
            val runDir = runRoot.resolve(which)
            if (!runDir.isDirectory) {
                return@forEach
            }
            val modsDir = runDir.resolve("mods")
            modsDir.mkdirs()

            // Cleared first, and this is the point rather than tidiness: a mod *removed* from
            // the instance has to disappear here too. Leaving a stale jar behind is how a
            // server ends up requiring a channel the client no longer has -- the failure in
            // the opposite direction, which is what started all of this.
            val existing = (modsDir.listFiles() ?: emptyArray())
                .filter { it.isFile && it.name.endsWith(".jar") }
            existing.forEach { it.delete() }

            jars.forEach { it.copyTo(modsDir.resolve(it.name), overwrite = true) }
            logger.lifecycle("run/{}/mods: {} mod(s)", which, jars.size)
        }

        if (mods.isNotEmpty()) {
            skips.forEach { (jar, why) ->
                logger.lifecycle("  client only, not on the server: {} -- {}", jar.name, why)
            }
        }
    }
}

// finalizedBy on the *prepare* task, for the reason spelled out in dev-runtime-conventions:
// the run directory does not exist until prepare<Type>Run creates it, and dependsOn would give
// no ordering guarantee against it. Client and server only -- the data run generates resources
// and has no business loading someone else's mod.
tasks.matching { it.name == "prepareClientRun" || it.name == "prepareServerRun" }.configureEach {
    finalizedBy(syncDevMods)
}

// Lists what the dev runs will load, so a channel-mismatch failure can be checked against
// reality in one command instead of being guessed at.
tasks.register("devMods") {
    group = "ascension"
    description = "Lists the third-party mods mirrored into dev runs from the test instance."
    doLast {
        val mods = instanceMods()
        if (!devModsEnabled) {
            println("Dev mods disabled (-PnoDevMods).")
        } else if (mods.isEmpty()) {
            println("No third-party mods found. dev_mods_dir = " +
                (providers.gradleProperty("dev_mods_dir").orNull ?: "<unset>"))
        } else {
            val skips = serverSkips(mods)
            println("${mods.size} mod(s) mirrored into dev runs from the test instance:")
            mods.forEach { jar ->
                val why = skips[jar]
                if (why == null) {
                    println("  both     ${jar.name}")
                } else {
                    println("  client   ${jar.name}  ($why)")
                }
            }
            println()
            println("${mods.size - skips.size} on the server, ${skips.size} client-only.")
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
