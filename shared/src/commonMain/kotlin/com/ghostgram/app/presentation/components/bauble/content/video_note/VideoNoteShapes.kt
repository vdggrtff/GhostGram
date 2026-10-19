package com.ghostgram.app.presentation.components.bauble.content.video_note

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Outline.Generic
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class VideoNoteShapeType {
    CIRCLE,   // 🟢 Классический круг
    SQUIRCLE, // 🟩 Сквиркл (скругленный суперэллипс)
    HEXAGON,  // ⬡ Шестиугольник
    DIAMOND   // 💎 Алмаз / Ромб
}

object VideoNoteShapes {

    val Squircle = RoundedCornerShape(38.dp)

    // ⬡ Математика 6-угольника:
    val Hexagon = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.25f)
                lineTo(w, h * 0.75f)
                lineTo(w * 0.5f, h)
                lineTo(0f, h * 0.75f)
                lineTo(0f, h * 0.25f)
                close()
            }
            return Generic(path)
        }
    }

    // 💎 Математика алмаза / ромба со скруглением:
    val Diamond = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val path = Path().apply {
                val w = size.width
                val h = size.height
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.5f)
                lineTo(w * 0.5f, h)
                lineTo(0f, h * 0.5f)
                close()
            }
            return Generic(path)
        }
    }

    fun getShape(type: VideoNoteShapeType): Shape = when (type) {
        VideoNoteShapeType.CIRCLE -> CircleShape
        VideoNoteShapeType.SQUIRCLE -> Squircle
        VideoNoteShapeType.HEXAGON -> Hexagon
        VideoNoteShapeType.DIAMOND -> Diamond
    }
}