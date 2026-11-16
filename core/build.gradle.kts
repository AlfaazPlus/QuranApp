import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
    alias(libs.plugins.androidx.room3)
}

val coreBuildConfigDir = layout.buildDirectory.dir("generated/coreBuildConfig/kotlin")

val generateCoreBuildConfig = tasks.register("generateCoreBuildConfig") {
    val versionCode = providers.gradleProperty("quranapp.versionCode")
    val versionName = providers.gradleProperty("quranapp.versionName")

    inputs.property("versionCode", versionCode)
    inputs.property("versionName", versionName)

    outputs.dir(coreBuildConfigDir)

    doLast {
        val dir = coreBuildConfigDir.get().asFile.resolve("com/quranapp/android/core")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            package com.quranapp.android.core

            object BuildConfig {
                @JvmField
                val VERSION_CODE: Int = ${versionCode.get()}

                @JvmField
                var VERSION_NAME: String = "${versionName.get()}"

                @JvmField
                var DEBUG: Boolean = false
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    // AGP 9 cannot combine com.android.library with androidTarget().
    // This is the Android-only target. Apple targets stay behind the Mac gate.
    android {
        namespace = "com.quranapp.android.core"
        compileSdk = 37
        minSdk = 24
        withJava()
        enableCoreLibraryDesugaring = true

        androidResources {
            enable = true
        }

        optimization {
            consumerKeepRules.file("consumer-rules.pro")
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withHostTest { }
    }

    if (System.getProperty("os.name").startsWith("Mac")) {
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinxSerialization)
            implementation(libs.ktor.client.core)
            implementation(libs.androidx.room3.common)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.room3.paging)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.paging.common)
            implementation(libs.okio)
            implementation(libs.datastore.preferences.core)
            implementation(libs.datastore.core.okio)
        }

        androidMain.dependencies {
            implementation(libs.lifecycle.viewmodel)
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

            implementation(libs.material)
            implementation(libs.guava)

            implementation(libs.ktor.client.okhttp)

            implementation(libs.commons.lang)
            implementation(libs.commons.compress)
            implementation("com.github.erosb:json-sKema:0.32.0")
            implementation(libs.workManager)
            implementation(libs.dataStore)

            implementation(libs.paging)
        }

        named("androidHostTest") {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }

        named("androidMain") {
            kotlin.srcDir(generateCoreBuildConfig)
        }
    }
}

tasks.configureEach {
    if (name == "kspAndroid" || name.startsWith("compileAndroid")) {
        dependsOn(generateCoreBuildConfig)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    coreLibraryDesugaring(libs.desugaring)
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension>("androidComponents") {
    onVariants { variant ->
        variant.sources.assets?.addStaticSourceDirectory(rootProject.file("backup").absolutePath)
    }
}
