import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvm()
    listOf(iosArm64(), iosSimulatorArm64())

    android {
        namespace = "com.ghostgram.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)

            // ВНИМАНИЕ! Склеиваем слои:
            implementation(project(":domain"))
            implementation(project(":core:network"))
            implementation(project(":core:tdlib"))
            implementation(libs.koin.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            // implementation(project(":core:database")) // Раскоментим, когда добавим Room
        }
    }
}