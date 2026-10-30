package app.orubus.island.island

import android.media.AudioManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** Что жест может сделать с текущей активностью — пересчитывается на каждом жесте. */
data class SwipeCaps(
    val canExpand: Boolean,
    val canCollapse: Boolean,
    val canDismiss: Boolean,
    val canTracks: Boolean,
)

/** Смещение карточки под пальцем: читается при рисовании, поэтому жест не пересобирает остров. */
class SwipeState {
    val x = mutableFloatStateOf(0f)
    val y = mutableFloatStateOf(0f)
    val alpha = mutableFloatStateOf(1f)
    /** Идущая анимация возврата/вылета — новый жест её отменяет */
    var animJob: kotlinx.coroutines.Job? = null

    fun reset() {
        x.floatValue = 0f
        y.floatValue = 0f
        alpha.floatValue = 1f
    }
}

/** Пружиной возвращаем смещение к нулю: каждый кадр Animatable пишется в состояние, которое читает рисование. */
private fun springBack(scope: CoroutineScope, state: SwipeState) {
    state.animJob?.cancel()
    val animX = Animatable(state.x.floatValue)
    val animY = Animatable(state.y.floatValue)
    val animA = Animatable(state.alpha.floatValue)
    state.animJob = scope.launch {
        kotlinx.coroutines.coroutineScope {
            launch { snapshotFlow { animX.value }.collect { state.x.floatValue = it } }
            launch { snapshotFlow { animY.value }.collect { state.y.floatValue = it } }
            launch { snapshotFlow { animA.value }.collect { state.alpha.floatValue = it } }
            launch { animX.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
            launch { animY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) }
            launch { animA.animateTo(1f, tween(180)) }
        }
        state.reset()
    }
}

/** Плашка улетает за край и тает, потом состояние сбрасывается. */
private fun flyOut(scope: CoroutineScope, state: SwipeState, toX: Float, toY: Float, after: () -> Unit) {
    state.animJob?.cancel()
    val animX = Animatable(state.x.floatValue)
    val animY = Animatable(state.y.floatValue)
    val animA = Animatable(state.alpha.floatValue)
    state.animJob = scope.launch {
        kotlinx.coroutines.coroutineScope {
            launch { snapshotFlow { animX.value }.collect { state.x.floatValue = it } }
            launch { snapshotFlow { animY.value }.collect { state.y.floatValue = it } }
            launch { snapshotFlow { animA.value }.collect { state.alpha.floatValue = it } }
            launch { animX.animateTo(toX, tween(150)) }
            launch { animY.animateTo(toY, tween(150)) }
            launch { animA.animateTo(0f, tween(140)) }
        }
        after()
        state.reset()
    }
}

/**
 * Свайпы острова, собранные вручную поверх pointerInput: карточка едет за пальцем, у жеста есть
 * скорость (быстрый короткий бросок тоже срабатывает), горизонталь и вертикаль различаются сразу,
 * и смахнутая плашка улетает за край, а не исчезает на месте.
 *
 * Живёт рядом с detectTapGestures: пока ось не выбрана, жест отменяется, если события обработали
 * нажатия (тап, двойной тап, долгое); после выбора оси смещения consumed — и нажатия отходят.
 */
fun Modifier.islandSwipe(
    state: SwipeState,
    caps: () -> SwipeCaps,
    onPull: (Float) -> Unit,
    onPullEnd: () -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onTracks: (next: Boolean) -> Unit,
) = pointerInput(Unit) {
    val animScope = CoroutineScope(coroutineContext + SupervisorJob())
    val slop = viewConfiguration.touchSlop
    val sidePx = 56.dp.toPx()
    val dismissPx = 64.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.animJob?.cancel()
        state.reset()
        val tracker = VelocityTracker()
        var dx = 0f
        var dy = 0f
        var axis = 0 // 0 — ещё не выбрали, 1 — горизонталь, 2 — вертикаль
        var pulled = false

        fun settle() {
            val c = caps()
            when (axis) {
                1 -> when {
                    c.canDismiss -> {
                        state.x.floatValue = dx
                        state.alpha.floatValue = 1f - (abs(dx) / (size.width.toFloat() * 0.9f)).coerceIn(0f, 0.6f)
                    }
                    c.canTracks -> state.x.floatValue = (dx * 0.22f).coerceIn(-sidePx * 0.45f, sidePx * 0.45f)
                    else -> state.x.floatValue = (dx * 0.12f).coerceIn(-12.dp.toPx(), 12.dp.toPx())
                }
                2 -> when {
                    dy < 0f && c.canDismiss -> {
                        state.y.floatValue = dy
                        state.alpha.floatValue = 1f - (-dy / (dismissPx * 2.2f)).coerceIn(0f, 0.6f)
                    }
                    dy > 0f -> state.y.floatValue = (dy * 0.4f).coerceAtMost(22.dp.toPx())
                    else -> state.y.floatValue = (dy * 0.25f).coerceAtLeast(-22.dp.toPx())
                }
            }
        }

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
            if (change == null) continue
            if (!change.pressed) {
                // Палец отпущен: считаем скорость и решаем, что случилось
                tracker.addPosition(change.uptimeMillis, change.position)
                val v = tracker.calculateVelocity()
                val c = caps()
                when (axis) {
                    1 -> {
                        val far = abs(dx) > size.width * 0.32f || (abs(v.x) > 1500f && sign(v.x) == sign(dx))
                        when {
                            c.canDismiss && far -> {
                                val dir = if (sign(dx) == 0f) 1f else sign(dx)
                                flyOut(animScope, state, dir * size.width * 1.4f, 0f, onDismiss)
                            }
                            c.canTracks && (abs(dx) > sidePx || (abs(v.x) > 1200f && abs(dx) > slop * 2)) -> {
                                // Толкнуть влево — следующий трек, вправо — предыдущий
                                onTracks(dx < 0)
                                springBack(animScope, state)
                            }
                            else -> springBack(animScope, state)
                        }
                    }
                    2 -> {
                        val upFar = -dy > dismissPx || v.y < -1400f
                        when {
                            dy < 0f && c.canDismiss && upFar -> flyOut(animScope, state, 0f, -size.height * 1.6f, onDismiss)
                            dy > 0f && c.canExpand && (dy > dismissPx || v.y > 1100f) -> {
                                onExpand()
                                springBack(animScope, state)
                            }
                            dy < 0f && c.canCollapse && (-dy > dismissPx || v.y < -1100f) -> {
                                onCollapse()
                                springBack(animScope, state)
                            }
                            else -> springBack(animScope, state)
                        }
                    }
                }
                onPullEnd()
                break
            }
            tracker.addPosition(change.uptimeMillis, change.position)
            val step = change.positionChange()
            dx += step.x
            dy += step.y
            if (axis == 0) {
                // События уже обработали нажатия (тап/долгое) — свайп не начнётся
                if (change.isConsumed) break
                if (Offset(dx, dy).getDistance() < slop) continue
                axis = if (abs(dx) > abs(dy)) 1 else 2
                pulled = axis == 2 && dy > 0f
            }
            change.consume()
            settle()
            if (pulled && axis == 2 && caps().canExpand) {
                onPull((dy / (dismissPx * 1.6f)).coerceIn(0f, 1f))
            }
        }
    }
}

/**
 * Долгое нажатие и скольжение — ползунок громкости. Ждём палец без движения дольше, чем тап,
 * и только там, где [accept] разрешает (правая половина); дальше скольжение по горизонтали
 * (или вертикали) меняет громкость. События читаем, даже если их обработал детектор нажатий:
 * после долгого нажатия он глушит всё до отпускания.
 */
fun Modifier.volumeSlide(
    accept: (Offset) -> Boolean,
    onBegin: () -> Unit,
    onDelta: (dx: Float, dy: Float) -> Unit,
    onEnd: () -> Unit,
) = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!accept(down.position)) {
            // Не наше — тихо дожидаемся отпускания; awaitEachGesture сам начнёт следующий жест
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Main)
                if (ev.changes.none { it.pressed }) break
            }
            return@awaitEachGesture
        }
        var longFired = false
        var drift = 0f
        while (true) {
            val ev = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                awaitPointerEvent(PointerEventPass.Main)
            }
            if (ev == null) {
                longFired = true
                break
            }
            val c = ev.changes.firstOrNull { it.pressed }
            if (c == null) break
            drift += c.positionChange().getDistance()
            if (drift > slop) break
        }
        if (!longFired) return@awaitEachGesture
        onBegin()
        var last = down.position
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Main)
            // Только исходный палец двигает громкость: второй палец не должен дёргать ползунок
            val c = ev.changes.firstOrNull { it.id == down.id } ?: continue
            if (!c.pressed) {
                onEnd()
                break
            }
            val p = c.position
            onDelta(p.x - last.x, p.y - last.y)
            last = p
            c.consume()
        }
    }
}

/** Громкость медиа для ползунка острова: уровень, максимум и скольжение. */
object IslandVolume {
    var level by mutableStateOf(0)
        private set
    var max by mutableStateOf(15)
        private set
    var mode by mutableStateOf(false)
        private set
    private var audio: AudioManager? = null
    private var startLevel = 0
    // Весь путь пальца с начала жеста: шаг громкости считается от полного пути,
    // а не от дельты одного события (событие несёт лишь несколько пикселей).
    private var accX = 0f
    private var accY = 0f

    fun attach(context: android.content.Context) {
        if (audio == null) {
            audio = context.getSystemService(AudioManager::class.java)
            max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            level = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        }
    }

    fun begin() {
        startLevel = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        level = startLevel
        accX = 0f
        accY = 0f
        mode = true
    }

    /** Скольжение: полный путь из px в шаги громкости — примерно 52 dp на шаг, вертикаль вверх — громче. */
    fun drag(dx: Float, dy: Float, pxPerStep: Float) {
        val a = audio ?: return
        accX += dx
        accY += dy
        val horizontal = (accX / pxPerStep).roundToInt()
        val vertical = (-accY / (pxPerStep * 1.25f)).roundToInt()
        val step = if (abs(horizontal) >= abs(vertical)) horizontal else vertical
        val next = (startLevel + step).coerceIn(0, max)
        if (next != level) {
            level = next
            runCatching { a.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0) }
        }
    }

    fun end() {
        mode = false
    }
}

/** Ползунок громкости вместо содержимого острова. */
@Composable
fun VolumeBarContent(modifier: Modifier = Modifier, tint: Color, onTick: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(context) {
        IslandVolume.attach(context)
        onDispose { }
    }
    val level by animateIntAsState(IslandVolume.level, tween(120), label = "vol")
    val fraction = if (IslandVolume.max > 0) level.toFloat() / IslandVolume.max else 0f
    val lastLevel = remember { mutableIntStateOf(IslandVolume.level) }
    if (lastLevel.intValue != IslandVolume.level) {
        lastLevel.intValue = IslandVolume.level
        onTick()
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.VolumeDown, null, tint = tint.copy(alpha = 0.75f), modifier = Modifier.height(18.dp))
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .background(tint.copy(alpha = 0.22f), CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .background(tint, CircleShape),
            )
        }
        Icon(Icons.Rounded.VolumeUp, null, tint = tint, modifier = Modifier.height(18.dp))
    }
}
