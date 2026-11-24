package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TradeSemanticColors(
    val profit: Color,
    val profitContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val indicatorFast: Color,
    val indicatorSlow: Color,
    val bollingerLine: Color,
    val chartGrid: Color
)

private val DarkTradeSemanticColors = TradeSemanticColors(
    profit = EmeraldProfitDark,
    profitContainer = EmeraldProfitContainerDark,
    loss = CoralLossDark,
    lossContainer = CoralLossContainerDark,
    warning = AmberSignalDark,
    warningContainer = AmberSignalContainerDark,
    indicatorFast = CyanIndicatorDark,
    indicatorSlow = AmberSignalDark,
    bollingerLine = VioletAccentDark,
    chartGrid = Color(0xFF1F2B42)
)

private val LightTradeSemanticColors = TradeSemanticColors(
    profit = EmeraldProfitLight,
    profitContainer = EmeraldProfitContainerLight,
    loss = CoralLossLight,
    lossContainer = CoralLossContainerLight,
    warning = AmberSignalLight,
    warningContainer = AmberSignalContainerLight,
    indicatorFast = CyanIndicatorLight,
    indicatorSlow = AmberSignalLight,
    bollingerLine = VioletAccentLight,
    chartGrid = Color(0xFFE2E8F0)
)

val LocalTradeColors = staticCompositionLocalOf { DarkTradeSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldProfitDark,
    onPrimary = Color(0xFF002419),
    primaryContainer = EmeraldProfitContainerDark,
    onPrimaryContainer = EmeraldProfitDark,
    secondary = CyanIndicatorDark,
    onSecondary = Color(0xFF002033),
    secondaryContainer = Color(0xFF0C2D48),
    onSecondaryContainer = CyanIndicatorDark,
    tertiary = AmberSignalDark,
    onTertiary = Color(0xFF2B1B00),
    tertiaryContainer = AmberSignalContainerDark,
    onTertiaryContainer = AmberSignalDark,
    error = CoralLossDark,
    onError = Color(0xFF33000B),
    errorContainer = CoralLossContainerDark,
    onErrorContainer = CoralLossDark,
    background = ObsidianBg,
    onBackground = ObsidianOnSurface,
    surface = ObsidianSurface,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = ObsidianOnSurfaceMuted,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldProfitLight,
    onPrimary = Color.White,
    primaryContainer = EmeraldProfitContainerLight,
    onPrimaryContainer = Color(0xFF003824),
    secondary = CyanIndicatorLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = AmberSignalLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberSignalContainerLight,
    onTertiaryContainer = Color(0xFF78350F),
    error = CoralLossLight,
    onError = Color.White,
    errorContainer = CoralLossContainerLight,
    onErrorContainer = Color(0xFF881337),
    background = PaperBg,
    onBackground = PaperOnSurface,
    surface = PaperSurface,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperSurfaceElevated,
    onSurfaceVariant = PaperOnSurfaceMuted,
    outline = PaperOutline,
    outlineVariant = Color(0xFFE2E8F0)
)

val TradeLabShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TradeLabTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tradeColors = if (darkTheme) DarkTradeSemanticColors else LightTradeSemanticColors

    CompositionLocalProvider(LocalTradeColors provides tradeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = TradeLabShapes,
            content = content
        )
    }
}
