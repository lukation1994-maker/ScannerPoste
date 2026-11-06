package it.scannerposte.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF14204F)
val Red = Color(0xFFE2231A)
val Blue = Color(0xFF2D52C4)
val Green = Color(0xFF1F9D55)
val Soft = Color(0xFFF3F5FA)

@Composable
fun ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = Red,
            tertiary = Blue,
            background = Color.White,
            surface = Color.White
        ),
        content = content
    )
}
