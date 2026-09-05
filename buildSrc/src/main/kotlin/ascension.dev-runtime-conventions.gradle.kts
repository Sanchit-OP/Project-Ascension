// Seeds fresh dev game directories with sane defaults.
//
// Minecraft only writes options.txt after the first launch, so every brand-new run
// directory shows the accessibility onboarding prompt before the main menu, starts paused on
// focus loss, and replays the movement tutorial. During development we create run directories
// constantly, so this is friction on every single iteration.
//
// The task is idempotent and never clobbers settings that are already present: it creates
// options.txt if missing, and otherwise only appends keys that are absent.

/** Dev-only defaults, each one justified. Values are Minecraft options.txt keys. */
val devGameOptions = mapOf(
    // Skips the "Would you like to enable the Narrator?" screen on first launch.
    "onboardAccessibility" to "false",
    // Keeps the game running while alt-tabbed to an editor. Single-player only pauses when
    // this is true, and pausing mid-test invalidates timing observations.
    "pauseOnLostFocus" to "false",
    // Suppresses the movement tutorial toasts.
    "tutorialStep" to "none",
    // Narrator explicitly off.
    "narrator" to "0",
)

val seedDevGameOptions = tasks.register("seedDevGameOptions") {
    group = "ascension"
    description = "Seeds run/* game directories with dev-friendly Minecraft options."

    val runRoot = rootProject.layout.projectDirectory.dir("run").asFile
    val options = devGameOptions

    doLast {
        listOf("client", "server", "data").forEach { which ->
            val dir = runRoot.resolve(which)
            if (!dir.exists()) {
                logger.info("No run/{} directory yet; nothing to seed", which)
                return@forEach
            }

            val file = dir.resolve("options.txt")
            if (!file.exists()) {
                file.writeText(options.entries.joinToString("\n") { "${it.key}:${it.value}" } + "\n")
                logger.lifecycle("Seeded ${file.path} with ${options.size} dev defaults")
                return@forEach
            }

            val existing = file.readLines()
            val present = existing.mapNotNull { it.substringBefore(':', "").ifEmpty { null } }.toSet()
            val missing = options.filterKeys { it !in present }
            if (missing.isNotEmpty()) {
                file.appendText(missing.entries.joinToString("\n") { "${it.key}:${it.value}" } + "\n")
                logger.lifecycle("Added ${missing.keys} to ${file.path}")
            }
        }
    }
}

// Ordering matters and is easy to get wrong. The run directory does not exist until
// prepare<Type>Run creates it, so this must hook the *preparation* task, not the run task.
//
// An earlier version also used dependsOn(seedDevGameOptions) on runClient/runServer/runData.
// That was a latent bug: dependsOn gives no ordering guarantee relative to prepareClientRun,
// so on a fresh clone the seed could run first, find no directory, silently do nothing, and
// the onboarding screen would appear anyway -- the exact problem this task exists to prevent.
// It only appeared to work here because the run directory already existed from an earlier
// launch. finalizedBy on the prepare task is ordered by construction.
tasks.matching { it.name.startsWith("prepare") && it.name.endsWith("Run") }.configureEach {
    finalizedBy(seedDevGameOptions)
}
