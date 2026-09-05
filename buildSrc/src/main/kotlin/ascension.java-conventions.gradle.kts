// Shared Java configuration for every Ascension module.
//
// Applied by ascension.mod-conventions; also usable on its own by a plain
// (non-mod) library module, should we ever need one.

plugins {
    java
    `java-library`
}

group = providers.gradleProperty("mod_group_id").get()
version = providers.gradleProperty("mod_version").get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21

    // Warnings we actually want to see. Deliberately not -Werror yet: NeoForge
    // and Mojang code generate unavoidable noise. Revisit once the baseline is
    // known to be clean (ADR-0007).
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-processing",
            "-Xlint:-serial",
        )
    )
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.withType<Jar>().configureEach {
    // Reproducible builds: identical sources produce an identical jar.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// Unit tests are for pure logic only. Per ADR-0008 they are never evidence for anything
// touching world state -- that requires a running client and a dedicated server.
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
