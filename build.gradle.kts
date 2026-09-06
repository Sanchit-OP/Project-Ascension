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

tasks.register("moduleReport") {
    group = "ascension"
    description = "Lists Ascension modules and their tier."

    val names = subprojects.map { it.path }.sorted()
    doLast {
        println("Project-Ascension modules:")
        names.forEach { println("  $it") }
    }
}
