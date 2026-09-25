import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.ghostgram.app.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false) // Больше никаких 50 warnings!
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "com.ghostgram.app"
            packageVersion = "0.1.0"
            description = "Modern, Privacy-First Telegram Client"
            copyright = "© 2026 GhostGRAM"
            vendor = "GhostGRAM"
        }
    }
}