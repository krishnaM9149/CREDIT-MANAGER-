package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BerryPinkDarkPrimary,
    onPrimary = OnBerryPinkDarkPrimary,
    primaryContainer = BerryDarkContainer,
    onPrimaryContainer = OnBerryDarkContainer,
    secondary = CaramelDarkSecondary,
    onSecondary = OnCaramelDarkSecondary,
    secondaryContainer = TruffleDarkContainer,
    onSecondaryContainer = OnTruffleDarkContainer,
    tertiary = HoneyDarkTertiary,
    onTertiary = OnHoneyDarkTertiary,
    tertiaryContainer = HoneyDarkContainer,
    onTertiaryContainer = OnHoneyDarkContainer,
    background = DarkCocoaBackground,
    onBackground = OnDarkCocoaBackground,
    surface = DarkCocoaSurface,
    onSurface = OnDarkCocoaSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = BerryCocoaPrimary,
    onPrimary = OnBerryCocoaPrimary,
    primaryContainer = BerryRoseContainer,
    onPrimaryContainer = OnBerryRoseContainer,
    secondary = TruffleSecondary,
    onSecondary = OnTruffleSecondary,
    secondaryContainer = CaramelSecondaryContainer,
    onSecondaryContainer = OnCaramelSecondaryContainer,
    tertiary = HoneyGoldTertiary,
    onTertiary = OnHoneyGoldTertiary,
    tertiaryContainer = HoneyCreamContainer,
    onTertiaryContainer = OnHoneyCreamContainer,
    background = CreamBackground,
    onBackground = OnCreamBackground,
    surface = AlmondSurface,
    onSurface = OnAlmondSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline
)

val SweetLedgerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SweetLedgerShapes,
        content = content
    )
}
