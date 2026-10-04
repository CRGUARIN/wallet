import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktlint)
}

// Release signing comes from WALLET_* environment variables or a git-ignored keystore.properties.
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

fun signingValue(
    envName: String,
    propertyName: String,
): String? = System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProperties.getProperty(propertyName)

val releaseKeystorePath = signingValue("WALLET_KEYSTORE_PATH", "storeFile")
val releaseKeystorePassword = signingValue("WALLET_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("WALLET_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("WALLET_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning =
    listOf(releaseKeystorePath, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.crguarin.wallet"
    compileSdk = 37

    // Per-app language (ES/EN) needs every language in the install, not only the device's one.
    bundle {
        language {
            enableSplit = false
        }
    }

    defaultConfig {
        applicationId = "com.crguarin.wallet"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        // Debug builds install alongside the owner's release app (com.crguarin.wallet) on the
        // test phone: a different application id and name keeps both installed and untouched.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig =
                if (hasReleaseSigning) {
                    signingConfigs.getByName("release")
                } else {
                    logger.warn(
                        "WARNING: no release signing config found (WALLET_KEYSTORE_* env vars or keystore.properties); " +
                            "signing the release APK with the debug key. Do not publish this build.",
                    )
                    signingConfigs.getByName("debug")
                }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    // Room schemas for MigrationTestHelper (Robolectric reads android_merged_assets, which is
    // mergeDebugAssets, and AGP no longer packages `test` source-set assets there): debug-only,
    // so release APKs are unaffected.
    sourceSets {
        getByName("debug") {
            assets.srcDir("$projectDir/schemas")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Navigation & Serialization
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
