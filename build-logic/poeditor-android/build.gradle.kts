/*
 * Vendored from https://github.com/penn5/poeditor-android
 * See .upstream-commit for the imported revision.
 *
 * Originally by penn5 (Hackintosh Five), the author of Treble Info.
 * Inlined here so the build no longer depends on an external git submodule.
 */

plugins {
    `kotlin-dsl`
}

group = "com.github.penn5"
version = "0.2.1"

gradlePlugin {
    plugins {
        register("poeditorPlugin") {
            id = "poeditor-android"
            implementationClass = "com.github.penn5.PoEditorPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(libs.agp)
    implementation(libs.kotlin.xml.builder)
}
