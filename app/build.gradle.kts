plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("kotlin-kapt")
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.quranapp.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quranapp.android"
        minSdk = 23
        targetSdk = 35
        // I don't know why I've used such a weird versioning scheme in the beginning,
        // but I can't change it now as the app is already in the Play Store
        // now just incrementing from there
        versionCode = 23_11_11_129
        versionName = "2026.02.22.2"

        setProperty("archivesBaseName", versionName)

        resValue("string", "app_name", "QuranApp")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.generatedDensities()
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false

            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "QuranApp Debug")

            /* ---------------------------------------------------------------- */
            resValue("string", "cleartextTrafficPermitted", "true")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            /* ---------------------------------------------------------------- */
            resValue("string", "cleartextTrafficPermitted", "false")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(project(":peacedesign"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.ui.tooling)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.androidx.coreKtx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.service)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.legacySupport)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.media)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activityKtx)
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.fragmentKtx)

    implementation(libs.exoplayerCore)
    implementation(libs.exoplayerUI)
    implementation(libs.extensionMediasession)

    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.material)
    implementation(libs.apache.commons)
    implementation(libs.guava)
    implementation(libs.viewbinding)

    /* kotlinx serialization */
    implementation(libs.retrofit)
    implementation(libs.kotlinxSerialization)
    implementation(libs.kotlinxRetrofit)

    /* SmoothRefreshLayout */
    implementation(libs.srlCore)
    implementation(libs.srlExtClassics)
    implementation(libs.srlExtMaterial)
    implementation(libs.srlExtDynamicRebound)
    implementation(libs.srlExtHorizontal)
    implementation(libs.srlExtTwoLevel)
    implementation(libs.srlExtUtil)

    implementation(libs.commonsCompress)
    implementation(libs.workManager)
    implementation(libs.dataStore)
}
