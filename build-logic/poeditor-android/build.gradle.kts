/*
 * Vendored from https://github.com/penn5/poeditor-android by penn5. See .upstream-commit.
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
