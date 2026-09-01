/*
 * Vendored from https://github.com/penn5/materialdesignicons-android by penn5. See .upstream-commit.
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
