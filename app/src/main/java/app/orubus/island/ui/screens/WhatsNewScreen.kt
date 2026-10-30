package app.orubus.island.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.Controls
import app.orubus.island.core.IslandBus
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.service.PrivacyMonitor
import app.orubus.island.ui.Page
import app.orubus.island.ui.Ui
import app.orubus.island.ui.glassCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class WhatsNewPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val body: String,
    val tryLabel: String? = null,
)

private fun pages() = listOf(
    WhatsNewPage(
        Icons.Rounded.BlurOn, tr("Liquid Glass Monochrome", "Liquid Glass Monochrome"),
        tr("Приложение и остров теперь в монохромном жидком стекле: стеклянные панели, плавающий док и мягкие анимации состояний.",
            "The app and the island are now liquid glass monochrome: glass panels, a floating dock and soft state-change animations."),
    ),
    WhatsNewPage(
        Icons.Rounded.Bolt, tr("Остров не умирает", "The island stays alive"),
        tr("Движок проверяет службы каждые 10 секунд и оживляет их сам. Включите самолечение один раз — и остров всегда с вами.",
            "The engine checks the services every 10 seconds and revives them. Enable self-heal once — the island is always with you."),
        tr("Открыть движок", "Open the engine"),
    ),
    WhatsNewPage(
        Icons.Rounded.Call, tr("Никаких двойных плашек", "No more double pop-ups"),
        tr("Одинаковые уведомления, приходящие дважды, схлопываются в одну плашку в течение 4,5 секунды.",
            "Identical notifications arriving twice collapse into one pop-up within a 4.5-second window."),
    ),
    WhatsNewPage(
        Icons.Rounded.TextFields, tr("Display: стиль и текст", "Display: style and text"),
        tr("Стиль капсулы или выреза, радиусы углов, максимальная ширина, цвет и размер текста, свой шрифт — всё в одном разделе.",
            "Capsule or notch style, corner radii, maximum width, text color and size, your own font — all in one section."),
    ),
    WhatsNewPage(
        Icons.Rounded.Mic, tr("Приватность: микрофон и камера", "Privacy: microphone and camera"),
        tr("Точки у острова показывают, когда приложения пользуются микрофоном или камерой — как индикаторы Android, но заметнее.",
            "Dots by the island show when apps use the microphone or camera — like Android indicators, but easier to notice."),
    ),
    WhatsNewPage(
        Icons.Rounded.WifiTethering, tr("Хотспот в острове", "Hotspot in the notch"),
        tr("Точка доступа включилась — остров покажет плашку. Статус хотспота виден одним взглядом.",
            "The hotspot turned on — the island shows a notice. Hotspot status at a glance."),
    ),
    WhatsNewPage(
        Icons.Rounded.Swipe, tr("Смахнуть — значит убрать", "Swipe to clear notifications"),
        tr("Смахните уведомление с острова вверх — оно исчезнет и из шторки. Настроить можно в «Жестах».",
            "Swipe a notification up on the island — it clears from the shade too. Toggle it in Gestures."),
    ),
    WhatsNewPage(
        Icons.Rounded.Lock, tr("Face Unlock (бета)", "Face Unlock (beta)"),
        tr("Плавная анимация разблокировки в стиле Face ID: раскрытый вид или малый вырез. Ранняя бета — улучшаем.",
            "A smooth Face ID-style unlock animation: expanded view or a small notch. Early beta — improvements underway."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.ContentCopy, tr("Двойной остров", "Dual notch"),
        tr("Идут и музыка, и уведомление? Покажем вторую плашку вместо кружка рядом с первой.",
            "Music and a notification at once? Show a second notch instead of the little bubble."),
    ),
    WhatsNewPage(
        Icons.Rounded.SportsEsports, tr("Игровой режим", "Game mode"),
        tr("Прячьте остров во время игр или оставляйте только важное. Полный контроль — в «Display».",
            "Hide the island during games or keep only what matters. Full control lives in Display."),
    ),
)

/** Экран «Что нового»: карусель страниц с точками и кнопкой «Попробовать». */
@Composable
fun WhatsNewScreen(onBack: () -> Unit) {
    val list = pages()
    val pager = rememberPagerState(pageCount = { list.size })
    val scope = rememberCoroutineScope()

    Page(tr("Что нового", "What's New"), onBack) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(560.dp)
                .glassCard(RoundedCornerShape(30.dp)),
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { i ->
                val p = list[i]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(0.6f))
                    // Телефон с мини-островом
                    PhoneMock(p.icon)
                    Spacer(Modifier.weight(0.6f))
                    Text(p.title, color = Ui.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        p.body, color = Ui.Sub, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    if (p.tryLabel != null) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Ui.Primary.copy(alpha = 0.14f))
                                .border(1.dp, Ui.GlassHi, CircleShape)
                                .clickable { tryIt(i) }
                                .padding(horizontal = 26.dp, vertical = 13.dp),
                        ) {
                            Text(p.tryLabel, color = Ui.Primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            list.indices.forEach { i ->
                val on = pager.currentPage == i
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (on) 22.dp else 7.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(if (on) Ui.Primary else Ui.Divider),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                tr("Назад", "Back"),
                color = Ui.Sub, fontSize = 14.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
            Spacer(Modifier.width(18.dp))
            Text(
                if (pager.currentPage < list.size - 1) tr("Дальше", "Next") else tr("Начать", "Get started"),
                color = Ui.Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        if (pager.currentPage < list.size - 1) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onBack()
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}

private fun tryIt(index: Int) {
    when (index) {
        1 -> SettingsRepo.update { copy(keepAlive = true, autoHeal = true) }
        6 -> Controls.postDemoMessage()
        7 -> IslandBus.post(PrivacyMonitor.demoNotice())
    }
}

/** Мокап телефона с маленьким островом и значком фичи. */
@Composable
private fun PhoneMock(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier
            .size(width = 150.dp, height = 260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF111114))
            .border(2.dp, Ui.GlassHi, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .size(width = 74.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Ui.GlassBorder, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Ui.Text, modifier = Modifier.size(13.dp))
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .size(width = 110.dp, height = 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Ui.GlassBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Ui.Primary, modifier = Modifier.size(20.dp))
        }
    }
}
