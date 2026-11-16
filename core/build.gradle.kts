import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
    alias(libs.plugins.androidx.room3)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.quranapp.android.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Same values as :app. :core cannot read the application BuildConfig.
        val appVersionCode = providers.gradleProperty("quranapp.versionCode").get()
        val appVersionName = providers.gradleProperty("quranapp.versionName").get()
        buildConfigField("int", "VERSION_CODE", appVersionCode)
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "VERSION_NAME",
                "\"${providers.gradleProperty("quranapp.versionName").get()}-debug\"",
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.named("main") {
        assets.directories.add(rootProject.file("backup").absolutePath)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.service)

    implementation(libs.androidx.coreKtx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activityKtx)
    implementation(libs.androidx.fragmentKtx)

    implementation(libs.media3ExoPlayer)
    implementation(libs.media3Datasource)
    implementation(libs.media3Database)
    implementation(libs.media3Session)
    implementation(libs.media3UI)

    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.material)
    implementation(libs.guava)

    implementation(libs.kotlinxSerialization)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.commons.lang)
    implementation(libs.commons.compress)
    implementation("com.github.erosb:json-sKema:0.32.0")
    implementation(libs.workManager)
    implementation(libs.dataStore)

    implementation(libs.androidx.room3.common)
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.paging)
    implementation(libs.androidx.room3.paging)
    implementation(libs.androidx.sqlite.bundled)

    implementation(libs.compose.navigation)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
