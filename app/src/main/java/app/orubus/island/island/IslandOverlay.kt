package app.orubus.island.island

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.BatteryInfo
import app.orubus.island.core.ChronoInfo
import app.orubus.island.core.Controls
import app.orubus.island.core.IslandBus
import app.orubus.island.core.Lyrics
import app.orubus.island.core.NoticeStyle
import app.orubus.island.core.Tints
import app.orubus.island.core.tr
import app.orubus.island.data.Elasticity
import app.orubus.island.data.ExpandStyle
import app.orubus.island.data.IslandSettings
import app.orubus.island.data.SettingsRepo
import app.orubus.island.data.TapAction
import app.orubus.island.service.CallMonitor
import app.orubus.island.service.NavScreen
import app.orubus.island.service.NotifListener
import app.orubus.island.service.PayMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/** Пример активности для подбора ширины: стрелка направо и «300 м». */
private val WidthDemo = app.orubus.island.core.LiveInfo(
    key = "width-demo", pkg = "", kind = app.orubus.island.core.LiveKind.NAV, title = "", text = "", sub = "",
    icon = null, art = null, postTime = 0L, open = null, distance = tr("300 м", "300 m"), turn = app.orubus.island.core.Turn.RIGHT,
)

/** Цвет острова на экране «Размер и положение»: его нужно спрятать под вырезом камеры. */
val CalibrationRed = Color(0xFFFF6B6B)

/** Запас вокруг острова в окне: под тень, «желе» и пузырь. */
const val ISLAND_PAD_DP = 12

private data class Frame(val act: Act, val expanded: Boolean) {
    val key: String get() = "${act.id}|$expanded"
}

@Composable
fun IslandOverlay(
    onTransition: () -> Unit = {},
    onBodyBounds: (Rect) -> Unit = {},
    onBubbleBounds: (Rect) -> Unit = {},
) {
    val saved by SettingsRepo.state.collectAsState()
    val powerSave by IslandBus.powerSave.collectAsState()
    // В энергосбережении Android с «Экономией заряда» остров обходится без живых эффектов
    val s = remember(saved, powerSave) { saved.forPowerSave(powerSave) }
    val media by IslandBus.media.collectAsState()
    val lyrics by IslandBus.lyrics.collectAsState()
    val progress by IslandBus.progress.collectAsState()
    val chrono by IslandBus.chrono.collectAsState()
    val calls by IslandBus.calls.collectAsState()
    val live by IslandBus.live.collectAsState()
    val torch by IslandBus.torch.collectAsState()
    val timer by IslandBus.timer.collectAsState()
    val stopwatch by IslandBus.stopwatch.collectAsState()
    val notice by IslandBus.notice.collectAsState()
    val noticeStack by IslandBus.noticeStack.collectAsState()
    val battery by IslandBus.battery.collectAsState()
    val foreground by IslandBus.foregroundPackage.collectAsState()
    val expanded by IslandBus.expanded.collectAsState()
    val focus by IslandBus.focus.collectAsState()
    val game by IslandBus.gameForeground.collectAsState()
    val calibrating by IslandBus.calibrating.collectAsState()
    val nextAlarm by IslandBus.nextAlarm.collectAsState()
    val egg by IslandBus.egg.collectAsState()
    val payReady by IslandBus.payReady.collectAsState()
    // Будильник появляется в острове за 12 часов и уходит, когда прозвенел: просыпаемся ровно в эти два момента,
    // а не каждые полминуты. Пока телефон спит, ожидание стоит — поэтому со включением экрана пересчитываем
    val screenOn by IslandBus.screenOn.collectAsState()
    var alarmNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nextAlarm, screenOn) {
        val at = nextAlarm?.triggerAt ?: return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            alarmNow = now
            val next = when {
                now < at - ALARM_LEAD_MS -> at - ALARM_LEAD_MS
                now < at -> at
                else -> break
            }
            delay(next - now + 50)
        }
    }

    // Обратный отсчёт из чужого уведомления, дошедший до нуля, уходит через 3 секунды, а не висит «00:00».
    // Просыпаемся ровно к этому моменту; пока экран погашен, ожидание стоит — со включением пересчитываем
    var chronoNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(chrono, screenOn) {
        while (true) {
            val now = System.currentTimeMillis()
            chronoNow = now
            val next = chrono.values.filter { it.countDown && it.pausedMs == null }
                .map { it.base + ChronoInfo.DONE_MS }.filter { it >= now }.minOrNull() ?: break
            delay(next - now + 50)
        }
    }
    val chronoActive = chrono.values.filterNot { it.finished(chronoNow) }

    // Пробная «живая активность» исчезает по времени, даже если таймер удаления не сработал (сон телефона)
    val hasExpiring = live.values.any { it.expiresAt > 0 }
    var wallNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(hasExpiring) {
        while (hasExpiring) {
            wallNow = System.currentTimeMillis()
            delay(1000)
        }
    }
    // Навигатор, у которого в уведомлении пусто, дополняется тем, что видно на его экране
    val screenNav by NavScreen.data.collectAsState()
    val liveActive = live.values.filter { it.expiresAt == 0L || wallNow < it.expiresAt }
        .map { NavScreen.merge(it, screenNav[it.pkg]) }
        .let { list -> list + screenNav.values.filter { sn -> sn.distance != null && list.none { it.pkg == sn.pkg } }.map(NavScreen::synthetic) }

    val call = if (!s.cardCall) null else {
        val tel = calls[CallMonitor.KEY]
        val fromApp = calls.filterKeys { it != CallMonitor.KEY }.values.maxByOrNull { it.postTime }
        when {
            fromApp == null -> tel
            tel == null -> fromApp
            // У звонилки богаче карточка (имя, фото, кнопки), а состояние надёжнее у телефона
            else -> fromApp.copy(
                incoming = tel.incoming,
                base = if (!tel.incoming && fromApp.incoming) tel.base else fromApp.base,
                avatar = fromApp.avatar ?: tel.avatar,
            )
        }
    }
    // Смахнутый входящий не показываем, пока звонит (сбрасывает служба, когда звонить перестали)
    val callHidden by IslandBus.callHidden.collectAsState()
    val acts = buildList {
        val gameSilent = game && !s.gameShowIsland
        // Входящий звонок важнее всего, как на iPhone — и в играх тоже
        if (call != null && call.incoming && !callHidden) add(Act.CallA(call))
        if (!(game && !s.gameShowNotices)) notice?.let { add(Act.NoticeA(it)) }

        val persistent = mutableListOf<Act>()
        val t = timer
        // Звонящий таймер не прячем даже в игровом режиме — человек ждёт сигнал
        if (t != null && t.ringing) persistent += Act.TimerA(t)
        if (!gameSilent) {
            // Оплата телефоном — пока ждём терминал, она главнее остального
            payReady?.let { persistent += Act.PayA(it) }
            if (call != null && !call.incoming) persistent += Act.CallA(call)
            if (torch.on && s.cardTorch) persistent += Act.TorchA(torch)
            if (t != null && !t.ringing) persistent += Act.TimerA(t)
            if (s.cardLive) liveActive.maxByOrNull { it.postTime }?.let { persistent += Act.LiveA(it) }
            stopwatch?.let { persistent += Act.StopwatchA(it) }
            if (s.cardTimer) chronoActive.maxByOrNull { it.postTime }?.let { persistent += Act.ChronoA(it) }
            val m = media
            if (s.cardMedia && m != null && !(s.hideWhenPlayerOpen && foreground == m.pkg)) persistent += Act.MediaA(m)
            if (s.cardProgress) progress.values.maxByOrNull { it.postTime }?.let { persistent += Act.ProgressA(it) }
            if (s.cardCharging && battery.charging) persistent += Act.ChargingA(battery)
        }
        // Будильник — тише всех: показывается, только когда больше ничего нет, и не уходит в пузырь.
        // Стопка свернутых уведомлений — полноразмерная плашка, как у обычного уведомления: ниже
        // карточек, но выше будильника. Она не даёт острову исчезнуть после смахнутой плашки —
        // вместо пустой капсулы теперь живая плашка, которую листают свайпом.
        // В игровом режиме с «Не показывать остров» она прячется вместе со всем островом
        if (s.notifStack && noticeStack.isNotEmpty() && !gameSilent) persistent += Act.StackA(noticeStack)
        val alarm = nextAlarm
        if (s.cardAlarm && alarm != null && alarm.triggerAt - alarmNow in 1..ALARM_LEAD_MS) persistent += Act.AlarmA(alarm)

        // Выбранная активность выходит вперёд, но звонящий таймер всегда главный
        val ringing = (persistent.firstOrNull() as? Act.TimerA)?.t?.ringing == true || call?.incoming == true
        val i = persistent.indexOfFirst { it.id == focus }
        if (i > 0 && !ringing) persistent.add(0, persistent.removeAt(i))
        addAll(persistent)
    }

    // Пока двигают ползунок «Ширина с активностью», на острове пример: стрелка и метры
    val previewUntil by IslandBus.widthPreviewUntil.collectAsState()
    var previewNow by remember { mutableLongStateOf(0L) }
    LaunchedEffect(previewUntil) {
        while (true) {
            previewNow = android.os.SystemClock.elapsedRealtime()
            if (previewNow >= previewUntil) break
            delay(250)
        }
    }
    val previewing = previewNow < previewUntil

    // Во время подгонки размера показываем только пустую красную капсулу
    val primary = when {
        calibrating -> Act.Idle
        previewing -> Act.LiveA(WidthDemo)
        else -> acts.firstOrNull() ?: Act.Idle
    }
    val incoming = (primary as? Act.CallA)?.c?.incoming == true
    // Вторая активность — кружком рядом, а с «Двойным островом» — второй плашкой
    val secondary = if ((!s.bubble && !s.dualNotch) || primary.transient || incoming || calibrating) null
    else acts.drop(1).firstOrNull { !it.transient && it !is Act.AlarmA }
    val showExpanded = expanded && !primary.transient && !incoming && !calibrating
    val h = s.height.dp

    // Пузырь уходит с анимацией, поэтому помним последнюю активность в нём
    val lastSecondary = remember { arrayOfNulls<Act>(1) }
    if (secondary != null) lastSecondary[0] = secondary
    val bubbleVisible = secondary != null && !showExpanded
    // «Авто-расширение»: активный остров чуть толще, пока показывает музыку, таймер или зарядку
    val sEff = if (s.autoExpandActive && !showExpanded && primary !is Act.Idle &&
        primary !is Act.NoticeA && primary !is Act.StackA && !calibrating && !previewing
    ) {
        s.copy(activeExtra = (s.activeExtra + 3).coerceAtMost(14))
    } else {
        s
    }
    // Место под пузырь/второй вырез читается при измерении: кадр анимации не пересобирает остров
    val slot = animateDpAsState(
        when {
            !bubbleVisible -> 0.dp
            s.dualNotch -> activeHeight(s) * 2.3f
            else -> activeHeight(s) + 6.dp
        },
        islandSpec(s),
        label = "slot",
    )

    // Раскрытая активность пропала (музыку закрыли, таймер отменили) — сворачиваемся, а не показываем часы
    val focusGone = expanded && focus != null && focus != Act.Idle.id && acts.none { it.id == focus }
    LaunchedEffect(focusGone) { if (focusGone) Controls.collapse() }

    // «Сворачивать раскрытый остров»: если его столько секунд не трогают, он сворачивается сам
    LaunchedEffect(showExpanded, s.autoCollapseSec) {
        val limit = s.autoCollapseSec * 1000L
        if (!showExpanded || limit <= 0) return@LaunchedEffect
        val opened = SystemClock.elapsedRealtime()
        while (true) {
            val idle = SystemClock.elapsedRealtime() - maxOf(opened, IslandBus.lastTouch)
            if (idle >= limit) break
            delay(limit - idle)
        }
        Controls.collapse()
    }

    // «Прятать без дела»: через hideIdleSec секунд пустого острова он прячется и возвращается с новым событием
    var idleHidden by remember { mutableStateOf(false) }
    LaunchedEffect(primary.id, s.autoHideIdle, s.hideIdleSec, screenOn) {
        idleHidden = false
        if (!s.autoHideIdle || primary !is Act.Idle || showExpanded || calibrating) return@LaunchedEffect
        delay(s.hideIdleSec * 1000L)
        if (primary is Act.Idle && !IslandBus.expanded.value) idleHidden = true
    }

    val hidden = (s.hideIdle || idleHidden) && primary is Act.Idle && !showExpanded && !calibrating

    // Перед сменой формы просим окно стать большим, чтобы анимация шла внутри неподвижного окна
    val transitionKey = "${primary.id}|$showExpanded|${secondary?.id}|$hidden"
    val lastKey = remember { arrayOfNulls<String>(1) }
    SideEffect {
        if (lastKey[0] != transitionKey) {
            lastKey[0] = transitionKey
            onTransition()
        }
    }

    // Спрятанный остров не ловит касания, даже пока уходит
    if (hidden) {
        SideEffect {
            onBodyBounds(Rect.Zero)
            onBubbleBounds(Rect.Zero)
        }
    }
    val hiddenNow = rememberUpdatedState(hidden)

    // После смахнутой последней плашки остров ныряет в вырез и тут же выпрыгивает обратно —
    // «пружинка из-под камеры» вместо внезапной смены на пустую капсулу
    var popHide by remember { mutableStateOf(false) }
    LaunchedEffect(primary.id) {
        val at = IslandBus.notchPopAt
        if (at == 0L) return@LaunchedEffect
        IslandBus.notchPopAt = 0L
        if (primary !is Act.Idle || s.reduceEffects) return@LaunchedEffect
        if (SystemClock.elapsedRealtime() - at > 1500) return@LaunchedEffect
        popHide = true
        delay(320)
        popHide = false
    }

    // Где остров в окне — вокруг него рисуются эффекты пасхалки
    val body = remember { mutableStateOf(Rect.Zero) }
    val onBody = remember(onBodyBounds) {
        { r: Rect ->
            body.value = r
            onBodyBounds(if (hiddenNow.value) Rect.Zero else r)
        }
    }
    val onBubble = remember(onBubbleBounds) { { r: Rect -> onBubbleBounds(if (hiddenNow.value) Rect.Zero else r) } }
    val eggOn = egg && !calibrating

    // Остров меряется по своему размеру, а не по окну, и стоит сверху по центру окна
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Оплата: волны NFC и вспышка успеха вокруг острова, как в ролике
        if (!calibrating) PayEffects(primary, { body.value }, s)
        // Пасхалка: свечение за островом, вспышки и звёзды поверх
        if (eggOn) EggGlow { body.value }
        // «Скрывать пустой остров»: он не пропадает рывком, а уходит в камеру и выходит из неё
        val top = TransformOrigin(0.5f, 0f)
        AnimatedVisibility(
            !hidden && !popHide,
            enter = if (s.reduceEffects) fadeIn() else scaleIn(islandSpec(s), initialScale = 0.3f, transformOrigin = top) + fadeIn(tween(120)),
            exit = if (s.reduceEffects) fadeOut() else scaleOut(islandSpec(s), targetScale = 0.3f, transformOrigin = top) + fadeOut(tween(160)),
        ) {
            IslandRow(slot, sEff, primary, showExpanded, lyrics, battery, bubbleVisible, lastSecondary[0], calibrating, onBody, onBubble)
        }
        if (eggOn) {
            EggSparks { body.value }
            EggDriver({ body.value }, s)
        }
    }
}

@Composable
private fun IslandRow(
    slot: State<Dp>,
    s: IslandSettings,
    primary: Act,
    showExpanded: Boolean,
    lyrics: Lyrics?,
    battery: BatteryInfo,
    bubbleVisible: Boolean,
    bubbleAct: Act?,
    calibrating: Boolean,
    onBodyBounds: (Rect) -> Unit,
    onBubbleBounds: (Rect) -> Unit,
) {
    Row(
        Modifier
            // Сдвиг вбок — для камеры не по центру. Широкие плашки и раскрытый остров при этом не уезжают
            // за край экрана: сдвиг не больше, чем позволяет их ширина, — аватар и текст остаются видны
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height) {
                    val room = ((constraints.maxWidth - p.width) / 2).coerceAtLeast(0)
                    p.place(s.offsetX.dp.roundToPx().coerceIn(-room, room), 0)
                }
            }
            .wrapContentSize(Alignment.TopCenter, unbounded = true)
            .padding(ISLAND_PAD_DP.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Пустое место слева уравновешивает пузырь, чтобы остров стоял над камерой
        Spacer(Modifier.animatedWidth(slot))
        IslandBody(primary, showExpanded, s, lyrics, battery, calibrating, onBodyBounds)
        BubbleSlot(slot, bubbleVisible, bubbleAct, s, onBubbleBounds)
    }
}

/** Ширина из анимации — читается при измерении, а не при сборке, поэтому кадр анимации не пересобирает остров. */
private fun Modifier.animatedWidth(width: State<Dp>) = layout { measurable, constraints ->
    val w = width.value.roundToPx().coerceAtLeast(0)
    val p = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
    layout(w, p.height) { p.place(0, 0) }
}

/**
 * Пузырь второй активности. Он лежит под островом и отделяется от него каплей: пока место под него растёт,
 * пузырь выходит из-под края острова и вырастает, при уходе — прячется обратно.
 * Отдельная функция: внутри Row подставился бы RowScope.AnimatedVisibility.
 */
@Composable
private fun BubbleSlot(slot: State<Dp>, visible: Boolean, act: Act?, s: IslandSettings, onBounds: (Rect) -> Unit) {
    Box(
        Modifier
            .zIndex(-1f)
            .layout { measurable, constraints ->
                val w = slot.value.roundToPx().coerceAtLeast(0)
                val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                layout(w, p.height) { p.place(w - p.width, 0) }
            }
            .onGloballyPositioned { onBounds(it.boundsInWindow()) },
    ) {
        val drop = TransformOrigin(0f, 0.5f)
        AnimatedVisibility(
            visible && act != null,
            enter = if (s.reduceEffects) fadeIn() else scaleIn(islandSpec(s), initialScale = 0.4f, transformOrigin = drop) + fadeIn(tween(90)),
            exit = if (s.reduceEffects) fadeOut() else scaleOut(islandSpec(s), targetScale = 0.4f, transformOrigin = drop) + fadeOut(tween(120, delayMillis = 60)),
        ) {
            if (act != null) Bubble(act, s)
        }
    }
}

@Composable
private fun IslandBody(
    primary: Act,
    expanded: Boolean,
    s: IslandSettings,
    lyrics: Lyrics?,
    battery: BatteryInfo,
    calibrating: Boolean,
    onBounds: (Rect) -> Unit,
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val conf = LocalConfiguration.current
    // Раскрытый остров iPhone — 371 pt при ширине экрана 393 pt, то есть по 11 с каждой стороны.
    // «Максимальная ширина» из настроек ограничивает, насколько широко он раскрывается
    val expandedWidth = ((conf.screenWidthDp.dp - 22.dp) * (s.maxWidthPct / 100f)).coerceAtMost(460.dp)
    // Ползунок громкости чуть уже раскрытого острова
    val volumeWidth = (expandedWidth - 22.dp).coerceAtLeast(200.dp)
    val h = s.height.dp

    // Смещение карточки под пальцем (свайпы), ползунок громкости (долгое нажатие справа)
    // и ширина острова в px — для разделения «нажатие / громкость» по половинам
    val swipe = remember { SwipeState() }
    val volumeMode = IslandVolume.mode
    var bodyW by remember { mutableIntStateOf(0) }
    val volumeStepPx = with(LocalDensity.current) { 52.dp.toPx() }
    DisposableEffect(context) { IslandVolume.attach(context); onDispose { } }

    // Скругления из настроек: «малый радиус» — для капсулы, «большой» — для раскрытого и плашек.
    // Радиус капсулы не больше половины высоты, иначе углы обрежутся некрасиво
    val smallR = s.radiusSmall.dp
    val bigR = s.radiusBig.dp
    // Скругления — как в ролике: плеер и фонарик 46, навигатор 42, обновление 36, уведомление 32
    val cornerTarget = when {
        volumeMode -> smallR.coerceAtMost(h / 2)
        expanded && (primary is Act.MediaA || primary is Act.TorchA) -> bigR
        expanded && primary is Act.LiveA -> bigR
        expanded -> bigR
        primary is Act.NoticeA && (primary.n.style == NoticeStyle.MESSAGE || primary.n.style == NoticeStyle.EGG) -> bigR.coerceAtMost(34.dp)
        primary is Act.StackA -> bigR.coerceAtMost(34.dp)
        primary is Act.NoticeA && primary.n.style == NoticeStyle.UPDATE -> bigR.coerceAtMost(38.dp)
        primary is Act.NoticeA && primary.n.style == NoticeStyle.WIDE && primary.n.title.isNotBlank() ->
            minOf(bigR, noticeHeight(s) / 2)
        primary is Act.NoticeA && (primary.n.style == NoticeStyle.PAY_DONE ||
            primary.n.style == NoticeStyle.LIVE || primary.n.style == NoticeStyle.ALARM || primary.n.style == NoticeStyle.SENT ||
            primary.n.style == NoticeStyle.FACEID) -> minOf(bigR, noticeHeight(s) / 2)
        primary is Act.PayA -> minOf(bigR, noticeHeight(s) / 2)
        primary is Act.CallA && primary.c.incoming -> minOf(bigR, callHeight(s) / 2)
        primary is Act.Idle -> smallR.coerceAtMost(h / 2)
        else -> smallR.coerceAtMost(activeHeight(s) / 2)
    }
    // Скругление читается при рисовании: кадр раскрытия перерисовывает рамку и стекло, а не пересобирает остров
    val corner = animateDpAsState(cornerTarget, islandSpec(s), label = "corner")
    val shape = remember(corner, s.notch) { AnimatedIslandShape(corner, s.notch) }
    val accent = (primary as? Act.MediaA)?.m?.accent
    val outlineColor = if (s.outlineFromArt && accent != null) accent else Color(s.outlineColor)
    // «Автоцвет фона»: плашка окрашивается цветом, подобранным по значку приложения
    val bgBase = (primary as? Act.NoticeA)?.n?.bg
        ?.takeIf { s.autoBg }
        ?.let { Color(it) } ?: Color(s.color)
    // Рамка по состоянию: «малая» — у капсулы, «большая» — у раскрытого острова и плашек
    val borderOn = if (expanded || primary is Act.NoticeA) s.outline && s.borderExpanded else s.outline && s.borderSmall

    // Свечение цвета обложки, пока играет музыка («Эффекты»): с треком цвет плавно меняется, с паузой гаснет
    val playingArt = (primary as? Act.MediaA)?.m?.takeIf { it.playing }?.accent
    val lastArt = remember { arrayOf(Color.White) }
    if (playingArt != null) lastArt[0] = playingArt
    val glowOn = s.fxGlow && !s.reduceEffects && !calibrating && playingArt != null
    val glowAlpha by animateFloatAsState(if (glowOn) 1f else 0f, tween(500), label = "glowAlpha")
    val glowTint by animateColorAsState(lastArt[0], tween(500), label = "glowTint")

    // «Как фрукт»: при раскрытии остров сплющивается и отпружинивает
    val jellyX = remember { Animatable(1f) }
    val jellyY = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(expanded) {
        if (firstRun) {
            firstRun = false
            return@LaunchedEffect
        }
        if (s.expandStyle != ExpandStyle.FRUIT || s.reduceEffects || s.elasticity == Elasticity.NONE) return@LaunchedEffect
        val amp = 0.07f * (1.25f - s.elasticity.damping)
        // Отскок затухает по «Упругости»: сильная — как раньше, слабая — почти без колебаний
        val rebound = spring<Float>(dampingRatio = (s.elasticity.damping * 0.62f).coerceAtLeast(0.3f), stiffness = 260f)
        val dir = if (expanded) 1f else -1f
        launch {
            jellyX.animateTo(1f - amp * dir, tween(110))
            jellyX.animateTo(1f, rebound)
        }
        launch {
            jellyY.animateTo(1f + amp * dir, tween(110))
            jellyY.animateTo(1f, rebound)
        }
    }
    // Оттягивание вниз пальцем
    val pull = remember { Animatable(0f) }

    // Liquid Glass: при смене содержимого по стеклу пробегает блик, при наклоне телефона блики сдвигаются
    val tilt = IslandBus.glassTilt.collectAsState()
    val sheen = remember { Animatable(-1f) }
    LaunchedEffect(primary.id, expanded, s.liquidGlass) {
        if (!s.liquidGlass || s.reduceEffects) return@LaunchedEffect
        sheen.snapTo(0f)
        sheen.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        sheen.snapTo(-1f)
    }

    // Морозное стекло при смене состояния: содержимое на миг размывается и проявляется обратно.
    // «Морозный переход» в «Эффектах» управляет этой дымкой; с Liquid Glass дымка гуще
    val frost = remember { Animatable(0f) }
    LaunchedEffect(primary.id, expanded) {
        if (!s.fxBlur || !s.fxFrost || s.reduceEffects) return@LaunchedEffect
        frost.snapTo(if (s.liquidGlass) 1.2f else 1f)
        frost.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
    }

    // Короткая пилюля (режим звука) вздрагивает вбок, как остров на iPhone при переключении «Звонок/Бесшумно»
    val shake = remember { Animatable(0f) }
    val pillId = (primary as? Act.NoticeA)?.n?.takeIf { it.style == NoticeStyle.PILL }?.id
    LaunchedEffect(pillId) {
        if (pillId == null || s.reduceEffects || !s.fxShake) return@LaunchedEffect
        for (x in floatArrayOf(-8f, 7f, -5f, 3.5f, -2f, 0f)) shake.animateTo(x, tween(60))
    }

    val current by rememberUpdatedState(primary)
    val isExpanded by rememberUpdatedState(expanded)
    val settings by rememberUpdatedState(s)
    val egg by IslandBus.egg.collectAsState()

    Box(
        Modifier
            .onGloballyPositioned { onBounds(it.boundsInWindow()) }
            .onSizeChanged { bodyW = it.width }
            .graphicsLayer {
                // Пасхалка: остров подпрыгивает в такт песни; оплата прошла — на миг подрастает
                val bump = Egg.bump() * PayFx.pop.floatValue
                scaleX = jellyX.value * (1f + 0.03f * pull.value) * bump
                scaleY = jellyY.value * (1f + 0.08f * pull.value) * bump
                translationX = shake.value.dp.toPx()
                transformOrigin = TransformOrigin(0.5f, 0f)
                // Карточка едет за пальцем при свайпе — только у свёрнутого острова
                if (!expanded) {
                    translationX += swipe.x.floatValue
                    translationY += swipe.y.floatValue
                    alpha = swipe.alpha.floatValue
                }
            }
            .then(if (glowAlpha > 0f) Modifier.drawBehind { islandGlow(glowTint, glowAlpha, corner.value.toPx(), s.notch) } else Modifier)
            // Тень и обрезка по форме — одним слоем; форма на каждом кадре берётся из анимации скругления
            .graphicsLayer {
                this.shape = islandShape(corner.value, s.notch)
                clip = true
                when {
                    s.liquidGlass && !calibrating && !s.reduceEffects -> {
                        shadowElevation = 10.dp.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = 0.5f)
                        spotShadowColor = Color.Black.copy(alpha = 0.6f)
                    }
                    s.shadow && !s.reduceEffects -> shadowElevation = 10.dp.toPx()
                }
            }
            .then(
                if (s.liquidGlass && !calibrating) {
                    Modifier.glassSurface(shape, bgBase, s.glassOpacity / 100f, tilt = { tilt.value }) { sheen.value }
                } else {
                    Modifier.background(if (calibrating) CalibrationRed else bgBase)
                },
            )
            .then(
                when {
                    calibrating -> Modifier.border(2.dp, Color.White, shape)
                    borderOn -> Modifier.border(s.outlineWidth.dp, outlineColor, shape)
                    else -> Modifier
                },
            )
            .then(
                if (egg && !calibrating) {
                    Modifier.drawWithContent {
                        drawContent()
                        eggRim(corner.value.toPx(), s.notch)
                    }
                } else {
                    Modifier
                },
            )
            // Любое касание — раскрытый остров не свернётся сам из-под пальца
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        IslandBus.lastTouch = SystemClock.elapsedRealtime()
                    }
                }
            }
            .pointerInput(s.doubleTapAction != TapAction.NOTHING, s.longPressAction != TapAction.NOTHING, s.longPressPartition, s.longPressVolume) {
                val doubleOn = settings.doubleTapAction != TapAction.NOTHING
                val longOn = settings.longPressAction != TapAction.NOTHING
                val onDouble: ((Offset) -> Unit)? = if (!doubleOn) null else {
                    { _: Offset ->
                        haptic()
                        perform(context, settings.doubleTapAction, current)
                    }
                }
                val onLong: ((Offset) -> Unit)? = if (!longOn) null else {
                    { pos: Offset ->
                        // Правая половина и включён ползунок громкости — там громкость, а не действие
                        val volumeTakesIt = settings.longPressPartition && settings.longPressVolume &&
                            !isExpanded && pos.x >= size.width / 2f
                        if (!volumeTakesIt) {
                            haptic()
                            perform(context, settings.longPressAction, current)
                        }
                    }
                }
                detectTapGestures(
                    onTap = { pos ->
                        haptic()
                        onTap(context, current, isExpanded, settings, pos, size.width.toFloat())
                    },
                    onDoubleTap = onDouble,
                    onLongPress = onLong,
                )
            }
            // Долгое нажатие на правой половине и скольжение — ползунок громкости над островом
            .volumeSlide(
                accept = { pos ->
                    settings.longPressPartition && settings.longPressVolume && !isExpanded && !IslandVolume.mode &&
                        pos.x >= bodyW / 2f
                },
                onBegin = {
                    haptic()
                    IslandVolume.begin()
                },
                onDelta = { dx, dy -> IslandVolume.drag(dx, dy, volumeStepPx) },
                onEnd = { IslandVolume.end() },
            )
            .islandSwipe(
                state = swipe,
                caps = {
                    val act = current
                    SwipeCaps(
                        canExpand = !isExpanded && settings.swipeExpand && !act.transient && (act !is Act.Idle || settings.idleExpand),
                        canCollapse = isExpanded,
                        canDismiss = !isExpanded && (act is Act.NoticeA || act is Act.PayA || act is Act.StackA || (act is Act.CallA && act.c.incoming)),
                        canTracks = !isExpanded && act is Act.MediaA && settings.swipeTracks,
                        canMinimize = !isExpanded && act is Act.NoticeA,
                        canBrowse = !isExpanded && act is Act.StackA,
                    )
                },
                onPull = { f -> scope.launch { pull.snapTo(f) } },
                onPullEnd = { scope.launch { pull.animateTo(0f, spring(dampingRatio = 0.45f)) } },
                onExpand = {
                    haptic()
                    Controls.expand(current.id)
                },
                onCollapse = {
                    haptic()
                    Controls.collapse()
                },
                onDismiss = {
                    val act = current
                    when (act) {
                        // Со «Смахнул — значит убрать» уведомление уходит и из шторки
                        is Act.NoticeA -> {
                            if (settings.swipeClearNotif) act.n.message?.key?.let { NotifListener.cancelKey(it) }
                            IslandBus.notice.value = null
                            // Пустому острову после смахнутой плашки — выход из-под камеры
                            IslandBus.notchPopAt = SystemClock.elapsedRealtime()
                        }
                        // Стопку смахнули вбок далеко — очистить целиком: и значок, и уведомления в шторке, если включено
                        is Act.StackA -> {
                            if (settings.swipeClearNotif) act.items.forEach { it.message?.key?.let(NotifListener::cancelKey) }
                            IslandBus.stackBrowse.value = -1
                            IslandBus.noticeStack.value = emptyList()
                            IslandBus.notchPopAt = SystemClock.elapsedRealtime()
                        }
                        // Подсказку оплаты тоже можно смахнуть
                        is Act.PayA -> {
                            PayMonitor.clearReady()
                            IslandBus.notchPopAt = SystemClock.elapsedRealtime()
                        }
                        // И входящий звонок, если не хочется отвечать: звонок идёт дальше, но остров свободен
                        is Act.CallA -> if (act.c.incoming) {
                            haptic()
                            Controls.hideIncomingCall()
                            IslandBus.notchPopAt = SystemClock.elapsedRealtime()
                        }
                        else -> Unit
                    }
                    swipe.reset()
                },
                // Плашку смахнули вверх — она не исчезает, а остаётся у выреза полноразмерной плашкой.
                // Выключатель «Свёрнутые уведомления» возвращает прежнее поведение: плашка просто смахивается
                onMinimize = {
                    val act = current
                    if (act is Act.NoticeA) {
                        if (settings.notifStack) {
                            IslandBus.minimizeNotice(act.n)
                        } else {
                            if (settings.swipeClearNotif) act.n.message?.key?.let { NotifListener.cancelKey(it) }
                            IslandBus.notice.value = null
                            IslandBus.notchPopAt = SystemClock.elapsedRealtime()
                        }
                    }
                    swipe.reset()
                },
                // Стопку листают вбок: остров остаётся на месте, меняется только уведомление
                onBrowse = { next ->
                    haptic()
                    IslandBus.browseStack(next)
                },
                onTracks = { next ->
                    haptic()
                    // Как в ПК-версии: толкнуть влево — следующий трек
                    if (next) Controls.next() else Controls.prev()
                },
            ),
    ) {
        // Ползунок громкости живёт вместо содержимого: остров превращается в широкую пилюлю
        AnimatedContent(
            targetState = volumeMode,
            contentKey = { it },
            transitionSpec = {
                ((fadeIn(tween(130)) + scaleIn(islandSpec(s), initialScale = 0.86f, transformOrigin = TransformOrigin(0.5f, 0f))) togetherWith
                    (fadeOut(tween(120)) + scaleOut(islandSpec(s), targetScale = 0.86f, transformOrigin = TransformOrigin(0.5f, 0f))))
                    .using(SizeTransform(clip = true) { _, _ -> islandSpec(s) })
            },
            label = "volume",
        ) { vol ->
            if (vol) {
                VolumeBarContent(
                    modifier = Modifier.width(volumeWidth),
                    tint = Color.White,
                    onTick = { if (s.haptics) haptic() },
                )
            } else {
        // Смена содержимого: с «Жидким раскрытием» новое вырастает из прежнего вместе с формой острова,
        // без него — прежнее перетекание. Форма в обоих случаях меняется пружиной из «Раскрытия острова»
        val widths = remember { HashMap<String, Int>() }
        val frames = updateTransition(Frame(primary, expanded), label = "island")
        frames.AnimatedContent(
            contentAlignment = Alignment.TopCenter,
            contentKey = { it.key },
            transitionSpec = {
                val change = if (s.liquidExpand) EnterTransition.None togetherWith ExitTransition.None else classicChange(s)
                change.using(SizeTransform(clip = true) { _, _ -> islandSpec(s) })
            },
        ) { f ->
            Box(
                (if (s.liquidExpand) liquidFlow(f.key, frames, widths, s) else classicFlow(s))
                    .graphicsLayer { alpha = 1f - frost.value * 0.12f }
                    .blur((frost.value * 9).dp),
            ) {
                if (f.expanded) {
                    ExpandedContent(f.act, s, lyrics, battery, expandedWidth)
                } else {
                    CompactContent(f.act, s, expandedWidth)
                }
            }
        }
            }
        }
    }
}

/**
 * Прежнее перетекание, как в ролике: старое тает за 1/6 с, уменьшаясь, новое проявляется чуть позже
 * из 90% размера. Скорость — от «Длительности» (по умолчанию как в ролике).
 */
private fun classicChange(s: IslandSettings): ContentTransform {
    val m = motionScale(s)
    val origin = TransformOrigin(0.5f, 0.3f)
    val inSpec = tween<Float>((300 * m).toInt(), delayMillis = (100 * m).toInt(), easing = ExpoOut)
    val outSpec = tween<Float>((167 * m).toInt(), easing = LinearEasing)
    // «Физика пружины»: масштаб нового содержимого идёт пружиной из «Упругости», а не твином —
    // мягкий перелёт в конце, как у самой формы острова
    val springy = !s.reduceEffects && s.expandStyle != ExpandStyle.INSTANT
    val enterSpec: FiniteAnimationSpec<Float> = if (springy) {
        spring(dampingRatio = (s.elasticity.damping * 0.9f).coerceAtLeast(0.5f), stiffness = animStiffness(s) * 1.6f)
    } else inSpec
    val enter = if (s.reduceEffects) {
        fadeIn(tween(150))
    } else {
        fadeIn(enterSpec) + scaleIn(enterSpec, initialScale = 0.9f, transformOrigin = origin)
    }
    val exit = if (s.reduceEffects) {
        fadeOut(tween(90))
    } else {
        fadeOut(outSpec) + scaleOut(outSpec, targetScale = 0.95f, transformOrigin = origin)
    }
    return enter togetherWith exit
}

/** Размытие к прежнему перетеканию (Android 12+): новое проявляется из 7 dp, уходящее размывается до 5 dp. */
@Composable
private fun AnimatedVisibilityScope.classicFlow(s: IslandSettings): Modifier {
    val m = motionScale(s)
    val blur = transition.animateFloat(
        transitionSpec = {
            if (targetState == EnterExitState.Visible) tween((300 * m).toInt(), delayMillis = (100 * m).toInt(), easing = ExpoOut)
            else tween((167 * m).toInt())
        },
        label = "blur",
    ) { state ->
        when {
            s.reduceEffects || !s.fxBlur -> 0f
            state == EnterExitState.PreEnter -> 7f
            state == EnterExitState.PostExit -> 5f
            else -> 0f
        }
    }
    return Modifier.graphicsLayer { blurLayer(blur.value) }
}

/**
 * «Жидкое раскрытие», как в образце: новое содержимое вырастает из прежнего вместе с формой острова и проявляется
 * из размытия, прежнее растягивается или сжимается к размеру нового и тает. Ход — та же пружина, что у формы,
 * поэтому содержимое не обгоняет форму и не отстаёт. [widths] — ширина каждого содержимого, запомненная при измерении.
 */
@Composable
private fun AnimatedVisibilityScope.liquidFlow(key: String, frames: Transition<Frame>, widths: HashMap<String, Int>, s: IslandSettings): Modifier {
    // «Другое» содержимое: для нового — прежнее, для уходящего — новое. Если на полпути направление
    // сменилось (раскрыли и тут же свернули), «другое» остаётся прежним — масштаб идёт дальше без скачка
    val target = frames.targetState.key
    val other = if (key == target) frames.currentState.key else target
    val from = remember { arrayOfNulls<String>(1) }
    if (other != key) from[0] = other
    // 0 — содержимого ещё (или уже) нет, 1 — оно на месте; читается только при рисовании
    val presence = transition.animateFloat({ islandSpec(s) }, label = "presence") { if (it == EnterExitState.Visible) 1f else 0f }
    // Проявление и таяние — своя пружина, быстрее и без отскока: новое видно уже в начале хода
    val fade = transition.animateFloat({ fadeSpec(s) }, label = "fade") { if (it == EnterExitState.Visible) 1f else 0f }
    DisposableEffect(key) { onDispose { widths.remove(key) } }
    val zoom = !s.reduceEffects
    val blurDp = if (s.reduceEffects || !s.fxBlur) 0f else 14f
    return Modifier.layout { measurable, constraints ->
        val content = measurable.measure(constraints)
        widths[key] = content.width
        layout(content.width, content.height) {
            content.placeWithLayer(0, 0) {
                val p = presence.value
                alpha = fade.value.coerceIn(0f, 1f)
                val ow = from[0]?.let { widths[it] }
                if (zoom && ow != null && content.width > 0) {
                    val k = (ow.toFloat() / content.width).coerceIn(0.25f, 3f)
                    scaleX = k + (1f - k) * p
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                // Размытие сильное в начале и сходит к концу хода, а не тянется до последнего кадра
                val rest = (1f - p).coerceAtLeast(0f)
                blurLayer(blurDp * rest * sqrt(rest))
            }
        }
    }
}

/** Размытие слоя на [dp] — на Android 12+, где оно есть, и только когда заметно. */
private fun GraphicsLayerScope.blurLayer(dp: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dp > 0.3f) {
        renderEffect = BlurEffect(dp.dp.toPx(), dp.dp.toPx(), TileMode.Decal)
    }
}

private fun onTap(context: Context, act: Act, expanded: Boolean, s: IslandSettings, pos: Offset, width: Float) {
    if (expanded) {
        Controls.collapse()
        return
    }
    when (act) {
        is Act.StackA -> {
            // Плашка стопки — как обычное уведомление: нажатие открывает приложение,
            // а само уведомление уходит из стопки
            IslandBus.takeFromStack()?.let { n ->
                n.message?.let { m ->
                    if (!Controls.send(context, m.intent)) Controls.openApp(context, m.pkg)
                }
                n.openPkg?.let { Controls.openApp(context, it) }
                n.alarm?.let { openAlarm(context, it) }
            }
        }
        is Act.NoticeA -> {
            // Пока вводят ответ, нажатие по карточке не должно открывать приложение
            if (IslandBus.reply.value != null) return
            // Нажатие на уведомление приложения открывает его, как на iPhone
            act.n.message?.let { m ->
                if (!Controls.send(context, m.intent)) Controls.openApp(context, m.pkg)
            }
            act.n.openPkg?.let { Controls.openApp(context, it) }
            act.n.alarm?.let { openAlarm(context, it) }
            IslandBus.notice.value = null
        }
        // Пустой остров раскрывается, если это не выключили в «Поведении»
        is Act.Idle -> if (s.idleExpand) Controls.expand(act.id)
        // Оплата: нажатие открывает приложение оплаты (окно Google открывать нечего)
        is Act.PayA -> if (act.pkg != PayMonitor.GMS) Controls.openApp(context, act.pkg)
        is Act.TimerA -> if (act.t.ringing) Controls.cancelTimer() else perform(context, s.tapAction, act)
        is Act.CallA -> if (act.c.incoming) {
            Controls.openCall(context, act.c)
        } else {
            perform(context, s.tapAction, act)
        }
        else -> perform(context, s.tapAction, act)
    }
}

private fun perform(context: Context, action: TapAction, act: Act) {
    if (act.transient) return
    val hasMedia = IslandBus.media.value != null
    when (action) {
        TapAction.EXPAND -> Controls.expand(act.id)
        TapAction.OPEN_APP -> act.pkg?.let { Controls.openApp(context, it) } ?: Controls.expand(act.id)
        TapAction.PLAY_PAUSE -> if (hasMedia) Controls.playPause() else Controls.expand(act.id)
        TapAction.NEXT_TRACK -> if (hasMedia) Controls.next() else Controls.expand(act.id)
        TapAction.SCREENSHOT -> Controls.screenshot()
        TapAction.NOTHING -> Unit
    }
}

/** Вторая активность — круглый пузырь справа от острова. */
@Composable
private fun Bubble(act: Act, s: IslandSettings) {
    val h = activeHeight(s)
    val haptic = rememberHaptic()
    val tilt = IslandBus.glassTilt.collectAsState()
    val now = rememberNow(500, active = act is Act.TimerA)
    // «Двойной остров»: второй вырез — капсула, а не кружок
    val shape = if (s.dualNotch) RoundedCornerShape(h / 2) else CircleShape
    val dualWidth = if (s.dualNotch) h * 2.1f else h
    Box(
        Modifier
            .size(width = dualWidth, height = h)
            .then(if (s.shadow && !s.reduceEffects) Modifier.shadow(8.dp, shape) else Modifier)
            .clip(shape)
            .then(
                if (s.liquidGlass) Modifier.glassSurface(shape, Color(s.color), s.glassOpacity / 100f, tilt = { tilt.value })
                else Modifier.background(Color(s.color)),
            )
            .then(if (s.outline && s.borderSmall) Modifier.border(s.outlineWidth.dp, Color(s.outlineColor), shape) else Modifier)
            .clickable {
                haptic()
                Controls.expand(act.id)
            },
        contentAlignment = Alignment.Center,
    ) {
        val inner: Dp = h * 0.62f
        when (act) {
            is Act.MediaA -> Art(act.m.art, h * 0.72f, h / 2)
            is Act.TimerA -> {
                ProgressRing(
                    progress = if (act.t.totalMs > 0) act.t.left(now).toFloat() / act.t.totalMs else 0f,
                    color = Tints.Orange,
                    modifier = Modifier.size(inner),
                    stroke = 2.5.dp,
                )
                Icon(Ios.timer, null, tint = Tints.Orange, modifier = Modifier.size(inner * 0.5f))
            }
            is Act.StopwatchA -> Icon(Ios.stopwatch, null, tint = Tints.Orange, modifier = Modifier.size(inner * 0.8f))
            is Act.TorchA -> Icon(Ios.flashlight, null, tint = Tints.Yellow, modifier = Modifier.size(inner * 0.75f))
            is Act.ChronoA -> AppIcon(act.c.icon, inner, Ios.timer, Tints.Orange)
            is Act.ProgressA -> {
                ProgressRing(act.p.fraction, Tints.Blue, Modifier.size(inner), stroke = 2.5.dp)
                Icon(Ios.download, null, tint = Tints.Blue, modifier = Modifier.size(inner * 0.5f))
            }
            is Act.CallA -> Icon(Ios.call, null, tint = Tints.Green, modifier = Modifier.size(inner * 0.7f))
            is Act.LiveA -> LiveGlyph(act.l, inner, liveTint(act.l.kind))
            is Act.ChargingA -> Icon(Ios.flash, null, tint = Tints.Green, modifier = Modifier.size(inner * 0.7f))
            is Act.StackA -> {
                // Стопка в пузыре: значок последнего уведомления и счётчик в углу
                val n = act.items.lastOrNull()
                val bmp = n?.message?.appIcon ?: n?.appIcon
                if (bmp != null) {
                    Image(bmp, null, Modifier.size(inner * 0.82f).clip(RoundedCornerShape(inner * 0.22f)))
                } else {
                    Icon(Ios.bell, null, tint = Tints.Lavender, modifier = Modifier.size(inner * 0.7f))
                }
                if (act.items.size > 1) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(inner * 0.44f)
                            .clip(CircleShape)
                            .background(Tints.Lavender),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${act.items.size}", color = StackInk,
                            fontSize = (inner.value * 0.24f).sp, style = Digits, maxLines = 1,
                        )
                    }
                }
            }
            else -> Unit
        }
    }
}

/**
 * Свечение вокруг острова: десять колец, всё прозрачнее наружу, — мягкий ореол без размытия, в запасе окна.
 * У «Как вырез» верх прямой, как у самого острова.
 */
fun DrawScope.islandGlow(color: Color, alpha: Float, corner: Float, notch: Boolean = false) {
    val spread = 10.dp.toPx()
    for (i in 1..10) {
        val d = spread * i / 10f
        drawIslandRect(color, Offset(-d, -d), Size(size.width + 2 * d, size.height + 2 * d), corner + d, notch, alpha = 0.06f * alpha)
    }
}

/** Контур формы острова со скруглением [corner] в [topLeft] — для свечения, волн и кромки вокруг него. */
private fun DrawScope.islandOutline(topLeft: Offset, size: Size, corner: Float, notch: Boolean, draw: DrawScope.(Outline) -> Unit) {
    val outline = islandShape(corner.coerceAtLeast(0f).toDp(), notch).createOutline(size, layoutDirection, this)
    translate(topLeft.x, topLeft.y) { draw(outline) }
}

/** Прямоугольник той же формы, что остров: у «Как вырез» верхние углы прямые. */
fun DrawScope.drawIslandRect(
    color: Color,
    topLeft: Offset,
    size: Size,
    corner: Float,
    notch: Boolean,
    style: DrawStyle = Fill,
    alpha: Float = 1f,
) = islandOutline(topLeft, size, corner, notch) { drawOutline(it, color, alpha, style) }

fun DrawScope.drawIslandRect(
    brush: Brush,
    topLeft: Offset,
    size: Size,
    corner: Float,
    notch: Boolean,
    style: DrawStyle = Fill,
    alpha: Float = 1f,
) = islandOutline(topLeft, size, corner, notch) { drawOutline(it, brush, alpha, style) }

/** Форма острова: капсула со скруглениями или, у «Как вырез», прямой верх у края экрана. */
fun islandShape(corner: Dp, s: IslandSettings): RoundedCornerShape = islandShape(corner, s.notch)

fun islandShape(corner: Dp, notch: Boolean): RoundedCornerShape =
    if (notch) RoundedCornerShape(0.dp, 0.dp, corner, corner) else RoundedCornerShape(corner)

/** Форма острова со скруглением из анимации: рамка и стекло читают его при рисовании и обновляются без пересборки. */
private class AnimatedIslandShape(private val corner: State<Dp>, private val notch: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        islandShape(corner.value, notch).createOutline(size, layoutDirection, density)
}
