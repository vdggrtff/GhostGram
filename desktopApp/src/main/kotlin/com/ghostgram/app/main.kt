package com.ghostgram.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ghostgram.app.di.initKoin

fun main() = application {

    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "GhostGram",
    ) {
        App()
    }
}