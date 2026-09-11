package com.ghostgram.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ghostgram.app.ui.theme.GhostGramTheme

@Composable
@Preview
fun App() {
    GhostGramTheme {
        MainLayout()
    }
}