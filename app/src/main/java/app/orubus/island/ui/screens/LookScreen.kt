package app.orubus.island.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupSlider
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile

/**
 * «Размер с активностью»: насколько капсула с музыкой/таймером толще и шире спокойной.
 */
@Composable
fun LookScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()

    Page(tr("Размер с активностью", "Size with activity"), onBack,
        subtitle = tr("Активный остров чуть заметнее — так лучше видно таймер и музыку",
            "The active island is a bit bolder — easier to see the timer and music")) {
        IslandPreview()

        SectionHeader(tr("Активная капсула", "Active capsule"))
        Group {
            GroupSlider(Pos.First, Icons.Rounded.UnfoldMore, tr("Толще на", "Thicker by"),
                s.activeExtra.toFloat(), 0f..20f, "${s.activeExtra} dp", color = Tile.Teal) { v ->
                SettingsRepo.update { copy(activeExtra = v.toInt()) }
            }
            GroupSlider(Pos.Last, Icons.Rounded.AspectRatio, tr("Шире на", "Wider by"),
                s.activeWidth.toFloat(), 0f..120f, "${s.activeWidth} dp", color = Tile.Blue) { v ->
                SettingsRepo.update { copy(activeWidth = v.toInt()) }
            }
        }
    }
}
