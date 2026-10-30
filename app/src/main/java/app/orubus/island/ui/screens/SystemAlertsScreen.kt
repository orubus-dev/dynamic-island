package app.orubus.island.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AirplanemodeInactive
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.UsbOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.data.FaceStyle
import app.orubus.island.data.IslandSettings
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.GroupToggle
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.Screen
import app.orubus.island.ui.RadioDot
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile
import app.orubus.island.ui.Ui
import app.orubus.island.ui.glassCard
import app.orubus.island.ui.publicShape

/** «Системные оповещения»: живые события и приватность — по разделам. */
@Composable
fun SystemAlertsScreen(onBack: () -> Unit, open: (Screen) -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    fun set(block: IslandSettings.() -> IslandSettings) = SettingsRepo.update(block)

    Page(tr("Системные оповещения", "System Alerts"), onBack) {
        // ── FEATURE ALERTS ──
        SectionHeader(tr("Оповещения функций", "Feature alerts"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.FlashlightOn, tr("Фонарик", "Flashlight"), null,
                s.cardTorch, color = Tile.Gray) { set { copy(cardTorch = it) } }
            GroupToggle(Pos.Last, Icons.Rounded.Navigation, tr("Навигация на картах", "Maps Navigation"), null,
                s.cardLive, color = Tile.Gray) { set { copy(cardLive = it) } }
        }

        // ── PRIVACY/SECURITY ALERTS ──
        SectionHeader(tr("Приватность и безопасность", "Privacy/security alerts"))
        Group {
            // Face Unlock + выбор стиля
            Column(Modifier.fillMaxWidth().glassCard(Pos.First.publicShape()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Face, null, tint = Ui.Text, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Face Unlock", "Face Unlock"), color = Ui.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(tr("Анимация разблокировки", "The unlock animation"), color = Ui.Sub, fontSize = 14.sp)
                    }
                    app.orubus.island.ui.IslandSwitch(s.evFaceUnlock, { v -> set { copy(evFaceUnlock = v) } })
                }
                if (s.evFaceUnlock) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FaceStyleCard(Modifier.weight(1f), FaceStyle.EXPANDED, s.faceStyle) { set { copy(faceStyle = FaceStyle.EXPANDED) } }
                        FaceStyleCard(Modifier.weight(1f), FaceStyle.SMALL, s.faceStyle) { set { copy(faceStyle = FaceStyle.SMALL) } }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "*${tr("Ранняя бета-версия — мы её улучшаем. На некоторых устройствах может работать не идеально.", "Early beta testing version — enhancements underway. May not work perfectly on some devices.")}",
                        color = Ui.Sub.copy(alpha = 0.7f), fontSize = 12.sp, fontStyle = FontStyle.Italic,
                    )
                }
            }
            GroupToggle(Pos.Middle, Icons.Rounded.Lock, tr("Блокировка телефона", "Phone Lock/Unlock"),
                tr("Замок на экране блокировки и его открытие при разблокировке", "A lock on the lock screen that opens when you unlock"),
                s.evLock && s.evUnlock, color = Tile.Gray) { v -> set { copy(evLock = v, evUnlock = v) } }
            GroupToggle(Pos.Middle, Icons.Rounded.Mic, tr("Точка доступа к микрофону и камере", "Mic/Camera Access Indicator Dot"),
                tr("Точки у выреза, пока приложения пользуются микрофоном или камерой", "Dots by the notch while apps use the microphone or camera"),
                s.privacyDots, color = Tile.Gray) { set { copy(privacyDots = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.Visibility, tr("Точки на экране блокировки", "Show Indicator While in Lock Screen"),
                null, s.dotsOnLockscreen, color = Tile.Gray) { set { copy(dotsOnLockscreen = it) } }
            GroupToggle(Pos.Last, Icons.Rounded.Videocam, tr("Оповещать о записи", "Alert when recording starts"),
                tr("Плашка, когда приложение начинает писать звук или видео", "A notice when an app starts recording audio or video"),
                s.evMic && s.evCamera, color = Tile.Gray) { v -> set { copy(evMic = v, evCamera = v) } }
        }

        // ── HOTSPOT ──
        SectionHeader(tr("Точка доступа", "Hotspot"))
        Group {
            GroupToggle(Pos.Single, Icons.Rounded.WifiTethering, tr("Хотспот в острове", "Hotspot in the notch"),
                tr("Плашка, когда точка доступа включается или выключается", "A notice when the hotspot turns on or off"),
                s.evHotspot, color = Tile.Gray, badge = tr("Новое", "New")) { set { copy(evHotspot = it) } }
        }

        // ── Battery / Connections / Modes / Payments / Other (классика) ──
        SectionHeader(tr("Батарея", "Battery"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Bolt, tr("Зарядка началась", "Charging started"), null,
                s.evChargeStart, color = Tile.Gray) { set { copy(evChargeStart = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.PowerOff, tr("Зарядка остановлена", "Charging stopped"), null,
                s.evChargeStop, color = Tile.Gray) { set { copy(evChargeStop = it) } }
            GroupToggle(Pos.Last, Icons.Rounded.BatteryAlert, tr("Низкий заряд", "Low battery"),
                tr("Когда заряд батареи падает до 5%", "When the battery drops to 5%"),
                s.evLowBattery, color = Tile.Gray) { set { copy(evLowBattery = it) } }
        }

        SectionHeader(tr("Подключения", "Connections"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Wifi, tr("Wi-Fi подключён", "Wi-Fi connected"), null,
                s.evWifiOn, color = Tile.Gray) { set { copy(evWifiOn = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.WifiOff, tr("Wi-Fi отключён", "Wi-Fi disconnected"), null,
                s.evWifiOff, color = Tile.Gray) { set { copy(evWifiOff = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.BluetoothConnected, tr("Наушники подключены", "Headphones connected"),
                tr("По Bluetooth, с зарядом, если он известен", "Over Bluetooth, with the charge if it's known"),
                s.evHeadsetOn, color = Tile.Gray) { set { copy(evHeadsetOn = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.BluetoothDisabled, tr("Наушники отключены", "Headphones disconnected"), null,
                s.evHeadsetOff, color = Tile.Gray) { set { copy(evHeadsetOff = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.Usb, tr("USB-устройство подключено", "USB device connected"), null,
                s.evUsbOn, color = Tile.Gray) { set { copy(evUsbOn = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.UsbOff, tr("USB-устройство отключено", "USB device disconnected"), null,
                s.evUsbOff, color = Tile.Gray) { set { copy(evUsbOff = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.Lock, tr("VPN", "VPN"),
                tr("Когда VPN включается или выключается", "When a VPN turns on or off"),
                s.evVpnOn && s.evVpnOff, color = Tile.Gray) { v -> set { copy(evVpnOn = v, evVpnOff = v) } }
            GroupItem(Pos.Last, Icons.Rounded.Bluetooth, tr("Свои значки устройств", "Custom device icons"),
                tr("Своё фото для наушников в плашке и плеере", "Your own photo for headphones in the notice and player"),
                onClick = { open(Screen.BT) }, color = Tile.Gray, badge = if (s.btIcons.isEmpty()) null else tr("Есть", "Set")) { Arrow() }
        }

        SectionHeader(tr("Режимы", "Modes"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.AirplanemodeActive, tr("Режим полёта включён", "Airplane mode on"), null,
                s.evAirplaneOn, color = Tile.Gray) { set { copy(evAirplaneOn = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.AirplanemodeInactive, tr("Режим полёта выключен", "Airplane mode off"), null,
                s.evAirplaneOff, color = Tile.Gray) { set { copy(evAirplaneOff = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.DoNotDisturbOn, tr("Не беспокоить", "Do Not Disturb"),
                tr("Луна и состояние при переключении", "A moon and the state when it changes"),
                s.evDnd, color = Tile.Gray) { set { copy(evDnd = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.Vibration, tr("Режим громкости", "Ring mode"),
                tr("Звонок, вибрация или без звука", "Ring, vibrate or silent"),
                s.evRinger, color = Tile.Gray) { set { copy(evRinger = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.BrightnessAuto, tr("Режим яркости", "Brightness mode"),
                tr("Когда включают или выключают адаптивную яркость", "When adaptive brightness is turned on or off"),
                s.evBrightness, color = Tile.Gray) { set { copy(evBrightness = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.DarkMode, tr("Тёмная тема", "Dark theme"),
                tr("Когда телефон переключается между тёмной и светлой темой", "When the phone switches between dark and light theme"),
                s.evDarkTheme, color = Tile.Gray) { set { copy(evDarkTheme = it) } }
            GroupToggle(Pos.Last, Icons.Rounded.ScreenRotation, tr("Автоповорот", "Auto-rotate"),
                tr("Когда включают или выключают автоповорот", "When auto-rotate is turned on or off"),
                s.evRotate, color = Tile.Gray) { set { copy(evRotate = it) } }
        }

        SectionHeader(tr("Оплата", "Payments"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Nfc, tr("Приложите к терминалу", "Hold near reader"),
                tr("Когда на экране оплата телефоном — Mir Pay, T-Pay, SberPay, Яндекс Пэй и другие. Держится, пока не заплатите или не выйдете из приложения",
                    "When a phone payment is on screen — Mir Pay, T-Pay, SberPay, Yandex Pay and others. Stays until you pay or leave the app"),
                s.evPayReady, color = Tile.Gray) { set { copy(evPayReady = it) } }
            GroupToggle(Pos.Middle, Icons.Rounded.AccountBalance, tr("Подсказка при открытии банка", "Hint when opening a bank"),
                tr("«К терминалу» сразу, как открыли банк или кошелёк, а не только на экране оплаты",
                    "Hold near reader as soon as you open a bank or wallet app, not only on the payment screen"),
                s.payHintOnOpen, color = Tile.Gray) { set { copy(payHintOnOpen = it) } }
            GroupToggle(Pos.Last, Icons.Rounded.CheckCircle, tr("Оплачено", "Paid"),
                tr("Галочка и сумма — с экрана кошелька, из уведомления или СМС банка",
                    "A check mark and the amount — from the wallet screen, a notification or a bank SMS"),
                s.evPaySuccess, color = Tile.Gray) { set { copy(evPaySuccess = it) } }
        }

        SectionHeader(tr("Другое", "Other"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Alarm, tr("Будильник поставлен", "Alarm set"),
                tr("Время подъёма и сколько осталось спать", "Wake-up time and how long you can sleep"),
                s.evAlarmSet, color = Tile.Gray) { set { copy(evAlarmSet = it) } }
            GroupToggle(Pos.Last, Icons.Rounded.Screenshot, tr("Скриншот сделан", "Screenshot taken"),
                tr("Плашка, когда вы делаете скриншот", "A notice when you take a screenshot"),
                s.evScreenshot, color = Tile.Gray) { set { copy(evScreenshot = it) } }
        }
    }
}

/** Карточка стиля Face Unlock: превью анимации + радио-точка. */
@Composable
private fun FaceStyleCard(modifier: Modifier, style: FaceStyle, selected: FaceStyle, onPick: () -> Unit) {
    val on = style == selected
    Column(
        modifier
            .glassCard(RoundedCornerShape(18.dp))
            .border(1.dp, if (on) Ui.GlassHi else Ui.GlassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onPick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (style == FaceStyle.EXPANDED) {
            // Раскрытый вид: большая квадратная «лицевая» плитка
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF101013)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Face, null, tint = Color(0xFF34C759), modifier = Modifier.size(36.dp))
            }
        } else {
            // Малый вырез: капсула с личиком и галочкой
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF101013)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Face, null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp))
                Box(Modifier.size(14.dp).clip(CircleShape).border(1.5.dp, Color(0xFF34C759), CircleShape))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (style == FaceStyle.EXPANDED) "Style 1" else "Style 2", color = Ui.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (style == FaceStyle.EXPANDED) tr("Раскрытый вид", "Expanded View") else tr("Малый вырез", "Small Notch View"),
            color = Ui.Sub, fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        RadioDot(on)
    }
}
