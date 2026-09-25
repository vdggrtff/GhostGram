import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildConfig)
}

kotlin {
    // Нативные таргеты под iOS (без генерации framework, чисто библиотека)
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    jvm()

    android {
        namespace = "com.ghostgram.core.network"
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
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":domain"))
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

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

buildConfig {
    packageName("com.ghostgram.core.network")

    val supabaseUrlKey = localProperties.getProperty("SUPABASE_URL") ?: ""
    val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: ""

    buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrlKey\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
}
