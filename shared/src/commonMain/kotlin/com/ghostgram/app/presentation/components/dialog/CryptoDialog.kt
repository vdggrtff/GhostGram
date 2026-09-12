package com.ghostgram.app.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun CryptoDialog(
    setCryptoDialogOpen: (Boolean) -> Unit
){
    AlertDialog(
        onDismissRequest = { setCryptoDialogOpen(false) },
        containerColor = GhostCard,
        title = {
            Text(
                "CryptoLayer E2EE",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "GhostGRAM использует гибридное сквозное шифрование. \n\n" +
                        "• Обмен ключами: ECDH (SECP256R1)\n" +
                        "• Шифрование данных: AES-256-GCM\n" +
                        "• Обфускация: WordCoder (Стеганография)\n\n" +
                        "Ваши ключи генерируются локально и никогда не покидают устройство.",
                color = GhostTextSecondary, fontSize = 14.sp, lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = { setCryptoDialogOpen(false) }) {
                Text("Понятно", color = GhostPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}