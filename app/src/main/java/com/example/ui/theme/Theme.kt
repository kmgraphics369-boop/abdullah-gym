package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GymDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = PureWhite,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = PureWhite,
    secondary = CrimsonBright,
    onSecondary = PureWhite,
    secondaryContainer = CarbonElevated,
    onSecondaryContainer = PureWhite,
    tertiary = StatusPaidGreen,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = CarbonSurface,
    onSurface = PureWhite,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = CoolSilver,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = CarbonElevated,
    surfaceContainerHigh = CarbonCard,
    outline = GlassBorder,
    outlineVariant = MutedSlate,
    error = StatusDueTodayRed,
    onError = PureWhite,
    errorContainer = StatusDueTodayContainer,
    onErrorContainer = PureWhite
)

val GymShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GymFeeManagerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GymDarkColorScheme,
        typography = GymTypography,
        shapes = GymShapes,
        content = content
    )
}
