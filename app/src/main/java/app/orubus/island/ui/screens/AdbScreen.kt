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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.service.AdbShield
import app.orubus.island.ui.Page
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Ui
import app.orubus.island.ui.glassCard
import kotlinx.coroutines.launch

/**
 * «Экранный щит»: выключаем системные всплывающие плашки — источник двойных поп-апов.
 * Гаситель острова убирает дубли внутри; этот экран убирает вторую, системную половину.
 *
 * Два пути: разрешение WRITE_SECURE_SETTINGS (щит включается в один тап) и свой встроенный
 * adb по классическому протоколу — эмулятор, `adb tcpip 5555`, Android 10 и старше.
 */
@Composable
fun AdbScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var direct by remember { mutableStateOf(AdbShield.canApplyDirectly(context)) }
    var headsUp by remember { mutableStateOf(AdbShield.headsUpOff(context.contentResolver)) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var steps by remember { mutableStateOf(listOf<AdbShield.Step>()) }
    var address by remember { mutableStateOf(AdbShield.ownAddress() ?: "192.168.1.2") }
    var port by remember { mutableStateOf("") }

    Page(tr("Экранный щит", "Popup shield"), onBack,
        subtitle = tr("Выключаем системные всплывающие плашки — после этого двойных поп-апов не бывает ни у одного приложения",
            "Turns off the system heads-up banners — after that no app can show a double pop-up")) {

        // ── Состояние ──
        SectionHeader(tr("Состояние", "Status"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(24.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.HealthAndSafety, null,
                    tint = if (headsUp == true) Ui.OnPrimary else Ui.Text,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (headsUp == true) Ui.Primary else Ui.IconBg)
                        .padding(9.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (headsUp == true) tr("Системные плашки выключены", "System banners are off")
                        else if (headsUp == false) tr("Системные плашки включены", "System banners are on")
                        else tr("Состояние неизвестно", "State unknown"),
                        color = Ui.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (headsUp == true) tr("Щит стоит: всплывает только остров", "Shield is up: only the island pops up")
                        else tr("Второй источник двойных поп-апов", "The second source of double pop-ups"),
                        color = Ui.Sub, fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(if (direct) Ui.Primary else Ui.IconBg)
                    .clickable(enabled = direct && !busy) {
                        busy = true
                        val ok = runCatching { AdbShield.applyDirectly(context) }.getOrDefault(false)
                        headsUp = AdbShield.headsUpOff(context.contentResolver)
                        direct = AdbShield.canApplyDirectly(context)
                        note = if (ok) tr("Щит включён: системные плашки выключены", "Shield is up: system banners are off")
                        else tr("Не получилось — выдайте WRITE_SECURE_SETTINGS через adb", "Couldn't apply — grant WRITE_SECURE_SETTINGS over adb first")
                        busy = false
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (busy) tr("Включаю…", "Working…")
                    else if (direct) tr("Включить щит", "Enable the shield")
                    else tr("Нужно WRITE_SECURE_SETTINGS", "Needs WRITE_SECURE_SETTINGS"),
                    color = if (direct) Ui.OnPrimary else Ui.Text,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            note?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Ui.Sub, fontSize = 13.sp)
            }
        }

        // ── Встроенный adb ──
        SectionHeader(tr("Свой adb внутри острова", "Built-in adb"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(24.dp)).padding(20.dp)) {
            Text(
                tr(
                    "Остров умеет сам подключаться к отладке и выполнять команды щита — как Shizuku или LADB, только без лишних приложений. Классический протокол работает с эмулятором, «adb tcpip 5555» и «Беспроводной отладкой» на Android 10 и старше. Порт спарки Android 11+ требует TLS — тогда используйте путь выше.",
                    "The island connects to debugging and runs the shield commands by itself — like Shizuku or LADB, without extra apps. The classic protocol works with an emulator, \"adb tcpip 5555\" and Wireless debugging on Android 10 or older. The Android 11+ pairing port requires TLS — use the path above instead.",
                ),
                color = Ui.Sub, fontSize = 13.sp, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddressField(
                    value = address,
                    onValue = { address = it },
                    hint = "192.168.1.2",
                    modifier = Modifier.weight(1f),
                )
                AddressField(
                    value = port,
                    onValue = { port = it.filter { c -> c.isDigit() }.take(5) },
                    hint = tr("порт", "port"),
                    modifier = Modifier.width(110.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(if (busy) Ui.IconBg else Ui.Primary)
                    .clickable(enabled = !busy) {
                        val p = port.toIntOrNull()
                        if (address.isBlank() || p == null || p !in 1..65535) {
                            note = tr("Введите адрес и порт (1–65535)", "Enter an address and a port (1–65535)")
                        } else {
                            busy = true
                            note = null
                            steps = emptyList()
                            scope.launch {
                                try {
                                    steps = AdbShield.runShield(context, address.trim(), p) { msg -> note = msg }
                                    headsUp = AdbShield.headsUpOff(context.contentResolver)
                                    direct = AdbShield.canApplyDirectly(context)
                                    note = if (steps.all { it.ok }) {
                                        tr("Готово: команды выполнены, щит стоит", "Done: commands applied, the shield is up")
                                    } else {
                                        tr("Часть команд не прошла — смотрите вывод ниже", "Some commands failed — see the output below")
                                    }
                                } catch (e: Exception) {
                                    note = e.message ?: tr("Сбой подключения", "Connection failed")
                                }
                                busy = false
                            }
                        }
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bolt, null, tint = if (busy) Ui.Sub else Ui.OnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (busy) tr("Подключаюсь и выполняю…", "Connecting and running…")
                        else tr("Подключиться и включить щит", "Connect and enable the shield"),
                        color = if (busy) Ui.Sub else Ui.OnPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                steps.forEach { step ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            if (step.ok) Icons.Rounded.Check else Icons.Rounded.Close, null,
                            tint = if (step.ok) app.orubus.island.core.Tints.Green else app.orubus.island.core.Tints.Red,
                            modifier = Modifier.size(17.dp).padding(top = 1.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(step.command, color = Ui.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            if (step.output.isNotBlank()) {
                                Text(step.output, color = Ui.Sub, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // ── Как получить разрешение ──
        SectionHeader(tr("Откуда берётся разрешение", "Where the permission comes from"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(24.dp)).padding(20.dp)) {
            Text(
                tr(
                    "WRITE_SECURE_SETTINGS выдаётся один раз: скриптом tools/orubus-setup.sh из репозитория, любой локальной adb-программой (LADB, Shizuku) или командой с компьютера:\n\nadb shell pm grant app.orubus.island android.permission.WRITE_SECURE_SETTINGS\n\nПосле этого щит включается кнопкой выше — навсегда, без отладки.",
                    "WRITE_SECURE_SETTINGS is granted once: with the tools/orubus-setup.sh script from the repo, any local adb app (LADB, Shizuku) or one command from a computer:\n\nadb shell pm grant app.orubus.island android.permission.WRITE_SECURE_SETTINGS\n\nAfter that the shield turns on with the button above — forever, no debugging needed.",
                ),
                color = Ui.Sub, fontSize = 13.sp, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Ui.IconBg)
                    .clickable {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText(
                                "Orubus Island",
                                "adb shell pm grant app.orubus.island android.permission.WRITE_SECURE_SETTINGS",
                            ),
                        )
                        note = tr("Команда скопирована", "Command copied")
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(tr("Копировать команду", "Copy the command"), color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Поле адреса/порта в стиле стекла. */
@Composable
private fun AddressField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Ui.IconBg)
            .border(1.dp, Ui.GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(color = Ui.Text, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = Ui.Sub.copy(alpha = 0.6f), fontSize = 14.sp)
                inner()
            },
        )
    }
}
