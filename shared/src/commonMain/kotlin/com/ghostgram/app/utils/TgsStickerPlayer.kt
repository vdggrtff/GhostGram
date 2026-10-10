package com.ghostgram.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun TgsStickerPlayer(
    tgsJson: String,
    modifier: Modifier = Modifier
)