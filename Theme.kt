package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BattleRoyaleColorScheme = darkColorScheme(
    primary = BR_Orange,
    onPrimary = Color.White,
    primaryContainer = BR_OrangeDark,
    onPrimaryContainer = Color.White,
    secondary = BR_Cyan,
    onSecondary = Color.Black,
    secondaryContainer = BR_CyanDark,
    onSecondaryContainer = Color.White,
    tertiary = BR_Gold,
    onTertiary = Color.Black,
    background = BR_DarkBg,
    onBackground = BR_TextPrimary,
    surface = BR_Surface,
    onSurface = BR_TextPrimary,
    surfaceVariant = BR_SurfaceLight,
    onSurfaceVariant = BR_TextSecondary,
    error = BR_Red,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BattleRoyaleColorScheme,
        typography = Typography,
        content = content
    )
}
