package com.ghostgram.app.utils.video

import androidx.compose.runtime.Composable

@Composable
expect fun InAppVideoPlayer(
    videoPath: String,
    controlsVisible: Boolean,
    playbackSpeed: Float = 1f,
    isLooping: Boolean = false,
    onToggleControls: () -> Unit,
    onDismiss: () -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    onTransformGestureChanged: (Boolean) -> Unit = {}
)

@Composable
expect fun MediaViewerSystemBars()
