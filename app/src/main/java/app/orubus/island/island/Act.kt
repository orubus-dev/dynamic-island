package app.orubus.island.island

import app.orubus.island.core.AlarmInfo
import app.orubus.island.core.BatteryInfo
import app.orubus.island.core.CallInfo
import app.orubus.island.core.ChronoInfo
import app.orubus.island.core.LiveInfo
import app.orubus.island.core.MediaInfo
import app.orubus.island.core.Notice
import app.orubus.island.core.PayReady
import app.orubus.island.core.ProgressInfo
import app.orubus.island.core.StopwatchState
import app.orubus.island.core.TimerState
import app.orubus.island.core.TorchState

/** То, что сейчас может показывать остров. Короткие (transient) не раскрываются и не уходят в пузырь. */
sealed class Act(val id: String, val transient: Boolean = false) {
    open val pkg: String? get() = null

    data object Idle : Act("idle")
    data class NoticeA(val n: Notice) : Act("notice", transient = true)
    /**
     * Свернутые уведомления — значок со счётчиком: смахнули плашку вверх, и она превратилась
     * в значок последнего уведомления со счётчиком справа. Не transient: может уйти в пузырь
     * и раскрыться на быстрые кнопки.
     */
    data class StackA(val items: List<Notice>) : Act("stack")
    data class TimerA(val t: TimerState) : Act("timer")
    data class StopwatchA(val w: StopwatchState) : Act("stopwatch")
    data class TorchA(val t: TorchState) : Act("torch")
    data class ChronoA(val c: ChronoInfo) : Act("chrono") {
        override val pkg: String get() = c.pkg
    }
    data class MediaA(val m: MediaInfo) : Act("media") {
        override val pkg: String get() = m.pkg
    }
    data class ProgressA(val p: ProgressInfo) : Act("progress") {
        override val pkg: String get() = p.pkg
    }
    data class CallA(val c: CallInfo) : Act("call") {
        override val pkg: String get() = c.pkg
    }
    data class LiveA(val l: LiveInfo) : Act("live") {
        override val pkg: String get() = l.pkg
    }
    data class ChargingA(val b: BatteryInfo) : Act("charging")
    /** Оплата телефоном: ждём терминал. Не раскрывается и не уходит в пузырь */
    data class PayA(val p: PayReady) : Act("pay", transient = true) {
        override val pkg: String get() = p.pkg
    }
    data class AlarmA(val a: AlarmInfo) : Act("alarm") {
        override val pkg: String? get() = a.pkg
    }
}
