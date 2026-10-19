plugins {
    id("ascension.mod-conventions")
}

description =
    "Removes Create's Copper/Netherite Backtank and Diving Helmet and Boots from the game -- our own " +
    "atmosphere tank already owns the wearable-pressurised-air idea -- makes raw helium a superheated " +
    "blaze-burner fuel (which is also what a thruster burns), and adds the Air Compressor: a Create " +
    "machine that turns rotation into air (ADR-0026)."

val createVersion: String = providers.gradleProperty("create_version").get()

repositories {
    // Create's own maven, only for the two libraries its jar carries inside itself (jar-in-jar),
    // which javac still has to see when we extend a Create class. Compile-only, never bundled.
    maven("https://maven.createmod.net") {
        name = "CreateMod"
        content {
            includeGroup("net.createmod.ponder")
            includeGroup("dev.engine-room.flywheel")
        }
    }
}

dependencies {
    compileOnly("net.createmod.ponder:Ponder-NeoForge-1.21.1:1.0.69")
    // Tier 1 module this bridges (ADR-0003 rule 1: never the reverse). Reads only its api package.
    implementation(project(":modules:atmosphere"))
    // Compile-only, never bundled: this jar's own neoforge.mods.toml requires Create, so NeoForge
    // never loads it without the real thing. Create's own Flywheel/Ponder/Registrate come inside
    // its jar (jar-in-jar) and are not needed for the few classes used here.
    compileOnly("maven.modrinth:create:$createVersion")
}

evaluationDependsOn(":modules:core")
evaluationDependsOn(":modules:atmosphere")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        register("ascension_atmosphere") {
            sourceSet(project(":modules:atmosphere").sourceSets["main"])
        }
    }
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val createVersionRange: String = providers.gradleProperty("create_version_range").get()
modsTomlProperties["create_version_range"] = createVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("create_version_range", createVersionRange)
}
