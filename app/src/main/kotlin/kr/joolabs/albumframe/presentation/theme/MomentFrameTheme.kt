package kr.joolabs.albumframe.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF090A0C)
val Surface = Color(0xFF15171B)
val SurfaceRaised = Color(0xFF202329)
val Accent = Color(0xFFE6B875)
val TextPrimary = Color(0xFFF7F2E9)
val TextSecondary = Color(0xFFAAA59D)
val Outline = Color(0xFF3A3C41)

private val MomentFrameColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF30220D),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
)

@Composable
fun MomentFrameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MomentFrameColors,
        typography = Typography(),
        content = content,
    )
}
