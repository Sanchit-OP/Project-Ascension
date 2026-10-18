plugins {
    id("ascension.mod-conventions")
}

description = "The airship road to space: Create Aeronautics ships (Sable) carry their crew between planets and space."

val sableVersion: String = providers.gradleProperty("sable_version").get()

dependencies {
    // The Tier 1 module this bridges -- ADR-0003 rule 1 (never the reverse). Ships cross the same
    // planet boundaries worlds already draws for players.
    implementation(project(":modules:worlds"))
    // Air aboard ships (ADR-0026): registers a ship space with atmosphere's api package only.
    implementation(project(":modules:atmosphere"))
    // Compile-only, never bundled: this jar's own neoforge.mods.toml requires Sable, so NeoForge
    // never loads it without the real thing (ADR-0006).
    compileOnly("maven.modrinth:sable:$sableVersion")
    // Sable's maths types (Pose3d, BoundingBox3d) live in sable-companion, which Sable ships inside
    // its own jar (jar-in-jar) rather than publishing. Vendored here as that one sub-jar, extracted
    // from Sable 2.0.5, the same way compat-immersivedamageindicators vendors fabric-api-base.
    // Compile-only; at runtime Sable's own copy is the one loaded.
    compileOnly(files("libs/sable-companion-common-1.21.1-1.6.0.jar"))
}

evaluationDependsOn(":modules:core")
evaluationDependsOn(":modules:worlds")
evaluationDependsOn(":modules:atmosphere")

neoForge {
    mods {
        register("ascension_core") {
            sourceSet(project(":modules:core").sourceSets["main"])
        }
        register("ascension_worlds") {
            sourceSet(project(":modules:worlds").sourceSets["main"])
        }
        register("ascension_atmosphere") {
            sourceSet(project(":modules:atmosphere").sourceSets["main"])
        }
    }
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val sableVersionRange: String = providers.gradleProperty("sable_version_range").get()
modsTomlProperties["sable_version_range"] = sableVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("sable_version_range", sableVersionRange)
}
