package app.orubus.island.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.ConnectivityManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Хотспот: раз в 2 секунды — системный список tethered-интерфейсов (ConnectivityManager,
 * скрытый, но стабильный с API 14 метод getTetheredIfaces) и резерв по именам интерфейсов
 * (ap0/swlan0/softap/rndis/ncm) — точка доступа видится быстро и на любом устройстве.
 */
object PrivacyMonitor {
    private var mic = false
    private var cam = false
    private var started = false
    private var pollJob: Job? = null
    private var hotspotLast = false

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
        camera.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                if (isMainCamera(app, cameraId)) {
                    cam = false
                    IslandBus.camActive.value = false
                }
            }

            override fun onCameraUnavailable(cameraId: String) {
                if (isMainCamera(app, cameraId)) {
                    cam = true
                    IslandBus.camActive.value = true
                    announce()
                }
            }
        }, null)

        // Хотспот: сначала текущее состояние (без плашки), дальше — проверка каждые 2 секунды.
        // TetheringManager скрыт от обычных приложений, поэтому используем стабильный с API 14
        // ConnectivityManager.getTetheredIfaces через рефлексию и резерв по именам интерфейсов.
        hotspotLast = tetheredNow(app)
        IslandBus.hotspotOn.value = hotspotLast

        // Резервная и основная проверка: каждые 2 секунды, чтобы плашка приходила сразу
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

    /** Резерв по именам интерфейсов: ap0/swlan0/softap — плюс USB-модем rndis/ncm. */
    private fun interfacesLookHotspot(): Boolean = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.any { n ->
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
        IslandBus.post(
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
