package com.ghostgram.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ghostgram.app.di.initKoin

fun main() = application {

    initKoin(appStoragePath = System.getProperty("user.home"))

    Window(
        onCloseRequest = {
            exitApplication()
            kotlin.system.exitProcess(0)
        },
        title = "GhostGram",
    ) {
        App()
    }
}