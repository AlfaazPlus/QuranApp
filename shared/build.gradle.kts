import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose.compiler)
}

val sharedBuildConfigDir = layout.buildDirectory.dir("generated/sharedBuildConfig/kotlin")

val generateSharedBuildConfig = tasks.register("generateSharedBuildConfig") {
    val versionCode = providers.gradleProperty("quranapp.versionCode")
    val versionName = providers.gradleProperty("quranapp.versionName")

    inputs.property("versionCode", versionCode)
    inputs.property("versionName", versionName)

    outputs.dir(sharedBuildConfigDir)

    doLast {
        val dir = sharedBuildConfigDir.get().asFile.resolve("com/quranapp/android/shared")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            package com.quranapp.android.shared

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
        namespace = "com.quranapp.android.shared"
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
            api(libs.compose.multiplatform.runtime)
            api(libs.compose.multiplatform.foundation)
            api(libs.compose.multiplatform.material3)
            api(libs.compose.multiplatform.ui)
            api(libs.compose.components.resources)

            implementation(libs.kotlinxSerialization)
            implementation(libs.ktor.client.core)
            implementation(libs.androidx.room3.common)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.room3.paging)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.paging.common)
            implementation(libs.okio)
            implementation(libs.coil.core)
            api(libs.datastore.preferences.core)
            implementation(libs.datastore.core.okio)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("com.apollographql.apollo:apollo-api:5.2.0")
            implementation("io.github.optimumcode:json-schema-validator:0.5.5")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.17.1")
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.compose.bom))

            api(libs.androidx.coreKtx)
            api(libs.androidx.appcompat)
            api(libs.androidx.activityKtx)
            api(libs.androidx.activityCompose)

            implementation(libs.compose.material3.windowSizeClass)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.runtime.livedata)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime)
            implementation(libs.lifecycle.service)

            implementation(libs.pagingCompose)
            implementation(libs.compose.navigation)
            implementation(libs.accompanist.permissions)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)

            implementation(libs.androidx.annotation)
            implementation(libs.androidx.webkit)
            implementation(libs.androidx.browser)
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
            kotlin.srcDir(generateSharedBuildConfig)
        }
    }
}

tasks.configureEach {
    if (name == "kspAndroid" || name.startsWith("compileAndroid")) {
        dependsOn(generateSharedBuildConfig)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.quranapp.android.generated.resources"
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    coreLibraryDesugaring(libs.desugaring)
    androidRuntimeClasspath(project.dependencies.platform(libs.compose.bom))
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
