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
import java.io.FileInputStream

@Composable
actual fun WebmStickerPlayer(
    filePath: String,
    isPaused: Boolean, // 💥 Сигнатура 1-в-1 совпадает с expect!
    modifier: Modifier
) {
    val cleanPath = remember(filePath) {
        if (filePath.startsWith("file://")) filePath.removePrefix("file://") else filePath
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSurfaceReady by remember { mutableStateOf(false) }

    // Освобождаем память при уходе с экрана
    DisposableEffect(cleanPath) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    // Реакция на паузу (если скроллим)
    LaunchedEffect(isPaused, mediaPlayer, isSurfaceReady) {
        try {
            if (isPaused) {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
            } else {
                if (mediaPlayer != null && isSurfaceReady && mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            }
        } catch (e: Exception) {}
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                // 💥 Прозрачный фон для видео-стикера
                isOpaque = false

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        isSurfaceReady = true
                        try {
                            val file = File(cleanPath)
                            if (!file.exists()) return

                            val player = MediaPlayer().apply {
                                // Надежное чтение через файловый дескриптор
                                FileInputStream(file).use { fis ->
                                    setDataSource(fis.fd)
                                }
                                setSurface(Surface(surface))
                                isLooping = true  // 💥 БЕСКОНЕЧНЫЙ ЦИКЛ!
                                setVolume(0f, 0f) // Без звука

                                setOnPreparedListener {
                                    if (!isPaused) start()
                                }
                                prepareAsync()
                            }
                            mediaPlayer = player
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        isSurfaceReady = false
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