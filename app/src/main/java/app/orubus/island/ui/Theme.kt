package app.orubus.island.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass Monochrome — чёрно-белая тема: только оттенки серого,
 * стеклянные панели с тонкой светящейся кромкой. Функциональные цвета
 * (статус движка, ошибки разрешений) — отдельно в [Func].
 */
object Ui {
    val Bg = Color(0xFF0A0A0C)
    val Card = Color(0xFF1A1A1E)
    val CardHi = Color(0xFF232328)
    val IconBg = Color(0xFF2E2E34)
    val Primary = Color(0xFFF5F5F7)
    val OnPrimary = Color(0xFF111113)
    val Text = Color(0xFFF2F2F5)
    val Sub = Color(0xFF9C9CA6)
    val Divider = Color(0xFF3C3C44)
    val Nav = Color(0xFF151518)
    val Pink = Color(0xFFE6E6EB)
    val Outline = Color(0xFF6E6E78)

    /** Стеклянная поверхность: лёгкий вертикальный отблеск + чуть заметная кромка. */
    val Glass = Brush.verticalGradient(
        listOf(Color(0x14FFFFFF), Color(0x07FFFFFF)),
    )
    val GlassBorder = Color(0x1FFFFFFF)
    val GlassHi = Color(0x26FFFFFF)
}

/** Функциональные цвета — только там, где оттенок несёт смысл (статус, ошибка). */
object Func {
    val Ok = Color(0xFF34C759)
    val Warn = Color(0xFFFF9F0A)
    val Err = Color(0xFFFF453A)
}

/**
 * Значки пунктов настроек. Монохром: у каждой категории — свой уровень серого,
 * вместо разноцветных плиток.
 */
object Tile {
    val Blue = Color(0xFF3A3A42)
    val Green = Color(0xFF33333A)
    val Orange = Color(0xFF303038)
    val Red = Color(0xFF36363E)
    val Purple = Color(0xFF32323A)
    val Pink = Color(0xFF34343C)
    val Teal = Color(0xFF2E2E36)
    val Indigo = Color(0xFF383840)
    val Yellow = Color(0xFF323238)
    val Gray = Color(0xFF2A2A30)
}

@Composable
fun IslandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.Primary,
            onPrimary = Ui.OnPrimary,
            secondary = Ui.Primary,
            background = Ui.Bg,
            onBackground = Ui.Text,
            surface = Ui.Card,
            onSurface = Ui.Text,
            surfaceVariant = Ui.IconBg,
            onSurfaceVariant = Ui.Sub,
            surfaceContainerHigh = Ui.Card,
            outline = Ui.Outline,
        ),
        typography = InterTypography,
        content = content,
    )
}
