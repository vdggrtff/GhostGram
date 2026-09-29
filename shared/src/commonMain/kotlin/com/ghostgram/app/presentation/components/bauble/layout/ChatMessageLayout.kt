package com.ghostgram.app.presentation.components.bauble.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * 💥 Кастомный алгоритм верстки текста и времени как в Telegram:
 * Однострочный текст — время справа в одну линию.
 * Многострочный текст — время под текстом справа с выравниванием.
 */
@Composable
fun ChatMessageLayout(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    time: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            text()
            time()
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables[0].measure(constraints.copy(minWidth = 0))
        val timePlaceable = measurables[1].measure(Constraints())

        val spacing = 7.dp.roundToPx()
        val isSingleLine = textPlaceable.height <= 28.dp.roundToPx()
        val fitsOnSingleLine = isSingleLine && (textPlaceable.width + spacing + timePlaceable.width <= constraints.maxWidth)

        if (fitsOnSingleLine) {
            val totalWidth = textPlaceable.width + spacing + timePlaceable.width
            val totalHeight = textPlaceable.height
            val timeY = (totalHeight - timePlaceable.height - 2.dp.roundToPx()).coerceAtLeast(0)

            layout(totalWidth, totalHeight) {
                textPlaceable.placeRelative(0, 0)
                timePlaceable.placeRelative(textPlaceable.width + spacing, timeY)
            }
        } else {
            val totalWidth = maxOf(textPlaceable.width, timePlaceable.width)
            val totalHeight = textPlaceable.height + timePlaceable.height + 2.dp.roundToPx()

            layout(totalWidth, totalHeight) {
                textPlaceable.placeRelative(0, 0)
                timePlaceable.placeRelative(totalWidth - timePlaceable.width, textPlaceable.height + 2.dp.roundToPx())
            }
        }
    }
}