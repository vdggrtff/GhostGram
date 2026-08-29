import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary) // Если плагин версии 8.2+, используем его для KMP-библиотек
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Нативные таргеты под iOS (без генерации framework, чисто библиотека)
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    jvm()

    android {
        namespace = "com.ghostgram.core.network" // Свой уникальный namespace!
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Базовый Ktor и корутины
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        // Движок OkHttp для Android
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        // Движок OkHttp для Desktop (JVM)
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        // Движок Darwin (NSURLSession) для iOS
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
