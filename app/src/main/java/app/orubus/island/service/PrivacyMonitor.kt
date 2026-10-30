package app.orubus.island.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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
 * Хотспот — раз в 2 секунды: скрытый (но стабильный с API 14) системный список
 * getTetheredIfaces и резерв по именам поднятых интерфейсов (ap0/swlan0/softap/rndis/ncm).
 */
object PrivacyMonitor {
    private var mic = false
    private var cam = false
    private var started = false
    private var pollJob: Job? = null
    private var hotspotLast = false
    private var cameraManager: CameraManager? = null
    private var torchCallback: CameraManager.TorchCallback? = null
    /** Фонарик горит (в том числе чужим приложением): вспышка держит камеру — не спутать её с камерой */
    @Volatile
    private var torchOn = false
    private val main = Handler(Looper.getMainLooper())

    fun start(context: Context, scope: CoroutineScope) {
        if (started) return
        started = true
        val app = context.applicationContext

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
        // Фонарик тоже открывает камеру. Следим за ним отдельно: пока горит вспышка, "камеру в использовании"
        // не объявляем — иначе фонарик любого приложения давал бы зелёную точку камеры
        val torch = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchOn = enabled
                if (enabled && cam) {
                    cam = false
                    IslandBus.camActive.value = false
                }
            }
        }
        camera.registerTorchCallback(torch, null)
        cameraManager = camera
        torchCallback = torch
        camera.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                if (isMainCamera(app, cameraId)) {
                    cam = false
                    IslandBus.camActive.value = false
                }
            }

            override fun onCameraUnavailable(cameraId: String) {
                if (!isMainCamera(app, cameraId)) return
                // Фонарик открывает камеру тем же вызовом: смотрим на вспышку чуть позже,
                // когда её состояние уже точно дошло, и только тогда объявляем камеру
                main.postDelayed({
                    if (!started || torchOn) return@postDelayed
                    cam = true
                    IslandBus.camActive.value = true
                    announce()
                }, 250)
            }
        }, null)

        // Хотспот: сначала текущее состояние (без плашки), дальше — события или опрос.
        hotspotLast = tetheredNow(app)
        IslandBus.hotspotOn.value = hotspotLast

        // Хотспот: TetheringManager — системный класс, обычному приложению недоступен, поэтому
        // опрос каждые 2 секунды: системный список getTetheredIfaces (скрытый, но стабильный с API 14)
        // плюс резерв по именам поднятых интерфейсов (ap0/swlan0/softap/rndis/ncm). Плашка приходит
        // с задержкой не больше пары секунд — на практике сразу после включения раздачи
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
        runCatching { torchCallback?.let { cameraManager?.unregisterTorchCallback(it) } }
        torchCallback = null
        cameraManager = null
        main.removeCallbacksAndMessages(null)
        IslandBus.micActive.value = false
        IslandBus.camActive.value = false
        IslandBus.hotspotOn.value = false
    }

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
        // У камеры — своя анимация: зелёная пульсирующая линза (NoticeStyle.CAM).
        // Микрофон показывает прежнюю оранжевую пилюлю с качающимся значком
        when {
            showCam && !showMic -> IslandBus.post(
                Notice(
                    tr("Камера", "Camera"),
                    tr("используется приложением", "in use by an app"),
                    Icons.Rounded.Videocam,
                    Tints.Green,
                    style = NoticeStyle.CAM,
                    extraMs = 800,
                ),
            )
            showCam && showMic -> IslandBus.post(
                Notice(
                    tr("Микрофон и камера", "Mic and camera"),
                    tr("используются приложением", "in use by an app"),
                    Icons.Rounded.Videocam,
                    Tints.Green,
                    style = NoticeStyle.CAM,
                    extraMs = 800,
                ),
            )
            else -> IslandBus.post(
                Notice(
                    tr("Микрофон", "Microphone"),
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
