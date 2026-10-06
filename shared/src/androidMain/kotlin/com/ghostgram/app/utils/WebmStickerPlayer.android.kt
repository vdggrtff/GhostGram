package com.ghostgram.app.utils

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
actual fun WebmStickerPlayer(
    filePath: String,
    isPaused: Boolean,
    modifier: Modifier
) {
    val cleanPath = if (filePath.startsWith("file://")) filePath.removePrefix("file://") else filePath
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isPaused) {
        try {
            if (isPaused) {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {}
    }

    // Освобождаем память и декодер при уходе с экрана
    DisposableEffect(cleanPath) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                // 💥 ГЛАВНЫЙ СЕКРЕТ СТИКЕРОВ: делаем фон 100% прозрачным!
                isOpaque = false

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        val surfaceObj = Surface(surface)
                        try {
                            val file = File(cleanPath)
                            if (!file.exists()) return

                            val player = MediaPlayer().apply {
                                setDataSource(cleanPath)
                                setSurface(surfaceObj)
                                isLooping = true // 💥 Бесконечный цикл!
                                setVolume(0f, 0f) // Без звука!
                                setOnPreparedListener { start() }
                                prepareAsync()
                            }
                            mediaPlayer = player
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (e: Exception) {}
                        mediaPlayer = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}