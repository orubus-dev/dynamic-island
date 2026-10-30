package app.orubus.island.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.orubus.island.R
import app.orubus.island.core.tr

/** Inter — свободный шрифт, близкий к SF Pro на iPhone (лицензия OFL, docs/licenses/Inter-OFL.txt). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Дополнительные свободные шрифты для «Стиля текста» острова (все — OFL, Google Fonts). */
val Nunito = FontFamily(Font(R.font.nunito, FontWeight.Normal), Font(R.font.nunito, FontWeight.Bold))
val Lora = FontFamily(Font(R.font.lora, FontWeight.Normal), Font(R.font.lora, FontWeight.Bold))
val Bayon = FontFamily(Font(R.font.bayon, FontWeight.Normal))

private fun TextStyle.inter() = copy(fontFamily = Inter)

/** Все стили Material 3 — шрифтом Inter. */
val InterTypography: Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.inter(),
        displayMedium = t.displayMedium.inter(),
        displaySmall = t.displaySmall.inter(),
        headlineLarge = t.headlineLarge.inter(),
        headlineMedium = t.headlineMedium.inter(),
        headlineSmall = t.headlineSmall.inter(),
        titleLarge = t.titleLarge.inter(),
        titleMedium = t.titleMedium.inter(),
        titleSmall = t.titleSmall.inter(),
        bodyLarge = t.bodyLarge.inter(),
        bodyMedium = t.bodyMedium.inter(),
        bodySmall = t.bodySmall.inter(),
        labelLarge = t.labelLarge.inter(),
        labelMedium = t.labelMedium.inter(),
        labelSmall = t.labelSmall.inter(),
    )
}

/** Выбор шрифта острова: системный, Inter («Core UI») и подключённые свободные. */
enum class FontChoice(val ru: String, val en: String) {
    SYSTEM("Системный", "System"),
    INTER("Core UI", "Core UI"),
    NUNITO("Nunito", "Nunito"),
    LORA("Lora", "Lora"),
    BAYON("Bayon", "Bayon"),
    MONO("Моно", "Mono");

    val title: String get() = tr(ru, en)

    fun family(): FontFamily = when (this) {
        SYSTEM -> FontFamily.Default
        INTER -> Inter
        NUNITO -> Nunito
        LORA -> Lora
        BAYON -> Bayon
        MONO -> FontFamily.Monospace
    }
}
