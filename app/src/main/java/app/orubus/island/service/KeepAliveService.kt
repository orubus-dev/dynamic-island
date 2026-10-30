package app.orubus.island.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.orubus.island.R
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.Perms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * «Не выгружать остров»: служба на переднем плане с тихим уведомлением. Процесс с такой службой Android
 * не закрывает вместе с приложением, когда его смахивают из недавних, — остров остаётся на экране.
 * С 1.7 она же — дом движка острова ([IslandEngine]): пока служба жива, она следит за пульсом
 * островной службы и оживляет её, если что-то упало.
 */
class KeepAliveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watch: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SettingsRepo.init(this)
        // Запущенная через startForegroundService служба обязана выйти на передний план, даже если её уже выключили:
        // иначе Android закрывает приложение с ошибкой. Поэтому сначала выходим, потом при необходимости уходим
        val shown = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(ID, notification())
            }
        }.isSuccess
        if (!shown || !SettingsRepo.current.keepAlive) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Движок: следить за пульсом острова и оживлять его (один цикл на жизнь службы)
        if (watch == null) watch = IslandEngine.watch(this, scope)
        return START_STICKY
    }

    override fun onDestroy() {
        watch?.cancel()
        watch = null
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(): Notification {
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Работа острова", "Island running"), NotificationManager.IMPORTANCE_MIN).apply {
                description = tr("Не даёт Android закрыть остров, когда его смахивают из недавних",
                    "Keeps Android from closing the island when it's swiped away from recent apps")
                setShowBadge(false)
            },
        )
        // Нажатие открывает настройки острова
        val open = packageManager.getLaunchIntentForPackage(packageName)
            ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tab_island)
            .setContentTitle(tr("Остров работает", "The island is running"))
            .setContentText(tr("Уведомление не даёт системе закрыть остров", "This notification keeps the system from closing the island"))
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val ID = 7
        private const val CHANNEL = "keep_alive"

        /**
         * Запустить или остановить по настройке. Служба нужна, только пока включён сам остров (спец. возможности).
         * Из фона Android 12+ может её не запустить — тогда она запустится, когда откроют приложение.
         */
        fun sync(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (SettingsRepo.current.keepAlive && Perms.accessibility(context)) {
                runCatching { context.startForegroundService(intent) }
            } else {
                context.stopService(intent)
            }
        }
    }
}
