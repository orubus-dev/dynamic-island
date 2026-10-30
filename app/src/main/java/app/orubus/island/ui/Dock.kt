package app.orubus.island.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.R
import app.orubus.island.core.tr

/**
 * Плавающий док из жидкого стекла с преломлением: главная слева, настройки справа.
 * Стекло — не картинка: линза-градиент, светлая кромка сверху и тёмная снизу,
 * хроматическая каёмка (красный и синий каналы чуть разъезжаются) и медленный блик, как от солнца.
 */
@Composable
fun RefractionDock(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val titles = listOf(tr("Главная", "Home"), tr("Настройки", "Settings"))
    val icons = listOf(ImageVector.vectorResource(R.drawable.ic_tab_island), Icons.Rounded.Settings)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.955f else 1f, spring(dampingRatio = 0.55f, stiffness = 520f), label = "press")
    // Блик переползает по стеклу туда-обратно — медленно и не отвлекая
    val sheen = rememberInfiniteTransition(label = "dockSheen").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(7400, easing = LinearEasing), RepeatMode.Restart),
        label = "sheenX",
    )

    Row(
        Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 12.dp)
            .shadow(24.dp, RoundedCornerShape(34.dp))
            .dockGlass(RoundedCornerShape(34.dp), { sheen.value })
            .scale(pressScale)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        titles.forEachIndexed { i, title ->
            val on = i == selected
            val tint by animateColorAsState(if (on) Ui.OnPrimary else Ui.Text, tween(220), label = "tint$i")
            val labelColor by animateColorAsState(if (on) Ui.OnPrimary else Ui.Sub, tween(220), label = "label$i")
            Row(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (on) Ui.Primary else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(i)
                    }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icons[i], title, tint = tint, modifier = Modifier.size(22.dp))
                TabLabel(title, labelColor, on)
            }
        }
    }
}

/** Подпись вкладки: если не влезает (крупный шрифт Android), чуть уменьшается, а не обрезается.
 * Важно: без fillMaxWidth — иначе первая вкладка забирает всю ширину дока, и «Настройки» уезжает за экран. */
@Composable
private fun TabLabel(text: String, color: Color, bold: Boolean) {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val weight = if (bold) FontWeight.SemiBold else FontWeight.Medium
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val room = constraints.maxWidth.takeIf { it != Constraints.Infinity }
            ?: with(LocalDensity.current) { 400.dp.roundToPx() }
        val size = remember(text, room, weight) {
            var sz = 12f
            while (sz > 8f && measurer.measure(text, base.copy(fontSize = sz.sp, fontWeight = weight), maxLines = 1).size.width > room) {
                sz -= 0.5f
            }
            sz.sp
        }
        Text(text, color = color, fontSize = size, fontWeight = weight, maxLines = 1, softWrap = false, style = base)
    }
}

/**
 * Жидкое стекло дока с преломлением, слои снизу вверх:
 * 1) базовый вертикальный градиент — стекло светлее сверху;
 * 2) линза: у верхней кромки свет загибается внутрь и ярче, у нижней темнеет, как при преломлении;
 * 3) хроматическая каёмка: белая линия, но красный и синий каналы сдвинуты по полпикселя врозь;
 * 4) медленный блик — полоса света переползает по стеклу.
 */
private fun Modifier.dockGlass(shape: RoundedCornerShape, sheenX: () -> Float) = this
    .background(
        Brush.verticalGradient(listOf(Color(0xFF2A2A30), Color(0xFF141417))),
        shape,
    )
    .drawBehind {
        val r = shape.topStart.toPx(size, this).coerceAtLeast(0f)
        // Линза: свет преломляется у кромок
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                startY = 0f,
                endY = size.height,
            ),
            size = size,
            cornerRadius = CornerRadius(r),
        )
        // Хроматическая каёмка: R и B расходятся на полпикселя
        val fringe = 0.75f
        translate(-fringe, 0f) {
            drawRoundRect(color = Color(0x2AFF8080), size = size, cornerRadius = CornerRadius(r), style = Stroke(width = 1.2.dp.toPx()))
        }
        translate(fringe, 0f) {
            drawRoundRect(color = Color(0x2A8080FF), size = size, cornerRadius = CornerRadius(r), style = Stroke(width = 1.2.dp.toPx()))
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.30f),
            size = size,
            cornerRadius = CornerRadius(r),
            style = Stroke(width = 1.dp.toPx()),
        )
        // Блик: мягкая полоса света переползает по стеклу
        val x = sheenX() * size.width
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.055f), Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + size.width * 0.35f, size.height),
            ),
            size = size,
            cornerRadius = CornerRadius(r),
        )
    }
