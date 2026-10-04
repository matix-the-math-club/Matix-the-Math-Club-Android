package club.matix.mathclub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF5B46C9)
val Brand2 = Color(0xFF283F71)
val Accent = Color(0xFFF7A600)

private val Light = lightColorScheme(
    primary = Brand, secondary = Brand2, tertiary = Accent,
    background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF7F7F5), onBackground = Color(0xFF2C2C2B), onSurface = Color(0xFF2C2C2B),
    onSurfaceVariant = Color(0xFF7D7A75)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8B7BE8), secondary = Color(0xFF7C93C8), tertiary = Accent,
    background = Color(0xFF191919), surface = Color(0xFF242424),
    surfaceVariant = Color(0xFF262626), onBackground = Color(0xFFE8E6E3), onSurface = Color(0xFFE8E6E3),
    onSurfaceVariant = Color(0xFF9B9894)
)

@Composable
fun MatixTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
