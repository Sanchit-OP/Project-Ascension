plugins {
    `kotlin-dsl`
}

dependencies {
    // Put the plugin on the buildSrc classpath so the precompiled convention
    // scripts in src/main/kotlin can apply it by id.
    implementation(libs.moddevgradle)
}
