package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = ElectricSky,
    onPrimary = Color(0xFF041C2C),
    primaryContainer = Color(0xFF0C354D),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = MintEmerald,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = SolarAmber,
    onTertiary = Color(0xFF2B1700),
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = AlertRose,
    onError = Color(0xFF3F0714),
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFECDD3),
    background = DeepSlateBg,
    onBackground = TextPrimaryDark,
    surface = DeepSlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepSlateSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DeepSlateBorder,
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepCerulean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = DeepEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = WarmAmberDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = DeepRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
    background = CrispLightBg,
    onBackground = TextPrimaryLight,
    surface = CrispLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CrispLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = CrispLightBorder,
    outlineVariant = Color(0xFFE2E8F0)
)

val JeeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JeeRevisionTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme,
        typography = JeeTypography,
        shapes = JeeShapes,
        content = content
    )
}
