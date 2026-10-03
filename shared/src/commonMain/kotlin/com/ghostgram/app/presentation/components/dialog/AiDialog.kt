package com.ghostgram.app.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostBorder
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.ui.theme.GhostTextMuted
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun AiDialog(
    aiApiKeyInput: String,
    setAiDialogOpen: (Boolean) -> Unit,
    updateAiKeyInput: (String) -> Unit,
    saveAiKey: (String) -> Unit,
){

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = { setAiDialogOpen(false) },
        containerColor = GhostCard,
        title = {
            Text(
                "Настройки Gemini AI",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    "Для безопасности и обхода лимитов вы можете использовать свой личный ключ Google Gemini API.",
                    color = GhostTextSecondary, fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = aiApiKeyInput,
                    onValueChange = { updateAiKeyInput(it) },
                    placeholder = { Text("AIzaSy...", color = GhostTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GhostPrimary,
                        unfocusedBorderColor = GhostBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "🔒 GhostGRAM Privacy: ИИ не имеет доступа к вашим E2EE зашифрованным сообщениям. Они фильтруются на уровне устройства.",
                    color = GhostSecureGreen, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { saveAiKey(aiApiKeyInput) }) {
                Text("Сохранить", color = GhostPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { setAiDialogOpen(false) }) {
                Text("Отмена", color = GhostTextSecondary)
            }
        }
    )
}