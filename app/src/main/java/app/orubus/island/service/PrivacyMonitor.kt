package app.orubus.island.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
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
 * Микрофон — AudioRecordingCallback, камера — CameraManager.AvailabilityCallback,
 * хотспот — раз в 10 секунд читаем список tethered-интерфейсов (в такт сердцебиению движка).
 */
object PrivacyMonitor {
    private var mic = false
    private var cam = false
    private var started = false
    private var pollJob: Job? = null

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

        // Хотспот: раз в 10 секунд смотрим сетевые интерфейсы — точка доступа поднимает ap0/swlan0/softap
        pollJob = scope.launch(Dispatchers.Default) {
            var last = false
            while (isActive) {
                val on = runCatching {
                    java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.any { n ->
                        val name = n.name.lowercase()
                        name.startsWith("ap") || name.contains("swlan") || name.contains("softap")
                    } ?: false
                }.getOrDefault(false)
                if (on != last) {
                    last = on
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
                delay(10_000)
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
}
