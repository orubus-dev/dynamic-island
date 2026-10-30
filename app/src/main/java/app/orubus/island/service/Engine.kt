package app.orubus.island.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import app.orubus.island.R
import app.orubus.island.core.IslandBus
import app.orubus.island.core.Notice
import app.orubus.island.core.NoticeStyle
import app.orubus.island.core.Tints
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.island.Ios
import app.orubus.island.ui.Perms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Движок острова: он знает, живы ли службы, и оживляет их, когда что-то упало.
 *
 * Три слоя оживления:
 *  1. Пульс. Островная служба раз в 10 секунд бьёт пульсом ([beat]); служба «Не выгружать остров»
 *     ([KeepAliveService]) смотрит за пульсом: stale — значит остров мёртв, хотя должен работать.
 *  2. Оживление. Если разрешение WRITE_SECURE_SETTINGS выдано через adb (беспроводная отладка),
 *     движок сам переключает службу спец. возможностей «выкл → вкл» — система привязывает её заново.
 *     Без разрешения — честно сообщает: «Остров упал», с кнопкой в настройки.
 *  3. Будильник после сбоя. Перехватываем необработанный сбой ([IslandApp]) и через полторы секунды
 *     будим процесс будильником ([Resurrector]) — так «Dynamic Island keeps stopping» больше не
 *     означает «остров умер до перезагрузки».
 */
object IslandEngine {
    enum class State { DOWN, UP, RECOVERING }

    data class Health(
        val state: State = State.DOWN,
        /** Окно острова на экране и пульс свежий */
        val overlay: Boolean = false,
        /** Служба уведомлений привязана */
        val notifListener: Boolean = false,
        val lastBeat: Long = 0L,
        val heals: Int = 0,
        val lastHeal: Long = 0L,
        /** WRITE_SECURE_SETTINGS выдан — остров может включить себя сам */
        val canSelfHeal: Boolean = false,
    )

    val health = MutableStateFlow(Health())

    private const val CHANNEL = "engine"
    private const val NOTIF_ID = 9
    private const val CHECK_MS = 10_000L
    private const val STALE_MS = 35_000L
    private const val HEAL_COOLDOWN_MS = 30_000L

    /** Живой пульс из [IslandService]. */
    fun beat() = health.update {
        it.copy(state = State.UP, overlay = true, lastBeat = SystemClock.elapsedRealtime())
    }

    /** Остров выключили штатно (спец. возможности или настройка) — это не поломка. */
    fun overlayGone() = health.update { it.copy(overlay = false, lastBeat = 0L) }

    fun notifListenerUp(up: Boolean) = health.update { it.copy(notifListener = up) }

    fun refreshSelfHeal(context: Context) = health.update {
        it.copy(canSelfHeal = canWriteSecureSettings(context))
    }

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Остров должен работать, но пульса нет — движок это уже заметил. */
    fun isBroken(): Boolean {
        if (!IslandBus.serviceRunning.value) return true
        val h = health.value
        return h.lastBeat == 0L || SystemClock.elapsedRealtime() - h.lastBeat > STALE_MS
    }

    /**
     * Оживить остров. Возвращает true, если удалось (или уже оживаем), false — если остаётся
     * только сообщить человеку.
     */
    fun heal(context: Context): Boolean {
        val now = SystemClock.elapsedRealtime()
        val h = health.value
        if (h.state == State.RECOVERING && now - h.lastHeal < HEAL_COOLDOWN_MS) return true
        health.update { it.copy(state = State.RECOVERING, heals = it.heals + 1, lastHeal = now) }
        val ok = reEnableAccessibility(context)
        if (!ok) announce(context)
        return ok
    }

    /**
     * Переключить службу спец. возможностей «выкл → вкл», чтобы система привязала её заново.
     * Работает только с WRITE_SECURE_SETTINGS: обычное приложение не имеет права трогать чужие
     * настройки доступности — разрешение выдаётся один раз через adb.
     */
    fun reEnableAccessibility(context: Context): Boolean = runCatching {
        if (!canWriteSecureSettings(context)) return false
        val cr = context.contentResolver
        val comp = ComponentName(context, IslandService::class.java).flattenToString()
        val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val services = current.split(':').filter { it.isNotBlank() }
        fun put(list: List<String>): Boolean = runCatching {
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, list.joinToString(":"))
        }.isSuccess
        if (!put(services)) return false
        if (comp in services) {
            // Служба числится включённой, но мертва: выключаем и через мгновение включаем обратно
            if (!put(services - comp)) return false
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { put(services) } }, 500)
        } else {
            if (!put(services + comp)) return false
        }
        runCatching { Settings.Secure.putInt(cr, "accessibility_enabled", 1) }
        refreshSelfHeal(context)
        true
    }.getOrDefault(false)

    /** Сообщить о падении: плашкой в острове, если он жив, и системным уведомлением — всегда. */
    private fun announce(context: Context) {
        if (IslandBus.serviceRunning.value) {
            IslandBus.post(
                Notice(
                    tr("Остров упал", "Island crashed"),
                    tr("Движок не смог оживить его сам", "The engine couldn't revive it on its own"),
                    Ios.load(context, R.drawable.ios_flash),
                    Tints.Red,
                    style = NoticeStyle.PILL,
                ),
            )
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Движок острова", "Island engine"), NotificationManager.IMPORTANCE_HIGH).apply {
                description = tr("Сообщает, когда остров остановился и его нужно включить", "Tells you when the island stopped and needs to be turned on")
                setShowBadge(false)
            },
        )
        val fix = PendingIntent.getActivity(
            context, 0,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tab_island)
            .setContentTitle(tr("Остров остановился", "The island stopped"))
            .setContentText(tr("Нажмите, чтобы включить его снова", "Tap to turn it back on"))
            .setContentIntent(fix)
            .setAutoCancel(true)
            .build()
        runCatching { nm?.notify(NOTIF_ID, notif) }
    }

    /** Цикл слежения: живёт в [KeepAliveService], проверяет пульс и оживляет упавший остров. */
    fun watch(context: Context, scope: CoroutineScope): Job = scope.launch(Dispatchers.Default) {
        refreshSelfHeal(context)
        while (isActive) {
            delay(CHECK_MS)
            val s = SettingsRepo.current
            if (!s.enabled || !s.autoHeal) continue
            if (!Perms.accessibility(context)) continue // выключен человеком — не наша работа включать
            if (!isBroken()) continue
            heal(context)
        }
    }

    /**
     * Разбудить процесс после сбоя: будильник через полторы секунды придёт в [Resurrector].
     * Точные будильники разрешены приложению (USE_EXACT_ALARM); где система их ограничивает —
     * ставим неточный, он тоже поднимет процесс.
     */
    fun scheduleRevival(context: Context) {
        if (!SettingsRepo.current.autoHeal) return
        runCatching {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val pi = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, Resurrector::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val at = System.currentTimeMillis() + 1_500
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}

/**
 * Разбудить остров после сбоя приложения, перезагрузки телефона или обновления.
 * BOOT_COMPLETED и MY_PACKAGE_REPLACED — защищённые системные рассылки, шлют их только система.
 */
class Resurrector : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            SettingsRepo.init(context.applicationContext)
            val s = SettingsRepo.current
            if (s.keepAlive) context.startForegroundService(Intent(context, KeepAliveService::class.java))
            // После сбоя система часто оставляет службу «включённой», но не привязанной:
            // переключение «выкл → вкл» заставляет систему привязать её заново
            if (s.autoHeal && IslandEngine.canWriteSecureSettings(context)) IslandEngine.reEnableAccessibility(context)
        }
    }
}
