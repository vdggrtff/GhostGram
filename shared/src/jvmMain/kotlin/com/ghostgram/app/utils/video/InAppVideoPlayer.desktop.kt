package com.ghostgram.app.utils.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ghostgram.app.utils.openVideoInSystemPlayer

@Composable
actual fun InAppVideoPlayer(
    videoPath: String,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(videoPath) {
        openVideoInSystemPlayer(videoPath)
    }
}

@Composable
actual fun MediaViewerSystemBars(){

}