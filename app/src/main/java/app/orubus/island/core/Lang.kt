package app.orubus.island.core

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import app.orubus.island.data.AppLanguage
import app.orubus.island.data.SettingsRepo
import java.util.Locale

/**
 * Язык приложения и острова: «как в системе», русский, английский, испанский, французский,
 * немецкий, португальский и арабский. Строки пишутся парой прямо в коде: tr("Зарядка", "Charging") —
 * русский и английский живут в самом вызове, остальные языки достаются из таблицы [I18n]
 * по английскому ключу (нет перевода — показывается английский).
 */
object Lang {
    /** Код языка, на котором сейчас остров: "ru", "en", "es", "fr", "de", "pt" или "ar" */
    val code: String
        get() = when (SettingsRepo.current.language) {
            AppLanguage.SYSTEM -> systemLanguage()
            AppLanguage.RU -> "ru"
            AppLanguage.EN -> "en"
            AppLanguage.ES -> "es"
            AppLanguage.FR -> "fr"
            AppLanguage.DE -> "de"
            AppLanguage.PT -> "pt"
            AppLanguage.AR -> "ar"
        }

    /** Прежний вопрос «английский ли сейчас?» — оставлен для старых проверок дат и форматов */
    val en: Boolean get() = code != "ru"

    /** Слева направо ли пишет этот язык (арабский — нет) */
    val rtl: Boolean get() = code == "ar"

    /** Для дат: «пятница, 24 октября», «Friday, October 24», «viernes, 24 de octubre» и так далее */
    val locale: Locale
        get() = when (code) {
            "ru" -> Locale.forLanguageTag("ru")
            "en" -> Locale.ENGLISH
            "es" -> Locale.forLanguageTag("es")
            "fr" -> Locale.forLanguageTag("fr")
            "de" -> Locale.forLanguageTag("de")
            "pt" -> Locale.forLanguageTag("pt-BR")
            "ar" -> Locale.forLanguageTag("ar")
            else -> Locale.ENGLISH
        }

    /**
     * Язык телефона, если он один из наших; иначе английский. Раньше «как в системе» умело только
     * русский и английский — теперь испанский телефон получает испанский остров без настроек.
     */
    private fun systemLanguage(): String {
        val lang = runCatching { Resources.getSystem().configuration.locales[0].language }
            .getOrNull() ?: Locale.getDefault().language
        return if (lang in SUPPORTED) lang else "en"
    }

    val SUPPORTED = setOf("ru", "en", "es", "fr", "de", "pt", "ar")
}

/** Строка на текущем языке. */
fun tr(ru: String, en: String): String = when (Lang.code) {
    "ru" -> ru
    else -> I18n.translate(en) ?: en
}

/**
 * Сменили язык (в настройках, на первом запуске или в системе) — всё внутри рисуется заново
 * уже на новом языке. Состояние снаружи (открытая вкладка и экран) остаётся.
 */
@Composable
fun LangScope(content: @Composable () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    // Язык системы меняется вместе с конфигурацией — чтение подписывает на неё
    LocalConfiguration.current
    key(s.language, Lang.code) { content() }
}
