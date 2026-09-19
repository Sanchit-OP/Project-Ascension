plugins {
    id("ascension.mod-conventions")
}

description = "Repositions and enlarges Immersive Damage Indicators' floating numbers so they " +
    "render above a hit target instead of inside its model."

val idiVersion: String = providers.gradleProperty("immersivedamageindicators_version").get()
val immersiveMessagesVersion: String = providers.gradleProperty("immersivemessages_version").get()
val txniLibVersion: String = providers.gradleProperty("txnilib_version").get()

dependencies {
    // No Ascension module involved at all -- this is a pure rendering tweak to a third party
    // mod's own floating combat text, not a bridge to anything of ours (unlike every other
    // compat jar so far). Compile-only, same reasoning as ascension-compat-chunky: never bundled,
    // and this jar's own neoforge.mods.toml requires Immersive Damage Indicators, so NeoForge
    // simply never loads this mod at all without the real thing present.
    compileOnly("maven.modrinth:immersive-damage-indicators:$idiVersion")
    // Needed at compile time because our code references toni.immersivemessages.api.ImmersiveMessage
    // directly (IDI's own CreateDamageIndicator event hands one to every listener). Never a
    // required dependency of our own neoforge.mods.toml -- IDI's own toml already requires it, so
    // it is guaranteed present the moment IDI is.
    compileOnly("maven.modrinth:immersive-messages-api:$immersiveMessagesVersion")
    // TxniLib is a separate required-dependency library mod (both Immersive Messages and Immersive
    // Damage Indicators declare it in their own neoforge.mods.toml) that carries the toni.lib.*
    // package -- AnimationTimeline, AnimationKeyframe, OnPoseMessage's own parameter types -- which
    // is not shaded into either mod's jar. Compile-only, same reasoning as the two dependencies
    // above: never bundled, guaranteed present at runtime because Immersive Messages itself
    // requires it.
    compileOnly("maven.modrinth:txnilib:$txniLibVersion")
    // CreateDamageIndicator.EVENT is a Fabric-style net.fabricmc.fabric.api.event.Event<T>, which
    // lives in the "fabric-api-base" sub-module -- not separately published on Modrinth, and
    // Forgified Fabric API's own Modrinth artifact is a JarJar shell with no top-level classes of
    // its own (confirmed: unzipping it finds zero .class files, only META-INF/jars/*.jar entries).
    // Vendored here as the one sub-jar actually needed, extracted directly from the installed
    // runtime copy. Compile-only and never bundled, same as the two dependencies above; Immersive
    // Damage Indicators' own required dependency on Forgified Fabric API guarantees the real thing
    // is present at runtime regardless.
    compileOnly(files("libs/fabric-api-base-0.4.42+d1308ded19.jar"))
}

@Suppress("UNCHECKED_CAST")
val modsTomlProperties = project.extra["modsTomlProperties"] as MutableMap<String, String>
val idiVersionRange: String = providers.gradleProperty("immersivedamageindicators_version_range").get()
modsTomlProperties["immersivedamageindicators_version_range"] = idiVersionRange

tasks.named<ProcessResources>("processResources") {
    inputs.property("immersivedamageindicators_version_range", idiVersionRange)
}
