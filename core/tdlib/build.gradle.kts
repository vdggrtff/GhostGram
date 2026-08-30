import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    // iOS таргеты
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    jvm() // Desktop

    android {
        namespace = "com.ghostgram.core.tdlib"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation("net.java.dev.jna:jna:5.19.1@aar")
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation("net.java.dev.jna:jna:5.19.1")
        }
    }
}