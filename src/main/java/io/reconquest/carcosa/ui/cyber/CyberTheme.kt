package io.reconquest.carcosa.ui.cyber

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

data class CyberPalette(
    val background: Color = Color(0xFF030406),
    val surface: Color = Color(0xFF070A0D),
    val panel: Color = Color(0xFF0B1014),
    val panelHot: Color = Color(0xFF111923),
    val panelBorder: Color = Color(0xFF27323A),
    val text: Color = Color(0xFFE6F1F5),
    val muted: Color = Color(0xFF6E7D86),
    val amber: Color = Color(0xFFFFB000),
    val cyan: Color = Color(0xFF00E5FF),
    val acid: Color = Color(0xFFB6FF00),
    val red: Color = Color(0xFFFF355E),
    val grid: Color = Color(0xFF122027),
)

val LocalCyberPalette = staticCompositionLocalOf { CyberPalette() }

private val CyberColorScheme = darkColorScheme(
    primary = Color(0xFFFFB000),
    onPrimary = Color(0xFF030406),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF030406),
    tertiary = Color(0xFFB6FF00),
    onTertiary = Color(0xFF030406),
    error = Color(0xFFFF355E),
    onError = Color(0xFF030406),
    background = Color(0xFF030406),
    onBackground = Color(0xFFE6F1F5),
    surface = Color(0xFF070A0D),
    onSurface = Color(0xFFE6F1F5),
    surfaceVariant = Color(0xFF0B1014),
    onSurfaceVariant = Color(0xFF9AA9B2),
    outline = Color(0xFF27323A),
)

private val Mono = FontFamily.Monospace

private val CyberTypography = Typography(
    displayLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 38.sp),
    displayMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    displaySmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    headlineSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    titleSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    bodyLarge = TextStyle(fontFamily = Mono, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Mono, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 11.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 10.sp),
)

@Composable
fun CyberTheme(content: @Composable () -> Unit) {
    val palette = CyberPalette()
    CompositionLocalProvider(LocalCyberPalette provides palette) {
        MaterialTheme(
            colorScheme = CyberColorScheme,
            typography = CyberTypography,
            content = content,
        )
    }
}
