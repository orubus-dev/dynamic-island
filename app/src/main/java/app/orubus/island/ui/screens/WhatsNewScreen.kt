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
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Swipe
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
import app.orubus.island.core.IslandBus
import app.orubus.island.core.tr
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
        Icons.Rounded.Grain, tr("Морозные переходы", "Frosted transitions"),
        tr("Смена содержимого идёт через морозную дымку, по стеклу пробегает блик, а масштаб движется пружиной — Liquid Glass теперь живёт и в анимациях.",
            "Content changes pass through a frosted haze, a sheen sweeps the glass, and scale moves on a spring — Liquid Glass now lives in the animations too."),
    ),
    WhatsNewPage(
        Icons.Rounded.Security, tr("Щит против дублей за миллисекунды", "Millisecond duplicate shield"),
        tr("Повторная плашка, пришедшая быстрее 900 мс, гасится сразу — даже если у неё изменился текст. Один пузырь, ни второго, ни третьего.",
            "A repeat banner arriving within 900 ms is killed instantly — even if its text changed. One bubble, no second, no third."),
    ),
    WhatsNewPage(
        Icons.Rounded.HealthAndSafety, tr("Экранный щит и свой adb", "Popup shield with built-in adb"),
        tr("Системные всплывающие плашки — второй источник двойных поп-апов. Щит выключает их в один тап, а встроенный adb (как Shizuku) сам подключается к отладке и выполняет команды.",
            "System heads-up banners are the second source of double pop-ups. The shield turns them off in one tap, and the built-in adb (Shizuku-style) connects to debugging and runs the commands by itself."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.Videocam, tr("У камеры — своя анимация", "Camera gets its own animation"),
        tr("Камера больше не показывает анимацию микрофона: зелёная линза мягко пульсирует, как точка камеры у iPhone. Фонарик тоже больше не притворяется камерой.",
            "The camera no longer borrows the microphone animation: a green lens pulses softly, like the iPhone's camera dot. The flashlight no longer fakes a camera either."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.OpenInFull, tr("Раскрытый остров под контроль", "Expanded island, under control"),
        tr("В «Положении и размере» появились форма и размер большого острова: высота плашек в процентах, скругление больших углов и максимальная ширина.",
            "Position and size now edits the big island too: notice height in percent, the expanded corner radius and the maximum width."),
    ),
    WhatsNewPage(
        Icons.Rounded.Face, tr("Unlock — целиком", "Unlock — the whole word"),
        tr("Малый вырез Face Unlock больше не обрезает слово до «Unloc»: капсула раздвигается ровно настолько, чтобы «Unlocked» поместилось целиком при любом шрифте.",
            "The Face Unlock small notch no longer clips the word to \"Unloc\": the capsule widens just enough for the whole \"Unlocked\" at any font size."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.Settings, tr("Настройки вернулись в док", "Settings is back in the dock"),
        tr("Значок настроек снова стоит рядом с «Домой» и больше не уезжает за экран — док показывает обе вкладки целиком.",
            "The settings icon sits next to Home again instead of sliding off-screen — the dock always shows both tabs."),
    ),
    WhatsNewPage(
        Icons.Rounded.VpnKey, tr("Новые события: VPN и проводные", "New events: VPN and wired"),
        tr("Остров замечает VPN-туннель и проводные наушники, сообщает, когда батарея дошла до 100%, — всё настраивается в «Системных оповещениях».",
            "The island notices VPN tunnels and wired headphones, and reports when the battery hits 100% — all in System Alerts."),
    ),
    WhatsNewPage(
        Icons.Rounded.ScreenRotation, tr("Автоповорот и энергосбережение", "Auto-rotate and battery saver"),
        tr("Два новых события в «Режимах»: включили или выключили автоповорот, включилось или выключилось энергосбережение Android.",
            "Two new events in Modes: auto-rotate turned on or off, and Android's battery saver turned on or off."),
    ),
    WhatsNewPage(
        Icons.Rounded.Swipe, tr("Пружинка из-под камеры", "A spring out of the camera"),
        tr("Смахнули последнюю плашку — остров ныряет в вырез и тут же выпрыгивает обратно пустой капсулой. Смахнутое в шторке уведомление убирает плашку мгновенно.",
            "Swipe the last banner away — the island dives into the notch and springs right back out as an idle capsule. Removing a notification in the shade clears its card instantly too."),
    ),
)

/** Экран «Что нового»: карусель страниц с точками и кнопкой «Попробовать». */
@Composable
fun WhatsNewScreen(onBack: () -> Unit) {
    val list = pages()
    val pager = rememberPagerState(pageCount = { list.size })
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

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
                                .clickable { tryIt(context, i) }
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

private fun tryIt(context: android.content.Context, index: Int) {
    when (index) {
        2 -> IslandBus.post(
            app.orubus.island.core.Notice(
                tr("Щит", "Shield"), tr("дубль заблокирован", "duplicate blocked"),
                Icons.Rounded.Security, app.orubus.island.core.Tints.Blue,
                style = app.orubus.island.core.NoticeStyle.PILL, extraMs = 400,
            ),
        )
        3 -> IslandBus.post(
            app.orubus.island.core.Notice(
                tr("Камера", "Camera"), tr("используется приложением", "in use by an app"),
                Icons.Rounded.Videocam, app.orubus.island.core.Tints.Green,
                style = app.orubus.island.core.NoticeStyle.CAM, extraMs = 600,
            ),
        )
        5 -> IslandBus.post(PrivacyMonitor.demoNotice())
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
