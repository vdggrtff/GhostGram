package com.ghostgram.app.utils.video

import androidx.compose.runtime.Composable

@Composable
expect fun InAppVideoPlayer(
    videoPath: String,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onDismiss: () -> Unit
)

@Composable
expect fun MediaViewerSystemBars()
