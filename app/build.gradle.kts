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

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
    id("materialdesignicons-android")
}

fun readProperties(name: String): Properties = Properties().apply {
    val propertiesFile = file(name)
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

fun Properties.require(key: String, file: String): String = getProperty(key)
    ?: error("$key is missing from app/$file")

val versionProperties = readProperties("version.properties")
val signingProperties = readProperties("signing.properties")

val appVersionName: String = versionProperties.require("versionName", "version.properties")
val appVersionCode: Int = versionProperties.require("versionCode", "version.properties").toInt()

aboutLibraries {
    collect {
        configPath = layout.projectDirectory.dir("librariesConfig")
    }
    export {
        // Timestamps break reproducible builds.
        includeMetaData = false
        prettyPrint = true
    }
}

android {
    compileSdk {
        version = release(37)
    }
    ndkVersion = "30.0.16138531"

    defaultConfig {
        applicationId = "tk.hack5.treblecheck"
        // Project Treble starts at API 26.
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
    }

    if (file("signing.properties").exists()) {
        signingConfigs {
            create("release") {
                keyAlias = signingProperties.require("keyAlias", "signing.properties")
                storeFile = file(signingProperties.require("storeFile", "signing.properties"))
                keyPassword = signingProperties.require("keyPassword", "signing.properties")
                storePassword = signingProperties.require("storePassword", "signing.properties")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (file("signing.properties").exists()) {
                signingConfig = signingConfigs["release"]
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signingConfigs["debug"]
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
        includeInApk = false
        includeInBundle = false
    }
    androidResources {
        localeFilters += listOf("zh-rCN", "zh-rTW")
    }
    lint {
        checkDependencies = true
        disable += setOf("GradleDependency", "NewerVersionAvailable")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    sourceSets {
        get("test").kotlin.directories += "src/sharedTest/java"
        get("test").resources.directories += "src/sharedTest/resources"
        get("androidTest").kotlin.directories += "src/sharedTest/java"
        get("androidTest").resources.directories += "src/sharedTest/resources"
    }
    namespace = "tk.hack5.treblecheck"
}


dependencies {
    constraints {
        implementation(libs.kotlin.stdlib) { version { strictly(libs.versions.kotlinStdlib.get()) } }
    }

    val composeBom = platform(libs.compose.bom)

    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.windowsizeclass)
    implementation(libs.compose.animation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.aboutlibraries.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockk.agent.jvm)
    testImplementation(libs.test.xmlpull)
    testImplementation(libs.test.kxml2)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}

tasks.named("preBuild") {
    mustRunAfter("updateDrawables")
}

tasks.register("versionName") {
    // Read now: the configuration cache forbids project access in doLast.
    val version = appVersionName
    doLast {
        println(version)
    }
}
