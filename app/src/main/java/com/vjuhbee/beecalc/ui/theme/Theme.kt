package com.vjuhbee.beecalc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Одна светлая тема: приложение используется на улице при ярком свете,
// предсказуемый высокий контраст важнее тёмного режима (SPEC.md §2, §8).
private val LightColorScheme = lightColorScheme(
    primary = HoneyDark,
    onPrimary = SurfaceLight,
    primaryContainer = HoneyContainer,
    onPrimaryContainer = OnHoneyContainer,
    secondary = HoneyAmber,
    onSecondary = TextDark,
    background = SurfaceLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark
)

@Composable
fun BeeCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
