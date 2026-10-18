// Root build script.
//
// Deliberately near-empty: all shared configuration lives in the convention
// plugins under buildSrc/, so that adding a module is a one-line change in
// settings.gradle.kts plus a three-line build script.
//
// See docs/technical/architecture.md.

// --- The dev run ------------------------------------------------------------
//
// Each module has its own runClient/runServer, and each one loads only itself and what it
// depends on. That is right for checking a module works alone (ADR-0003 rule 6) and wrong for
// everything else: `:modules:atmosphere:runServer` has no idea `ascension-worlds` exists, so a
// dimension it declares is simply absent, and the symptom is "Unknown dimension" rather than
// anything pointing at the build.
//
// So there is one command that always launches the whole stack, and it does not change as
// modules are added. Only the constant below does.
//
//     ./gradlew runDevServer
//     ./gradlew runDevClient
//
// The host is the module furthest down the dependency chain, since its run already loads
// everything beneath it. Evaluation order follows real dependencies, so it cannot cycle.
//
// Moved to :modules:compat:chunky once that module existed: Tier 2 depends on the Tier 1 it
// integrates, so it is now the furthest thing down the chain, and Chunky is already mirrored
// into dev runs via dev_mods_dir, so the compat jar has something real to talk to.
val devRunHost = ":modules:compat:chunky"

tasks.register("runDevServer") {
    group = "ascension"
    description = "Dev server with every Ascension module loaded."
    dependsOn("$devRunHost:runServer")
}

tasks.register("runDevClient") {
    group = "ascension"
    description = "Dev client with every Ascension module loaded."
    dependsOn("$devRunHost:runClient")
}

// --- The resource ledger ----------------------------------------------------
//
// docs/progression/resource-ledger.md is generated from every module's data by
// tools/resource-ledger.py: each item we register, where a player gets it, what uses it. Every
// module's `check` (so every `build`) runs it with --check, which fails when an item has no
// source, a material has no use, or the ledger is stale; the rules file excuses known gaps, with
// reasons. Titanium was mined for a month with no use; this is what catches that now
// (docs/progression/resource-economy.md, principle 6). Needs Python 3 on PATH as `python`,
// standard library only. Half a second, so it always runs.
val checkResourceLedger = tasks.register<Exec>("checkResourceLedger") {
    group = "verification"
    description = "Fails if an item has no source, a material has no use, or the resource ledger is stale."
    workingDir = rootDir
    commandLine("python", "tools/resource-ledger.py", "--check")
}

// Data mistakes Minecraft ignores without a word (tools/check-data.py): a misspelt field turns a
// narrow condition into an always-true one. Same Python and the same always-on rule as the ledger.
val checkData = tasks.register<Exec>("checkData") {
    group = "verification"
    description = "Fails on a data mistake the game would silently ignore."
    workingDir = rootDir
    commandLine("python", "tools/check-data.py")
}

// The quest book (tools/generate-quests.py, docs/progression/quests.md): fails when a page names an item,
// advancement or dimension that does not exist, uses a guarded story word, loops its dependencies, or when
// the files generated from the pages are out of date. The generator's own tests run beside it.
val checkQuests = tasks.register<Exec>("checkQuests") {
    group = "verification"
    description = "Fails on a quest book mistake, or when the generated quest files are stale."
    workingDir = rootDir
    commandLine("python", "tools/generate-quests.py", "--check")
}

val testQuestTools = tasks.register<Exec>("testQuestTools") {
    group = "verification"
    description = "Runs the quest generator's own tests."
    workingDir = rootDir
    commandLine("python", "-m", "unittest", "discover", "-s", "tools/quests/tests", "-t", "tools")
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(checkResourceLedger, checkData, checkQuests, testQuestTools)
    }
}

tasks.register("moduleReport") {
    group = "ascension"
    description = "Lists Ascension modules and their tier."

    val names = subprojects.map { it.path }.sorted()
    doLast {
        println("Project-Ascension modules:")
        names.forEach { println("  $it") }
    }
}
