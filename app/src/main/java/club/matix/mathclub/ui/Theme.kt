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

class ThemePreset(val id: String, val name: String, val brand: Color, val brand2: Color, val accent: Color)

/** Same presets as TH_PRESETS in the web app. */
val ThemePresets = listOf(
    ThemePreset("matix", "Matix", Color(0xFF5B46C9), Color(0xFF283F71), Color(0xFFF7A600)),
    ThemePreset("ocean", "Ocean", Color(0xFF0284C7), Color(0xFF075985), Color(0xFF22D3EE)),
    ThemePreset("forest", "Forest", Color(0xFF16A34A), Color(0xFF14532D), Color(0xFFA3E635)),
    ThemePreset("sunset", "Sunset", Color(0xFFE11D48), Color(0xFF7F1D1D), Color(0xFFF59E0B)),
    ThemePreset("grape", "Grape", Color(0xFF7C3AED), Color(0xFF4C1D95), Color(0xFFEC4899)),
    ThemePreset("candy", "Candy", Color(0xFFDB2777), Color(0xFF831843), Color(0xFF38BDF8)),
    ThemePreset("mint", "Mint", Color(0xFF0D9488), Color(0xFF134E4A), Color(0xFF5EEAD4)),
    ThemePreset("slate", "Slate", Color(0xFF475569), Color(0xFF1E293B), Color(0xFF94A3B8))
)

@Composable
fun MatixTheme(dark: Boolean, themeId: String = "matix", content: @Composable () -> Unit) {
    val t = ThemePresets.firstOrNull { it.id == themeId } ?: ThemePresets[0]
    val base = if (dark) Dark else Light
    val scheme = base.copy(
        primary = if (dark) t.brand.copy(alpha = 1f).let { lighten(it) } else t.brand,
        secondary = t.brand2, tertiary = t.accent
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun lighten(c: Color): Color = Color(
    red = c.red + (1f - c.red) * 0.35f, green = c.green + (1f - c.green) * 0.35f, blue = c.blue + (1f - c.blue) * 0.35f
)
