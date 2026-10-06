package com.ghostgram.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun WebmStickerPlayer(
    filePath: String,
    isPaused: Boolean = false, // 💥
    modifier: Modifier = Modifier
)