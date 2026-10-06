package com.ghostgram.app.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun WebmStickerPlayer(filePath: String,  isPaused: Boolean, modifier: Modifier) {
    // На ПК видео-стикер пока показывается превьюшкой
    Box(modifier = modifier)
}