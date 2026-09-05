// Root build script.
//
// Deliberately near-empty: all shared configuration lives in the convention
// plugins under buildSrc/, so that adding a module is a one-line change in
// settings.gradle.kts plus a three-line build script.
//
// See docs/technical/architecture.md.

tasks.register("moduleReport") {
    group = "ascension"
    description = "Lists Ascension modules and their tier."

    val names = subprojects.map { it.path }.sorted()
    doLast {
        println("Project-Ascension modules:")
        names.forEach { println("  $it") }
    }
}
