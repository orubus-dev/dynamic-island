@file:OptIn(ExperimentalLayoutApi::class)

package app.orubus.island.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BorderStyle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LineWeight
import androidx.compose.material.icons.rounded.Texture
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.data.Elasticity
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
import app.orubus.island.ui.Ui

private val capsuleColors = listOf(
    0xFF000000, 0xFF111114, 0xFF1C1C22, 0xFF26233A, 0xFF1A2238, 0xFF102A2A,
    0xFF2B1730, 0xFF3A1A1A, 0xFF2E2A1A, 0xFF3D3D45, 0xFFF2F2F7, 0xFFC6C3F7,
)

private val outlineColors = listOf(
    0xFF3A3A40, 0xFF8E8E93, 0xFFFFFFFF, 0xFFC6C3F7, 0xFF0A84FF, 0xFF64D2FF,
    0xFF34C759, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF453A, 0xFFFF2D92, 0xFFBF5AF2,
)

@Composable
fun ColorsScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var elasticityDialog by remember { mutableStateOf(false) }
    Page(tr("Цвет и стекло", "Color & glass"), onBack) {
        IslandPreview()

        SectionHeader(tr("Цвет", "Color"))
        Swatches(capsuleColors, s.color) { c -> SettingsRepo.update { copy(color = c) } }

        SectionHeader(tr("Контур", "Outline"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.BorderStyle, tr("Контур", "Outline"),
                tr("Тонкая обводка вокруг капсулы", "A thin line around the capsule"),
                s.outline, color = Tile.Gray) { v ->
                SettingsRepo.update { copy(outline = v) }
            }
            GroupToggle(Pos.Middle, Icons.Rounded.Image, tr("Цвет из обложки", "Color from artwork"),
                tr("Пока играет музыка, контур окрашивается в цвет обложки", "While music plays, the outline takes the artwork's color"),
                s.outlineFromArt, color = Tile.Pink) { v -> SettingsRepo.update { copy(outlineFromArt = v, outline = outline || v) } }
            GroupSlider(Pos.Last, Icons.Rounded.LineWeight, tr("Толщина", "Thickness"), s.outlineWidth, 0.5f..4f,
                "%.1f dp".format(s.outlineWidth), color = Tile.Gray) { v ->
                SettingsRepo.update { copy(outlineWidth = (v * 2).toInt() / 2f) }
            }
        }
        Swatches(outlineColors, s.outlineColor) { c -> SettingsRepo.update { copy(outlineColor = c, outline = true) } }

        SectionHeader(tr("Тень", "Shadow"))
        GroupToggle(Pos.Single, Icons.Rounded.Layers, tr("Тень", "Shadow"),
            tr("Мягкая тень под капсулой — заметна на светлых экранах", "A soft shadow under the capsule — visible on light screens"),
            s.shadow, color = Tile.Indigo) { v ->
            SettingsRepo.update { copy(shadow = v) }
        }

        SectionHeader(tr("Liquid Glass", "Liquid Glass"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.WaterDrop, tr("Жидкое стекло", "Liquid Glass"),
                tr("Полупрозрачное стекло с бликами вместо чёрной заливки", "Translucent glass with highlights instead of a black fill"),
                s.liquidGlass, color = Tile.Teal) { v ->
                SettingsRepo.update { copy(liquidGlass = v) }
            }
            GroupSlider(Pos.Middle, Icons.Rounded.Contrast, tr("Прозрачность", "Glass opacity"), s.glassOpacity.toFloat(), 30f..95f,
                "${s.glassOpacity}%", color = Tile.Gray) { v ->
                SettingsRepo.update { copy(glassOpacity = v.toInt()) }
            }
            GroupToggle(Pos.Middle, Icons.Rounded.Texture, tr("Размытие фона", "Blur behind"),
                tr("При раскрытии фон за стеклом размывается (Android 12+)", "The background behind the glass blurs when expanded (Android 12+)"),
                s.glassBlurBehind, color = Tile.Indigo) { v ->
                SettingsRepo.update { copy(glassBlurBehind = v) }
            }
            GroupToggle(Pos.Last, Icons.Rounded.Water, tr("Жидкое раскрытие", "Liquid expansion"),
                tr("Новое содержимое вырастает из прежнего вместе с островом и проявляется из размытия",
                    "New content grows out of the old together with the island, fading in from blur"),
                s.liquidExpand, color = Tile.Blue) { v ->
                SettingsRepo.update { copy(liquidExpand = v) }
            }
        }

        SectionHeader(tr("Упругость", "Elasticity"))
        Group {
            GroupItem(Pos.Single, Icons.Rounded.Animation, tr("Упругость раскрытия", "Expansion bounce"), s.elasticity.title,
                onClick = { elasticityDialog = true }, color = Tile.Orange) { Arrow() }
        }

        Text(
            tr("Цвет кнопок «Ответить» и «Пометить прочитанным» — в «Функции» → «Уведомления».",
                "The color of the Reply and Mark as read buttons is in Features → Notifications."),
            color = Ui.Sub, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 8.dp),
        )
    }

    if (elasticityDialog) {
        ChoiceDialog(tr("Упругость раскрытия", "Expansion bounce"), Elasticity.entries, s.elasticity, { it.title },
            { v -> SettingsRepo.update { copy(elasticity = v) } }) { elasticityDialog = false }
    }
}

/** Кружки цветов в карточке: выбранный — с галочкой. */
@Composable
fun Swatches(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Ui.Card)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        colors.forEach { c ->
            val color = Color(c)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, if (c == selected) Ui.Primary else Ui.Divider, CircleShape)
                    .clickable { onPick(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (c == selected) {
                    Icon(Icons.Rounded.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White)
                }
            }
        }
    }
}
