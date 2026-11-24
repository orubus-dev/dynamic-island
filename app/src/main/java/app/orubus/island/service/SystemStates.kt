package app.orubus.island.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import app.orubus.island.core.IslandBus
import app.orubus.island.core.SysKind
import app.orubus.island.core.SysState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Состояния телефона — постоянные капсулы, пока что-то включено (раздел «Состояния»):
 * VPN, точка доступа, экономия заряда, «Не беспокоить», режим полёта, без звука/вибрация,
 * низкий заряд, роуминг, мало места и отсутствие SIM.
 *
 * Всё, что умеет IslandBus, берём оттуда (заряд — из SystemEvents, хотспот — из PrivacyMonitor),
 * остальное слушаем сами. Раз в минуту тихая проверка медленно меняющегося: место и SIM.
 */
object SystemStates {
    private var receiver: BroadcastReceiver? = null
    private var vpnCallback: ConnectivityManager.NetworkCallback? = null
    private var roamingObserver: ContentObserver? = null

    fun start(context: Context, scope: CoroutineScope) {
        if (receiver != null) return
        val ctx = context.applicationContext
        appContext = ctx
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = refresh(ctx)
        }.also { r ->
            val filter = IntentFilter().apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            ContextCompat.registerReceiver(ctx, r, filter, ContextCompat.RECEIVER_EXPORTED)
        }

        // VPN: сеть с транспортом VPN есть — туннель поднят
        runCatching {
            val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return@runCatching
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refresh(ctx)
                override fun onLost(network: Network) = refresh(ctx)
            }
            vpnCallback = callback
            cm.registerNetworkCallback(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_VPN).build(), callback)
        }

        // Роуминг данных: переключатель в глобальных настройках
        runCatching {
            val cr = ctx.contentResolver
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = refresh(ctx)
            }
            roamingObserver = observer
            cr.registerContentObserver(Settings.Global.getUriFor(Settings.Global.DATA_ROAMING), false, observer)
        }

        refresh(ctx)
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(60_000)
                refresh(ctx)
            }
        }
    }

    fun stop() {
        receiver?.let { r -> runCatching { appContext?.unregisterReceiver(r) } }
        receiver = null
        vpnCallback?.let { cb ->
            runCatching { ctxConnectivityManager()?.unregisterNetworkCallback(cb) }
        }
        vpnCallback = null
        roamingObserver?.let { ob -> runCatching { cr()?.unregisterContentObserver(ob) } }
        roamingObserver = null
        appContext = null
        IslandBus.sysStates.value = emptyList()
    }

    private var appContext: Context? = null
    private fun ctxConnectivityManager(): ConnectivityManager? =
        appContext?.getSystemService(ConnectivityManager::class.java)
    private fun cr(): ContentResolver? = appContext?.contentResolver

    /** Пересобрать список состояний: быстро и без исключений — плохой датчик не должен ломать остров. */
    fun refresh(context: Context) {
        appContext = context.applicationContext
        val out = mutableListOf<SysState>()

        runCatching {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val vpn = cm?.allNetworks?.any { n ->
                val caps = cm.getNetworkCapabilities(n)
                caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } == true
            if (vpn) out += SysState(SysKind.VPN)
        }

        if (IslandBus.hotspotOn.value) out += SysState(SysKind.HOTSPOT)

        runCatching {
            val pm = context.getSystemService(PowerManager::class.java)
            if (pm?.isPowerSaveMode == true) out += SysState(SysKind.POWER_SAVE)
        }

        runCatching {
            val nm = context.getSystemService(NotificationManager::class.java)
            val f = nm?.currentInterruptionFilter
            if (f != null && f != NotificationManager.INTERRUPTION_FILTER_ALL) out += SysState(SysKind.DND)
        }

        runCatching {
            if (Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
                out += SysState(SysKind.AIRPLANE)
            }
        }

        runCatching {
            when (context.getSystemService(AudioManager::class.java)?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> out += SysState(SysKind.SILENT)
                AudioManager.RINGER_MODE_VIBRATE -> out += SysState(SysKind.VIBRATE)
            }
        }

        val b = IslandBus.battery.value
        if (b.level in 0..15 && !b.charging) out += SysState(SysKind.LOW_BATTERY, b.level)

        runCatching {
            if (Settings.Global.getInt(context.contentResolver, Settings.Global.DATA_ROAMING, 0) == 1 &&
                Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 0
            ) {
                out += SysState(SysKind.ROAMING)
            }
        }

        runCatching {
            val dir = Environment.getDataDirectory()
            val bytes = StatFs(dir.path).availableBytes
            if (bytes < 1_500_000_000L) out += SysState(SysKind.STORAGE, (bytes / 1_000_000_000L).toInt())
        }

        runCatching {
            val state = context.getSystemService(TelephonyManager::class.java)?.simState
            if (state == TelephonyManager.SIM_STATE_ABSENT || state == TelephonyManager.SIM_STATE_UNKNOWN) {
                out += SysState(SysKind.NO_SIM)
            }
        }

        val next = out.distinctBy { it.stableKey }
        if (IslandBus.sysStates.value.map { it.stableKey } != next.map { it.stableKey }) {
            IslandBus.sysStates.value = next
        }
    }
}
