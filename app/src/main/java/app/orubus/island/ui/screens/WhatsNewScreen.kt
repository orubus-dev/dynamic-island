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
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VpnKey
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
        Icons.Rounded.Bookmarks, tr("Смахните вверх — значок со счётчиком", "Swipe up — an icon with a badge"),
        tr("Уведомления больше не исчезают бесследно: смахнули плашку вверх — она превращается в значок последнего уведомления со счётчиком справа. Свайп вбок листает стопку, и остров не гаснет; тап слева открывает быстрые кнопки, тап по счётчику достаёт уведомление.",
            "Notifications no longer vanish without a trace: swipe a banner up and it turns into an icon of the last notification with a count badge. Swipe sideways to browse the stack while the island stays alive; tap the left side for the quick tiles, tap the badge to bring the notification back."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.Contrast, tr("Значок — монохром", "The icon, in monochrome"),
        tr("Тот же остров с обложкой и эквалайзером, но только в чёрном и белом — под стать Liquid Glass в самом приложении. Тематические значки Android 13+ тоже стали чище.",
            "The same island with the cover and equalizer, but strictly in black and white to match the app's Liquid Glass. The Android 13+ themed icon is cleaner now too."),
    ),
    WhatsNewPage(
        Icons.Rounded.HealthAndSafety, tr("Экранный щит и свой adb", "Popup shield with built-in adb"),
        tr("Системные всплывающие плашки — второй источник двойных поп-апов. Щит выключает их в один тап, а встроенный в остров adb (как у Shizuku) сам подключается к отладке и выполняет команды.",
            "System heads-up banners are the second source of double pop-ups. The shield turns them off in one tap, and the built-in adb (Shizuku-style) connects to debugging and runs the commands by itself."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.Security, tr("Щит дублей за миллисекунды", "Millisecond duplicate shield"),
        tr("Повторная плашка, пришедшая быстрее 900 мс, гасится мгновенно — даже если у неё изменился текст. По желанию остров показывает отчёт «Щит · дубль заблокирован».",
            "A repeat banner arriving within 900 ms is killed instantly — even if its text changed. The island can show a short \"Shield · duplicate blocked\" report."),
    ),
    WhatsNewPage(
        Icons.Rounded.Videocam, tr("У камеры — своя анимация", "Camera gets its own animation"),
        tr("Камера больше не показывает анимацию микрофона: зелёная линза мягко пульсирует, как точка камеры у iPhone. Фонарик тоже больше не притворяется камерой.",
            "The camera no longer borrows the microphone animation: a green lens pulses softly, like the iPhone's camera dot. The flashlight no longer fakes a camera either."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.Face, tr("Unlock — целиком", "Unlock — the whole word"),
        tr("Малый вырез Face Unlock больше не обрезает слово до «Unloc»: капсула раздвигается ровно настолько, чтобы «Unlocked» поместилось целиком при любом шрифте.",
            "The Face Unlock small notch no longer clips the word to \"Unloc\": the capsule widens just enough for the whole \"Unlocked\" at any font size."),
        tr("Попробовать", "Try it now"),
    ),
    WhatsNewPage(
        Icons.Rounded.Settings, tr("Настройки вернулись в док", "Settings is back in the dock"),
        tr("Значок настроек снова стоит рядом с «Домой» и больше не уезжает за край экрана — док показывает обе вкладки целиком.",
            "The settings icon sits next to Home again instead of sliding off-screen — the dock always shows both tabs."),
    ),
    WhatsNewPage(
        Icons.Rounded.OpenInFull, tr("Раскрытый остров под контролем", "Expanded island, under control"),
        tr("В «Положении и размере» — форма и размер большого острова: высота плашек в процентах, скругление больших углов и максимальная ширина раскрытия.",
            "Position and size now edits the big island too: notice height in percent, the expanded corner radius and the maximum width."),
    ),
    WhatsNewPage(
        Icons.Rounded.Grain, tr("Морозный переход и пружина", "Frost and spring"),
        tr("Морозную дымку смены содержимого можно выключить отдельно, а масштаб нового содержимого движется пружиной из «Упругости» — Liquid Glass живёт и в анимациях.",
            "The frosted haze of content changes gets its own toggle, and incoming content scales on the elasticity spring — Liquid Glass lives in the animations too."),
    ),
    WhatsNewPage(
        Icons.Rounded.Headset, tr("Проводные наушники и «Заряжено»", "Wired headphones and Charged"),
        tr("Остров замечает проводные наушники и радуется, когда батарея доходит до 100%. Плюс событие «Энергосбережение» — все в «Системных оповещениях».",
            "The island notices wired headphones and celebrates at 100% battery. Plus a Battery Saver event — all in System Alerts."),
    ),
    WhatsNewPage(
        Icons.Rounded.Swipe, tr("Пружинка из-под камеры", "A spring out of the camera"),
        tr("Смахнули последнюю плашку — остров ныряет в вырез и тут же выпрыгивает обратно пустой капсулой. Уведомление, убранное в шторке, убирает плашку мгновенно.",
            "Swipe the last banner away — the island dives into the notch and springs right back out as an idle capsule. Removing a notification in the shade clears its card instantly too."),
    ),
    WhatsNewPage(
        Icons.Rounded.VpnKey, tr("Из 1.10.0, если пропустили", "From 1.10.0, in case you missed it"),
        tr("Скорость сети в капсуле, скриншоты, тихие часы, напоминания о перерыве, flip-to-shhh, лицензии открытого кода и еще десяток событий.",
            "Network speed in the capsule, screenshots, quiet hours, break reminders, flip-to-shhh, open-source licenses and a dozen more events."),
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
        0 -> IslandBus.post(
            app.orubus.island.core.Notice(
                tr("Щит", "Shield"), tr("дубль заблокирован", "duplicate blocked"),
                Icons.Rounded.Security, app.orubus.island.core.Tints.Blue,
                style = app.orubus.island.core.NoticeStyle.PILL, extraMs = 400,
            ),
        )
        2 -> IslandBus.post(
            app.orubus.island.core.Notice(
                tr("Камера", "Camera"), tr("используется приложением", "in use by an app"),
                Icons.Rounded.Videocam, app.orubus.island.core.Tints.Green,
                style = app.orubus.island.core.NoticeStyle.CAM, extraMs = 600,
            ),
        )
        3 -> IslandBus.post(PrivacyMonitor.demoNotice())
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
