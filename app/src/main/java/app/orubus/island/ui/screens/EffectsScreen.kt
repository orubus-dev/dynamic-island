package app.orubus.island.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.orubus.island.core.tr
import app.orubus.island.data.FxRate
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.ChoiceDialog
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.GroupToggle
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile

/**
 * «Эффекты»: живые украшения острова — размытие смены содержимого, эквалайзер,
 * тряска капсулы, свечение обложки и волны оплаты. Каждый выключается отдельно.
 */
@Composable
fun EffectsScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var rateDialog by remember { mutableStateOf(false) }

    Page(tr("Эффекты", "Effects"), onBack,
        subtitle = tr("Живые украшения стоят немного заряда — каждый можно выключить",
            "Live decorations cost a bit of battery — each one can be turned off")) {
        IslandPreview()

        SectionHeader(tr("Содержимое", "Content"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.BlurOn, tr("Размытие смены", "Change blur"),
                tr("Новое содержимое проявляется из размытия (Android 12+)",
                    "New content fades in from a blur (Android 12+)"),
                s.fxBlur, color = Tile.Indigo) { v ->
                SettingsRepo.update { copy(fxBlur = v) }
            }
            GroupItem(Pos.Middle, Icons.Rounded.GraphicEq, tr("Частота украшений", "Decoration rate"), s.fxRate.title,
                onClick = { rateDialog = true }, color = Tile.Gray) { Arrow() }
            GroupToggle(Pos.Last, Icons.Rounded.Vibration, tr("Тряска капсулы", "Capsule shake"),
                tr("Короткое вздрагивание при смене режима звука",
                    "A short shake when the sound mode changes"),
                s.fxShake, color = Tile.Purple) { v ->
                SettingsRepo.update { copy(fxShake = v) }
            }
        }

        SectionHeader(tr("Музыка", "Music"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Equalizer, tr("Эквалайзер", "Equalizer"),
                tr("Полоски пляшут в такт музыке; без неё стоят ровно",
                    "The bars dance to the music; without it they stand still"),
                s.fxEqualizer, color = Tile.Green) { v ->
                SettingsRepo.update { copy(fxEqualizer = v) }
            }
            GroupToggle(Pos.Last, Icons.Rounded.Bolt, tr("Свечение обложки", "Artwork glow"),
                tr("Остров светится цветом обложки, пока играет музыка",
                    "The island glows in the artwork's color while music plays"),
                s.fxGlow, color = Tile.Orange) { v ->
                SettingsRepo.update { copy(fxGlow = v) }
            }
        }

        SectionHeader(tr("Оплата", "Payments"))
        Group {
            GroupToggle(Pos.Single, Icons.Rounded.Nfc, tr("Волны оплаты", "Payment waves"),
                tr("Волны NFC вокруг острова у терминала и вспышка после оплаты",
                    "NFC waves around the island at the reader and a flash after paying"),
                s.fxPay, color = Tile.Blue) { v ->
                SettingsRepo.update { copy(fxPay = v) }
            }
        }

        SectionHeader(tr("Батарея", "Battery"))
        Group {
            GroupToggle(Pos.Single, Icons.Rounded.Bolt, tr("Экономия заряда", "Battery saver"),
                tr("В режиме энергосбережения Android остров обходится без живых эффектов",
                    "In Android's power-saving mode the island goes without live effects"),
                s.batterySaver, color = Tile.Gray) { v ->
                SettingsRepo.update { copy(batterySaver = v) }
            }
        }
    }

    if (rateDialog) {
        ChoiceDialog(tr("Частота украшений", "Decoration rate"), FxRate.entries, s.fxRate, { it.title },
            { v -> SettingsRepo.update { copy(fxRate = v) } }) { rateDialog = false }
    }
}
