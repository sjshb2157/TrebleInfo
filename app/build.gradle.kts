/*
 *     Treble Info
 *     Copyright (C) 2019-2026 Hackintosh Five
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: GPL-3.0-or-later

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
    id("poeditor-android")
    id("materialdesignicons-android")
}

/**
 * Reads a `.properties` file from the module directory.
 *
 * Replaces `org.jetbrains.kotlin.konan.properties.loadProperties`, which is an
 * internal Kotlin/Native helper that happened to be on the buildscript
 * classpath and is not part of any supported API.
 */
fun readProperties(name: String): Properties = Properties().apply {
    val propertiesFile = file(name)
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

val versionProperties = readProperties("version.properties")
val billingProperties = readProperties("billing.properties")
val signingProperties = readProperties("signing.properties")

val appVersionName: String = versionProperties.getProperty("versionName")
val appVersionCode: Int = versionProperties.getProperty("versionCode").toInt()

aboutLibraries {
    // `configPath` / `excludeFields` moved into the `collect` and `export`
    // blocks in AboutLibraries 11. `excludeFields = ["generated"]` is gone;
    // the timestamp is now controlled by `includeMetaData`, which must stay
    // off for reproducible builds.
    collect {
        configPath = layout.projectDirectory.dir("librariesConfig")
    }
    export {
        includeMetaData = false
        prettyPrint = true
    }
}

fun com.android.build.api.dsl.BuildType.setupBilling() {
    buildConfigField("String", "GPLAY_PRODUCT", billingProperties.getProperty("gplayProduct"))

    buildConfigField("String", "PAYPAL_EMAIL", billingProperties.getProperty("paypalEmail"))
    buildConfigField("String", "PAYPAL_CURRENCY", billingProperties.getProperty("paypalCurrency"))
    buildConfigField("String", "PAYPAL_DESCRIPTION", billingProperties.getProperty("paypalDescription"))
}

android {
    // AGP 9 replaces the plain `compileSdk = 37` assignment with a block, so
    // that minor platform revisions (android-37.1, ...) can be expressed.
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt())
    }
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "tk.hack5.treblecheck"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notClass"] = "tk.hack5.treblecheck.ScreenshotTaker"
    }

    flavorDimensions += "freedom"
    productFlavors {
        create("free") {
            dimension = "freedom"
        }
        create("nonfree") {
            dimension = "freedom"
        }
    }

    if (file("signing.properties").exists()) {
        signingConfigs {
            create("release") {
                keyAlias = signingProperties.getProperty("keyAlias")
                storeFile = file(signingProperties.getProperty("storeFile"))
                keyPassword = signingProperties.getProperty("keyPassword")
                storePassword = signingProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (file("signing.properties").exists()) {
                signingConfig = signingConfigs["release"]
            }
            setupBilling()
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signingConfigs["debug"]
            setupBilling()
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/DebugProbesKt.bin"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.version"
            excludes += "/kotlin-tooling-metadata.json"
            excludes += "/kotlin/**.kotlin_builtins"
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    lint {
        checkDependencies = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    sourceSets {
        // src/sharedTest/java holds only Kotlin, so registering it on .kotlin
        // is enough; the old duplicate .java registration is gone.
        get("test").kotlin.srcDir("src/sharedTest/java")
        get("test").resources.srcDir("src/sharedTest/resources")
        get("androidTest").kotlin.srcDir("src/sharedTest/java")
        get("androidTest").resources.srcDir("src/sharedTest/resources")
    }
    namespace = "tk.hack5.treblecheck"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

if (file("poeditor.properties").exists()) {
    project.poeditor.apiToken = readProperties("poeditor.properties").getProperty("apiToken")
}

project.poeditor.projectId = 285385

tasks.withType(com.github.penn5.ImportPoEditorStringsBaseTask::class) {
    allowFuzzy = false
}

dependencies {
    val composeBom = platform(libs.compose.bom)

    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsizeclass)
    implementation(libs.compose.animation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.aboutlibraries.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    "nonfreeImplementation"(libs.billing)
    "nonfreeImplementation"(libs.billing.ktx)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockk.agent.jvm)
    testImplementation(libs.test.xmlpull)
    testImplementation(libs.test.kxml2)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.screengrab)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks.named("preBuild") {
    mustRunAfter("updateDrawables")
    mustRunAfter("importTranslations")
}

tasks.register("versionName") {
    // Captured at configuration time so the task body does not reach back into
    // the project, which the configuration cache forbids.
    val version = appVersionName
    doLast {
        println(version)
    }
}
