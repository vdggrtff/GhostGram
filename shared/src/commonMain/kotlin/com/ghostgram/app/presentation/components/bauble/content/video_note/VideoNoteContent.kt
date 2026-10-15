package com.ghostgram.app.presentation.components.bauble.content.video_note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.utils.openVideoInSystemPlayer
import entity.Message

@Composable
fun VideoNoteContent(
    message: Message,
    onLongClick: () -> Unit
) {
    val thumbModel = message.photoPath?.let { if (it.startsWith("/")) "file://$it" else it }
    val videoPath = message.fileName

    // 💥 СТЕЙТ ФОРМЫ (Клик двумя пальцами или двойной тап переключает форму!)
    var currentShapeType by remember { mutableStateOf(VideoNoteShapeType.CIRCLE) }
    val activeShape = VideoNoteShapes.getShape(currentShapeType)

    Box(
        modifier = Modifier
            .size(200.dp) // Фиксированный размер кружочка как в Telegram
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Одиночный тап: запускаем видеоплеер
                        if (!videoPath.isNullOrBlank()) {
                            openVideoInSystemPlayer(videoPath)
                        }
                    },
                    onDoubleTap = {
                        // 💥 ДВОЙНОЙ ТАП: ЦИКЛИЧЕСКИ МЕНЯЕТ ФОРМУ КРУЖОЧКА!
                        currentShapeType = when (currentShapeType) {
                            VideoNoteShapeType.CIRCLE -> VideoNoteShapeType.SQUIRCLE
                            VideoNoteShapeType.SQUIRCLE -> VideoNoteShapeType.HEXAGON
                            VideoNoteShapeType.HEXAGON -> VideoNoteShapeType.DIAMOND
                            VideoNoteShapeType.DIAMOND -> VideoNoteShapeType.CIRCLE
                        }
                    },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 💥 1. САМО ВИДЕОСООБЩЕНИЕ (Обрезано по выбранной кастомной форме!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp) // Отступ под неоновое кольцо
                .clip(activeShape)
                .background(Color(0xFF1E2330)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbModel != null) {
                AsyncImage(
                    model = thumbModel,
                    contentDescription = "Кружочек",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("📹", fontSize = 36.sp)
            }

            // Полупрозрачный оверлей с кнопкой Play
            if (videoPath.isNullOrBlank() && !message.isSending) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp).padding(start = 2.dp)
                    )
                }
            }
        }

        // 💥 2. ДИНАМИЧЕСКОЕ КОЛЬЦО ПРОГРЕССА ПО КРАЮ (Canvas Progress Ring)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = GhostPrimary,
                startAngle = -90f,
                sweepAngle = 360f, // Полный круг окантовки
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 💥 3. КАПСУЛА С ВРЕМЕНЕМ В НИЖНЕЙ ЧАСТИ
        val durationText = message.fileExtraInfo
        if (!durationText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}