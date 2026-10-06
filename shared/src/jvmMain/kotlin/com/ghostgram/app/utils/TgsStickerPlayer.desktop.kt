package com.ghostgram.app.utils

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
actual fun TgsStickerPlayer(
    tgsJson: String,
    modifier: Modifier
) {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.JsonString(tgsJson))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = Compottie.IterateForever,
        isPlaying = true
    )
    val painter = rememberLottiePainter(composition = composition, progress = { progress })
    Image(painter = painter, contentDescription = null, modifier = modifier)
}