package app.orubus.island.service

import android.app.AppOpsManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.lang.reflect.Proxy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import app.orubus.island.core.IslandBus
import app.orubus.island.core.Notice
import app.orubus.island.core.NoticeStyle
import app.orubus.island.core.Tints
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Приватность и связь: точки микрофона и камеры у острова, оповещения о записи,
 * статус хотспота и демонстрация Face Unlock.
 *
 * Микрофон — AudioRecordingCallback, камера — CameraManager.AvailabilityCallback.
 * Хотспот: с Android 11 — публичный TetheringManager, события приходят мгновенно без опроса;
 * на старых версиях — раз в 2 секунды скрытый (но стабильный с API 14) список
 * getTetheredIfaces и резерв по именам поднятых интерфейсов (ap0/swlan0/softap/rndis/ncm).
 */
object PrivacyMonitor {
    private var mic = false
    private var cam = false
    private var started = false
    private var pollJob: Job? = null
    private var hotspotLast = false
    private val main = Handler(Looper.getMainLooper())
    private var tetherManager: Any? = null
    private var tetherProxy: Any? = null

    // Кто именно пишет звук / держит камеру — из AppOps (короткие коды стабильны годами)
    private const val OP_RECORD_AUDIO = 27
    private const val OP_CAMERA = 26
    /** Фонарик горит: вспышка держит камеру — не спутать её с камерой */
    @Volatile
    private var torchOn = false
    private var micPkg: String? = null
    private var camPkg: String? = null
    private var appContext: Context? = null

    fun start(context: Context, scope: CoroutineScope) {
        if (started) return
        started = true
        val app = context.applicationContext
        appContext = app

        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.registerAudioRecordingCallback(object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                val active = configs.isNotEmpty()
                if (active != mic) {
                    mic = active
                    IslandBus.micActive.value = active
                    if (active) announce()
                }
            }
        }, null)

        val camera = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // Фонарик тоже открывает камеру. Следим за вспышкой отдельно: пока она горит (даже у чужого
        // приложения), «камеру в использовании» не объявляем — иначе фонарик выдавал себя за камеру
        camera.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchOn = enabled
                if (enabled && cam && camPkg == null) {
                    cam = false
                    IslandBus.camActive.value = false
                }
            }
        }, null)
        camera.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                if (isMainCamera(app, cameraId)) {
                    cam = false
                    if (camPkg == null) IslandBus.camActive.value = false
                }
            }

            override fun onCameraUnavailable(cameraId: String) {
                if (!isMainCamera(app, cameraId)) return
                // Фонарик открывает камеру тем же вызовом: смотрим на вспышку чуть позже,
                // когда её состояние уже точно дошло, и только тогда объявляем камеру
                main.postDelayed({
                    if (!started || torchOn || camPkg != null) return@postDelayed
                    cam = true
                    IslandBus.camActive.value = true
                    announce()
                }, 250)
            }
        }, null)

        watchAppOps(app)

        // Хотспот: сначала текущее состояние (без плашки), дальше — события или опрос.
        hotspotLast = tetheredNow(app)
        IslandBus.hotspotOn.value = hotspotLast

        // Основной путь (Android 11+): публичный TetheringManager присылает tethered-интерфейсы
        // событием — плашка приходит мгновенно, без опроса. Не вышло (старее/ограничения) — опрос ниже.
        if (Build.VERSION.SDK_INT >= 30 && registerTetheringEvents(app)) return

        // Резерв: проверка каждые 2 секунды, чтобы плашка приходила почти сразу
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2_000)
                val on = tetheredNow(app) || interfacesLookHotspot()
                if (on != hotspotLast) setHotspot(on)
            }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        pollJob?.cancel()
        pollJob = null
        runCatching {
            val tm = tetherManager
            val proxy = tetherProxy
            if (tm != null && proxy != null) {
                tm.javaClass.methods.firstOrNull { it.name == "unregisterTetheringEventCallback" }?.invoke(tm, proxy)
            }
        }
        tetherProxy = null
        tetherManager = null
        main.removeCallbacksAndMessages(null)
        IslandBus.micActive.value = false
        IslandBus.camActive.value = false
        IslandBus.hotspotOn.value = false
    }

    /** Подписка на активные операции микрофона и камеры: даёт и факт использования, и имя приложения.
    *  Интерфейс OnOpActiveChangedListener различается между версиями Android — вызываем всё через Proxy. */
    private fun watchAppOps(app: Context) {
        runCatching {
            val ops = app.getSystemService(AppOpsManager::class.java) ?: return
            val iface = Class.forName("android.app.AppOpsManager\$OnOpActiveChangedListener")
            val proxy = Proxy.newProxyInstance(iface.classLoader, arrayOf(iface)) { _, method, args ->
                if (method.name == "onOpActiveChanged" && args != null && args.size >= 4) {
                    val pkg = args.filterIsInstance<String>().lastOrNull()
                    val active = args.last() as? Boolean ?: false
                    val isMic = when (val op = args[0]) {
                        is Int -> op == OP_RECORD_AUDIO
                        is String -> op == "android:record_audio"
                        else -> false
                    }
                    if (isMic) {
                        micPkg = if (active) pkg else null
                        IslandBus.micActive.value = active
                        if (active) announce()
                    } else {
                        camPkg = if (active) pkg else null
                        IslandBus.camActive.value = active
                        if (active) announce()
                    }
                }
                null
            }
            val array = intArrayOf(OP_RECORD_AUDIO, OP_CAMERA)
            // Публичный вариант — с именем вызывающего пакета; на старых версиях — двухаргументный
            val m3 = runCatching {
                AppOpsManager::class.java.getMethod("startWatchingActive", IntArray::class.java, String::class.java, iface)
            }.getOrNull()
            val m2 = runCatching {
                AppOpsManager::class.java.getMethod("startWatchingActive", IntArray::class.java, iface)
            }.getOrNull()
            when {
                m3 != null -> m3.invoke(ops, array, app.packageName, proxy)
                m2 != null -> m2.invoke(ops, array, proxy)
                else -> return
            }
        }
    }

    /** Человекочитаемое имя приложения по пакету; не вышло — сам пакет. */
    private fun appLabel(context: Context?, pkg: String?): String? {
        if (context == null || pkg == null) return null
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
    }

    /** Подписка на события хотспота: TetheringManager — SystemApi, поэтому рефлексия и Proxy.
    *  true — подписались, опрос не нужен. */
    private fun registerTetheringEvents(app: Context): Boolean = runCatching {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return@runCatching false
        val tm = runCatching { app.getSystemService(Class.forName("android.net.TetheringManager")) }.getOrNull()
            ?: runCatching { ConnectivityManager::class.java.getMethod("getTetheringManager").invoke(cm) }.getOrNull()
            ?: return@runCatching false
        val cbClass = Class.forName("android.net.TetheringManager\$TetheringEventCallback")
        val proxy = Proxy.newProxyInstance(cbClass.classLoader, arrayOf(cbClass)) { _, method, args ->
            if (method.name == "onTetheredInterfacesChanged") {
                val list = args?.firstOrNull() as? List<*>
                setHotspot(!list.isNullOrEmpty())
            }
            null
        }
        val reg = tm.javaClass.methods.firstOrNull {
            it.name == "registerTetheringEventCallback" && it.parameterTypes.size == 2 &&
                java.util.concurrent.Executor::class.java.isAssignableFrom(it.parameterTypes[0])
        } ?: return@runCatching false
        reg.invoke(tm, ContextCompat.getMainExecutor(app), proxy)
        tetherManager = tm
        tetherProxy = proxy
        true
    }.getOrDefault(false)

    /** Подключён ли хотспот прямо сейчас: системный список tethered-интерфейсов (скрытый, но стабильный). */
    private fun tetheredNow(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return@runCatching false
        val method = ConnectivityManager::class.java.getMethod("getTetheredIfaces")
        val ifaces = method.invoke(cm) as? Array<*> ?: return@runCatching false
        ifaces.isNotEmpty()
    }.getOrDefault(false)

    /** Резерв по именам интерфейсов: ap0/swlan0/softap — плюс USB-модем rndis/ncm. Только поднятые. */
    private fun interfacesLookHotspot(): Boolean = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.any { n ->
            // Интерфейс может существовать, но быть выключен (ap0 лежит на многих телефонах всегда)
            if (!runCatching { n.isUp }.getOrDefault(false)) return@any false
            val name = n.name.lowercase()
            name.startsWith("ap") || name.contains("swlan") || name.contains("softap") ||
                name.startsWith("rndis") || name.startsWith("ncm")
        } ?: false
    }.getOrDefault(false)

    private fun setHotspot(on: Boolean) {
        if (on == hotspotLast) return
        hotspotLast = on
        IslandBus.hotspotOn.value = on
        if (SettingsRepo.current.evHotspot) {
            IslandBus.post(
                Notice(
                    tr("Точка доступа", "Hotspot"),
                    if (on) tr("включена", "is on") else tr("выключена", "is off"),
                    Icons.Rounded.WifiTethering,
                    if (on) Tints.Green else Tints.Gray,
                    style = NoticeStyle.PILL,
                ),
            )
        }
    }

    private fun announce() {
        val s = SettingsRepo.current
        if (!s.privacyDots) return
        val showMic = mic && s.evMic
        val showCam = cam && s.evCamera
        if (!showMic && !showCam) return
        val title = when {
            showMic && showCam -> tr("Микрофон и камера", "Mic and camera")
            showMic -> tr("Микрофон", "Microphone")
            else -> tr("Камера", "Camera")
        }
        // Знаем, кто именно пользуется — зовём по имени
        val who = appLabel(appContext, micPkg ?: camPkg)
        val camera = showCam && (who != null || !showMic)
        // У камеры — своя анимация: зелёная пульсирующая линза (NoticeStyle.CAM).
        // Микрофон показывает прежнюю оранжевую пилюлю с качающимся значком
        when {
            camera && who != null -> IslandBus.post(
                Notice(title, who, Icons.Rounded.Videocam, Tints.Green, style = NoticeStyle.CAM),
            )
            camera -> IslandBus.post(
                Notice(
                    title,
                    tr("используется приложением", "in use by an app"),
                    Icons.Rounded.Videocam,
                    Tints.Green,
                    style = NoticeStyle.CAM,
                    extraMs = 800,
                ),
            )
            who != null -> IslandBus.post(
                Notice(title, who, Icons.Rounded.Mic, Tints.Orange),
            )
            else -> IslandBus.post(
                Notice(
                    title,
                    tr("используется приложением", "in use by an app"),
                    Icons.Rounded.Mic,
                    Tints.Orange,
                    style = NoticeStyle.PILL,
                    extraMs = 800,
                ),
            )
        }
    }

    private fun isMainCamera(context: Context, cameraId: String): Boolean {
        val camera = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return runCatching {
            val facing = camera.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK || facing == CameraCharacteristics.LENS_FACING_FRONT
        }.getOrDefault(false)
    }

    /** Демо-плашка «Попробовать» для Face Unlock в карусели «Что нового». */
    fun demoNotice(): Notice = Notice(
        "",
        tr("Разблокировано", "Unlocked"),
        Icons.Rounded.Face as ImageVector,
        Tints.Green,
        style = NoticeStyle.FACEID,
        extraMs = 600,
    )

    /** Демо хотспота для «Проверки». */
    fun demoHotspot() {
        IslandBus.post(
            Notice(
                tr("Точка доступа", "Hotspot"),
                tr("включена", "is on"),
                Icons.Rounded.WifiTethering,
                Tints.Green,
                style = NoticeStyle.PILL,
                extraMs = 400,
            ),
        )
    }
}
