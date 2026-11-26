package com.jpbott.bible.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable fun BibleTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFA2D2BD)) else lightColorScheme(primary = Color(0xFF31534B), background = Color(0xFFFFFBF5))
    MaterialTheme(colorScheme = colors, content = content)
}
