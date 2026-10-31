package app.orubus.island.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import app.orubus.island.core.IslandBus
import app.orubus.island.core.TimerState
import app.orubus.island.core.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Доводит таймер острова до звонка и звонит, пока его не выключат. Пока экран погашен, телефон
 * засыпает и обычное ожидание стоит, поэтому к концу таймера его будит будильник системы.
 *
 * Таймер переживает смерть процесса: каждое состояние пишется в [TimerStore], при старте службы
 * таймер возвращается оттуда, а будильник приходит в манифестный [TimerWakeReceiver] — он звонит,
 * даже если острова уже нет (тогда — системным уведомлением).
 */
class TimerAlarm(private val context: Context, private val scope: CoroutineScope) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val wakeIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(TIMER_WAKE_ACTION).setClass(context, TimerWakeReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private var collectJob: Job? = null
    private var finishJob: Job? = null

    fun start() {
        // Сначала восстановление: коллектор ниже первым делом увидит восстановленный таймер,
        // а не null (иначе null сотрёт хранилище раньше времени)
        restoreSaved()
        collectJob = scope.launch { IslandBus.timer.collect { onTimer(it) } }
    }

    fun stop() {
        collectJob?.cancel()
        finishJob?.cancel()
        cancelWake()
        stopRinging()
    }

    private fun onTimer(t: TimerState?) {
        finishJob?.cancel()
        cancelWake()
        if (t == null || !t.ringing) stopRinging()
        if (t == null) {
            TimerStore.clear(context)
            return
        }
        // Каждое состояние — в хранилище: процесс умрёт, а таймер дозвенит
        TimerStore.save(context, t)
        if (t.ringing) {
            startRinging()
        } else if (t.pausedLeft == null) {
            scheduleWake(t.endAt)
            finishJob = scope.launch {
                delay(t.left(SystemClock.elapsedRealtime()))
                ringIfDue()
            }
        }
    }

    /** Таймер из хранилища: время могло выйти, пока процесса не было, — тогда звоним сразу. */
    private fun restoreSaved() {
        if (IslandBus.timer.value != null) return
        val t = TimerStore.restore(context) ?: return
        // Коллектор выше сам заведёт ожидание или звонок на этом состоянии
        IslandBus.timer.value = t
    }

    /** Время вышло — таймер звонит. Срабатывает от ожидания или от будильника, что раньше. */
    private fun ringIfDue() {
        val t = IslandBus.timer.value ?: return
        if (t.ringing || t.pausedLeft != null || t.left(SystemClock.elapsedRealtime()) > 100) return
        IslandBus.timer.value = t.copy(pausedLeft = 0, ringing = true)
    }

    private fun scheduleWake(endAt: Long) {
        val am = alarms ?: return
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAt, wakeIntent)
            } else {
                // Точные будильники запрещены в настройках — система разбудит чуть позже, но разбудит
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAt, wakeIntent)
            }
        }
    }

    private fun cancelWake() {
        runCatching { alarms?.cancel(wakeIntent) }
    }

    private fun startRinging() = TimerRinger.start(context)
    private fun stopRinging() = TimerRinger.stop()
}

/** Действие будильника таймера: приходит в манифестный [TimerWakeReceiver], даже если процесса не было. */
internal const val TIMER_WAKE_ACTION = "app.orubus.island.TIMER_WAKE"

/**
 * Звонок таймера вне службы: один на процесс, чтобы звонить и из службы острова, и из
 * манифестного приёмника [TimerWakeReceiver], когда острова уже нет.
 */
internal object TimerRinger {
    private var ringtone: Ringtone? = null
    private var vib: Vibrator? = null
    private var vibrating = false
    private var autoStopJob: Job? = null

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun start(context: Context) {
        if (ringtone != null || vibrating) return
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = runCatching {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
        }.getOrNull()
        vibrator(context)?.let {
            runCatching { it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0)) }
            vib = it
            vibrating = true
        }
        // Звонить вечно незачем: через минуту таймер гаснет сам
        autoStopJob = IslandBus.scope.launch {
            delay(60_000)
            if (IslandBus.timer.value?.ringing == true) IslandBus.timer.value = null
        }
    }

    fun stop() {
        autoStopJob?.cancel()
        autoStopJob = null
        ringtone?.let { runCatching { it.stop() } }
        ringtone = null
        if (vibrating) runCatching { vib?.cancel() }
        vib = null
        vibrating = false
    }

    /** Острова на экране нет: сообщить о звонке системным уведомлением (Android 13+ — при праве). */
    fun postNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, tr("Таймер острова", "Island timer"), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = tr("Звонит, когда время таймера вышло", "Rings when the timer runs out")
                    setShowBadge(false)
                },
            )
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val notif = Notification.Builder(context, CHANNEL)
                .setSmallIcon(app.orubus.island.R.drawable.ic_tab_island)
                .setContentTitle(tr("Таймер", "Timer"))
                .setContentText(tr("Время вышло", "Time's up"))
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .also { n -> if (open != null) n.setContentIntent(PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) }
                .build()
            nm.notify(NOTIF_ID, notif)
        }
    }

    private const val CHANNEL = "timer"
    private const val NOTIF_ID = 11
}

/**
 * Будильник таймера приходит сюда даже после смерти процесса: приёмник заявлен в манифесте.
 * Восстанавливает таймер из хранилища, звонит; без острова на экране — ещё и уведомлением.
 */
class TimerWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TIMER_WAKE_ACTION) return
        runCatching {
            // Процесс проснулся ровно на этом будильнике — таймера в памяти нет, вернём из хранилища
            if (IslandBus.timer.value == null) TimerStore.restore(context)?.let { IslandBus.timer.value = it }
            val t = IslandBus.timer.value ?: return
            if (!t.ringing && t.pausedLeft == null && t.left(SystemClock.elapsedRealtime()) <= 100) {
                IslandBus.timer.value = t.copy(pausedLeft = 0, ringing = true)
            }
            if (IslandBus.timer.value?.ringing == true) {
                TimerRinger.start(context.applicationContext)
                if (!IslandBus.serviceRunning.value) TimerRinger.postNotification(context.applicationContext)
            }
        }
    }
}

/** Таймер между жизнями процесса: SharedPreferences и настенные часы (elapsedRealtime после ребута чужой). */
internal object TimerStore {
    private const val FILE = "timer_state"
    private const val KEY = "timer"

    fun save(context: Context, t: TimerState) {
        runCatching {
            // endAt живёт в elapsedRealtime, для перезагрузки дублируем остаток настенными часами
            val wallEnd = System.currentTimeMillis() + (t.endAt - SystemClock.elapsedRealtime())
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString(KEY, "${t.totalMs};$wallEnd;${t.pausedLeft ?: -1L};${if (t.ringing) 1 else 0}")
                .apply()
        }
    }

    fun clear(context: Context) {
        runCatching { context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(KEY).apply() }
    }

    /**
     * Вернуть таймер после смерти процесса или перезагрузки: остаток считается по настенным часам.
     * Истёк только что — вернём звонящим; давно (старше полутора минут) — null, хранилище чистим.
     */
    fun restore(context: Context): TimerState? = runCatching {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: return@runCatching null
        val p = raw.split(';')
        val total = p.getOrNull(0)?.toLongOrNull()
        val wallEnd = p.getOrNull(1)?.toLongOrNull()
        if (p.size < 4 || total == null || wallEnd == null) return@runCatching null
        val paused = p[2].toLongOrNull()?.takeIf { it >= 0 }
        val ringing = p[3] == "1"
        val now = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val state = when {
            ringing -> TimerState(total, now, pausedLeft = 0, ringing = true)
            paused != null -> TimerState(total, now + paused, pausedLeft = paused)
            else -> {
                val leftWall = wallEnd - nowWall
                when {
                    leftWall > 0 -> TimerState(total, now + leftWall)
                    // Время вышло, пока процесса не было: дозвеним, если это случилось только что
                    nowWall - wallEnd <= 1_500 -> TimerState(total, now, pausedLeft = 0, ringing = true)
                    else -> {
                        clear(context)
                        return@runCatching null
                    }
                }
            }
        }
        state
    }.getOrNull()
}
