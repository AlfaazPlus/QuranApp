import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.quranapp.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quranapp.android"
        minSdk = 24
        targetSdk = 36

        versionCode = providers.gradleProperty("quranapp.versionCode").get().toInt()
        versionName = providers.gradleProperty("quranapp.versionName").get()

        resValue("string", "app_name", "QuranApp")

        // Rasterizing vectors during the build is not reproducible.
        vectorDrawables.generatedDensities()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        dataBinding = true
        compose = true
        buildConfig = true
    }

    val keystorePath = System.getenv("QURANAPP_KEYSTORE_FILE")
        ?: (project.findProperty("QURANAPP_KEYSTORE_FILE") as String?)
    val releaseStorePassword = System.getenv("QURANAPP_KEYSTORE_PASSWORD")
        ?: (project.findProperty("QURANAPP_KEYSTORE_PASSWORD") as String?)
    val releaseKeyAlias = System.getenv("QURANAPP_KEY_ALIAS")
        ?: (project.findProperty("QURANAPP_KEY_ALIAS") as String?)
    val releaseKeyPassword = System.getenv("QURANAPP_KEY_PASSWORD")
        ?: (project.findProperty("QURANAPP_KEY_PASSWORD") as String?)

    val keystoreFile = keystorePath?.let { rootProject.file(it) }

    val canSignRelease = keystoreFile != null
            && keystoreFile.exists()
            && !releaseStorePassword.isNullOrBlank()
            && !releaseKeyAlias.isNullOrBlank()
            && !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = keystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
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

            // Code shrinking, resource shrinking, and R8 optimization.
            // Default Android keep rules (proguard-android-optimize.txt) are included.
            optimization {
                enable = true
            }

            isCrunchPngs = false

            // Commit id in META-INF/version-control-info.textproto, for failed RB diffs.
            vcsInfo.include = true

            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }

            proguardFiles("proguard-rules.pro")

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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.glance)
    implementation(libs.glance.appwidget)

    coreLibraryDesugaring(libs.desugaring)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
