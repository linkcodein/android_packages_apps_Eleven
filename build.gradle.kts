/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

import org.lineageos.generatebp.GenerateBpPlugin
import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module

plugins {
    id("com.android.application") version "8.1.2"
    id("org.jetbrains.kotlin.android") version "1.7.10"
}

apply {
    plugin<GenerateBpPlugin>()
}

buildscript {
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.17/.m2")
    }

    dependencies {
        classpath("org.lineageos:gradle-generatebp:+")
    }
}

android {
    compileSdk = 36
    namespace = "org.lineageos.eleven"

    defaultConfig {
        applicationId = "org.lineageos.eleven"
        minSdk = 30
        targetSdk = 36
        versionCode = 420
        versionName = "4.2.0"
    }

    signingConfigs {
        create("release") {
            // The release artifact is re-signed with apksigner using the
            // platform release keys at /storage/release-keys.{pk8,x509.pem}.
            // The keystore used here only exists so Gradle can produce a
            // signed installable APK during the build step.
            val tempKeystore = file("${rootProject.projectDir}/build/temp-release.jks")
            if (tempKeystore.exists()) {
                storeFile = tempKeystore
                storePassword = "eleven"
                keyAlias = "eleven"
                keyPassword = "eleven"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
        }
        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            // Includes the default ProGuard rules files.
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard.cfg"
                )
            )

            // Sign the release build with a temporary keystore so the APK is
            // installable. The final artifact is re-signed with the platform
            // release keys via apksigner after the build.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")

            aidl.srcDirs("src")
            assets.srcDirs("assets")
            java.srcDirs("src")
            res.srcDirs("res")
            resources.srcDirs("res")
        }
    }

    buildFeatures {
        aidl = true
    }

    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
        checkAllWarnings = true
        showAll = true
        warningsAsErrors = true
        xmlReport = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")
}

configure<GenerateBpPluginExtension> {
    targetSdk.set(android.defaultConfig.targetSdk!!)
    minSdk.set(android.defaultConfig.minSdk!!)
    availableInAOSP.set { module: Module ->
        when {
            module.group.startsWith("androidx") -> true
            module.group.startsWith("org.jetbrains") -> true
            module.group == "com.google.errorprone" -> true
            module.group == "com.google.guava" -> true
            module.group == "junit" -> true
            else -> false
        }
    }
}