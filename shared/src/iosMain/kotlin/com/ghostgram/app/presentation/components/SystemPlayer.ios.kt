package com.ghostgram.app.presentation.components

actual fun openVideoInSystemPlayer(filePath: String) {
    println("📱 На ios запустим через Intent: $filePath")
    // TODO: Добавить Android FileProvider Intent
}