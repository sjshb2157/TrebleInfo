/*
 * Vendored from https://github.com/penn5/materialdesignicons-android
 * See .upstream-commit for the imported revision.
 *
 * Originally by penn5 (Hackintosh Five), the author of Treble Info.
 * Inlined here so the build no longer depends on an external git submodule.
 */

plugins {
    `kotlin-dsl`
}

group = "com.github.penn5"
version = "0.1.2"

gradlePlugin {
    plugins {
        register("materialdesigniconsPlugin") {
            id = "materialdesignicons-android"
            implementationClass = "com.github.penn5.MaterialDesignIconsPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(libs.agp)
    implementation(libs.commons.io)
}

kotlin {
    jvmToolchain(21)
}
