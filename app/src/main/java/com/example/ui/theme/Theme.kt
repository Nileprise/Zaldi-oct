package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ZaldiDarkColorScheme = darkColorScheme(
    primary = ZaldiAmber,
    onPrimary = Color(0xFF0B0F17),
    primaryContainer = Color(0xFF3B2806),
    onPrimaryContainer = ZaldiAmberLight,
    secondary = ZaldiEmerald,
    onSecondary = Color(0xFF06281E),
    secondaryContainer = Color(0xFF08382A),
    onSecondaryContainer = ZaldiEmeraldLight,
    tertiary = ZaldiSky,
    onTertiary = Color(0xFF072638),
    tertiaryContainer = Color(0xFF0C3B56),
    onTertiaryContainer = Color(0xFFBAE6FD),
    error = ZaldiCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF451212),
    onErrorContainer = Color(0xFFFCA5A5),
    background = ZaldiObsidian,
    onBackground = ZaldiTextPrimaryDark,
    surface = ZaldiSlate,
    onSurface = ZaldiTextPrimaryDark,
    surfaceVariant = ZaldiSurfaceVariant,
    onSurfaceVariant = ZaldiTextSecondaryDark,
    outline = ZaldiOutlineDark
)

private val ZaldiLightColorScheme = lightColorScheme(
    primary = ZaldiAmberDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = ZaldiEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    error = ZaldiCrimson,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = ZaldiDayBackground,
    onBackground = ZaldiTextPrimaryLight,
    surface = ZaldiDaySurface,
    onSurface = ZaldiTextPrimaryLight,
    surfaceVariant = ZaldiDaySurfaceVariant,
    onSurfaceVariant = ZaldiTextSecondaryLight,
    outline = ZaldiOutlineLight
)

val ZaldiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZaldiDarkColorScheme else ZaldiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ZaldiShapes,
        content = content
    )
}
