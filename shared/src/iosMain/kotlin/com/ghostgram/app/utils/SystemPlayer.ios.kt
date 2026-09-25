package com.ghostgram.app.utils

actual fun openVideoInSystemPlayer(filePath: String) {
    println("📱 На ios запустим через Intent: $filePath")
    // TODO: Добавить Android FileProvider Intent
}