package app.orubus.island.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.orubus.island.core.tr
import app.orubus.island.data.ExpandStyle
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.ChoiceDialog
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.GroupSlider
import app.orubus.island.ui.GroupToggle
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile

/**
 * «Анимации»: как раскрывается остров, сколько идут события и переключатель «Уменьшить эффекты».
 */
@Composable
fun AnimScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var styleDialog by remember { mutableStateOf(false) }

    Page(tr("Анимации", "Animations"), onBack,
        subtitle = tr("Всё живёт на превью: меняйте — и смотрите на остров сверху",
            "Everything runs on the preview: change and watch the island above")) {
        IslandPreview(expanded = true)

        SectionHeader(tr("Раскрытие", "Expansion"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.WaterDrop, tr("Стиль раскрытия", "Expansion style"), s.expandStyle.title,
                onClick = { styleDialog = true }, color = Tile.Teal) { Arrow() }
            GroupSlider(Pos.Last, Icons.Rounded.Speed, tr("Длительность", "Duration"), s.durationPercent.toFloat(), 20f..120f,
                "${s.durationPercent}%", color = Tile.Blue) { v ->
                SettingsRepo.update { copy(durationPercent = v.toInt()) }
            }
        }

        SectionHeader(tr("События", "Events"))
        Group {
            GroupSlider(Pos.Single, Icons.Rounded.Bolt, tr("Показ событий", "Event display"),
                s.noticeDurationMs.toFloat(), 1200f..6000f,
                "%.1f с".format(s.noticeDurationMs / 1000f), color = Tile.Orange) { v ->
                SettingsRepo.update { copy(noticeDurationMs = (v.toInt() / 100) * 100) }
            }
        }

        SectionHeader(tr("Если что-то отвлекает", "If anything distracts you"))
        Group {
            GroupToggle(Pos.Single, Icons.Rounded.FiberManualRecord, tr("Уменьшить эффекты", "Reduce effects"),
                tr("Без эквалайзера, «желе» и бликов — остров проще и тише",
                    "No equalizer, jelly or highlights — the island gets simpler and quieter"),
                s.reduceEffects, color = Tile.Gray) { v ->
                SettingsRepo.update { copy(reduceEffects = v) }
            }
        }
    }

    if (styleDialog) {
        ChoiceDialog(tr("Стиль раскрытия", "Expansion style"), ExpandStyle.entries, s.expandStyle, { it.title },
            { v -> SettingsRepo.update { copy(expandStyle = v) } }) { styleDialog = false }
    }
}
