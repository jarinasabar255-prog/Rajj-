package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FinorixDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = SlateElevated,
    onPrimaryContainer = ElectricCyan,
    secondary = BullishEmerald,
    onSecondary = ObsidianVoid,
    secondaryContainer = BullishContainer,
    onSecondaryContainer = BullishEmerald,
    tertiary = AmberQuant,
    onTertiary = ObsidianVoid,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberQuant,
    error = BearishCrimson,
    onError = Color.White,
    errorContainer = BearishContainer,
    onErrorContainer = BearishCrimson,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = SlateElevated
)

val FinorixShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FinorixProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FinorixDarkColorScheme,
        typography = FinorixTypography,
        shapes = FinorixShapes,
        content = content
    )
}
