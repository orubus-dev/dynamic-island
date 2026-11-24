package app.orubus.island.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.data.AppLanguage
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.RadioDot
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile
import app.orubus.island.ui.Ui

/**
 * Язык приложения и острова. Показывается на первом запуске — до всех настроек — и потом
 * живёт в «О приложении» → «Язык». Восемь языков: как в системе, русский, английский,
 * испанский, французский, немецкий, португальский и арабский.
 */
@Composable
fun LanguageScreen(onBack: (() -> Unit)?) {
    val s by SettingsRepo.state.collectAsState()

    Page(tr("Язык", "Language"), onBack,
        subtitle = tr("На каком языке остров и его настройки. Можно менять когда угодно",
            "The language of the island and its settings. You can change it anytime")) {
        SectionHeader(tr("Язык", "Language"))
        Group {
            AppLanguage.entries.forEachIndexed { i, lang ->
                GroupItem(
                    pos = when (i) {
                        0 -> Pos.First
                        AppLanguage.entries.size - 1 -> Pos.Last
                        else -> Pos.Middle
                    },
                    icon = Icons.Rounded.Language,
                    title = lang.native,
                    subtitle = when (lang) {
                        AppLanguage.SYSTEM -> tr("Следует за языком телефона", "Follows the phone's language")
                        else -> null
                    },
                    color = Tile.Blue,
                    onClick = { SettingsRepo.update { copy(language = lang) } },
                ) { RadioDot(s.language == lang) }
            }
        }
    }
}

/**
 * Первый запуск: выбор языка на весь экран — до острова и настроек. Ничего не спрашивает дважды:
 * после обновления приложения выбор не возвращается (настройки уже сохранены).
 */
@Composable
fun StartupLanguage(onDone: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var picked by remember { mutableStateOf<AppLanguage?>(null) }
    val current = picked ?: s.language

    Box(Modifier.fillMaxSize().background(Ui.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(38.dp))
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Ui.IconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Language, null, tint = Ui.Text, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(tr("Выберите язык", "Choose your language"), color = Ui.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Им будет говорить остров. Потом можно поменять в настройках",
                    "This is the language the island speaks. You can change it later in settings"),
                color = Ui.Sub, fontSize = 15.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(22.dp))
            Group {
                AppLanguage.entries.forEachIndexed { i, lang ->
                    GroupItem(
                        pos = when (i) {
                            0 -> Pos.First
                            AppLanguage.entries.size - 1 -> Pos.Last
                            else -> Pos.Middle
                        },
                        icon = Icons.Rounded.Language,
                        title = lang.native,
                        color = Tile.Blue,
                        onClick = { picked = lang; SettingsRepo.update { copy(language = lang) } },
                    ) { RadioDot(current == lang) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Ui.Primary)
                    .clickable(enabled = picked != null, onClick = onDone)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tr("Продолжить", "Continue"),
                    color = Ui.OnPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                tr("«Как в системе» — остров говорит на языке телефона, а если он незнаком — по-английски",
                    "“System default” — the island speaks the phone's language, or English if it's an unfamiliar one"),
                color = Ui.Sub, fontSize = 12.sp, lineHeight = 16.sp,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
