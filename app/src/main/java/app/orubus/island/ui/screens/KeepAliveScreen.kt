package app.orubus.island.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.Tints
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.service.IslandEngine
import app.orubus.island.service.KeepAliveService
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.GroupToggle
import app.orubus.island.ui.Page
import app.orubus.island.ui.Perms
import app.orubus.island.ui.Perms.Vendor
import app.orubus.island.ui.Pos
import app.orubus.island.ui.Screen
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile

/**
 * «Чтобы остров не закрывался»: Xiaomi (HyperOS), Oppo, realme, OnePlus (ColorOS), Samsung и другие сами
 * выгружают приложения в фоне — особенно если смахнуть их из недавних. Здесь кнопки прямо в нужные
 * настройки этой прошивки и подсказка, как закрепить остров в недавних.
 */
@Composable
fun KeepAliveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vendor = Perms.vendor()
    val (battery, _) = rememberOnResume { Perms.batteryUnrestricted(context) }
    val s by SettingsRepo.state.collectAsState()

    Page(tr("Чтобы остров не закрывался", "Keep the island running"), onBack,
        subtitle = tr("Прошивка телефона может выгружать остров в фоне — тогда он пропадает, пока его не включить снова",
            "Your phone's firmware may stop the island in the background — then it disappears until you turn it on again")) {
        // Процесс со службой на переднем плане Android не закрывает, когда приложение смахивают из недавних
        GroupToggle(
            Pos.Single, Icons.Rounded.PushPin, tr("Не выгружать остров", "Keep the island loaded"),
            tr("Остров не пропадёт, даже если смахнуть приложение из недавних. Android может показать тихое уведомление",
                "The island stays even if you swipe the app away from recent apps. Android may show a quiet notification"),
            s.keepAlive, color = Tile.Purple,
        ) { v ->
            SettingsRepo.update { copy(keepAlive = v) }
            KeepAliveService.sync(context)
        }

        // Движок острова: жив ли остров, оживляем ли его после сбоев
        val engine by IslandEngine.health.collectAsState()
        SectionHeader(tr("Движок острова", "Island engine"))
        Group {
            GroupItem(
                Pos.First, Icons.Rounded.MonitorHeart, tr("Состояние", "Status"), engineSubtitle(engine),
                color = when (engine.state) {
                    IslandEngine.State.UP -> Tile.Green
                    IslandEngine.State.RECOVERING -> Tile.Orange
                    IslandEngine.State.DOWN -> Tile.Red
                },
            ) { EngineBadge(engine.state) }
            GroupToggle(
                Pos.Last, Icons.Rounded.RestartAlt, tr("Оживлять после сбоя", "Revive after a crash"),
                tr("Если остров или его службы упали — движок включит их заново и подскажет, если нужно ваше вмешательство",
                    "If the island or its services crash, the engine turns them back on and tells you if your help is needed"),
                s.autoHeal, color = Tile.Orange,
            ) { v -> SettingsRepo.update { copy(autoHeal = v) } }
        }

        Group {
            GroupItem(
                if (Perms.hasAutostart()) Pos.First else Pos.Single, Icons.Rounded.BatteryChargingFull,
                tr("Батарея без ограничений", "Unrestricted battery"),
                batteryHint(vendor), onClick = { Perms.openBattery(context) }, color = Tile.Green,
            ) { Status(battery) }
            if (Perms.hasAutostart()) {
                GroupItem(
                    Pos.Last, Icons.Rounded.RocketLaunch, tr("Автозапуск", "Autostart"),
                    tr("Включите для «Динамического острова» — тогда он запустится сам после перезагрузки и закрытия",
                        "Turn it on for Dynamic Island — then it starts by itself after a reboot or being closed"),
                    onClick = { Perms.openAutostart(context) }, color = Tile.Orange,
                ) { Arrow() }
            }
        }

        SectionHeader(tr("Закрепите в недавних", "Lock it in recent apps"))
        InfoText(lockHint(vendor))

        SectionHeader(tr("Если остров всё равно пропал", "If the island still disappears"))
        Group {
            GroupItem(
                Pos.First, Icons.Rounded.Lock, tr("Спец. возможности", "Accessibility"),
                tr("Выключите и снова включите службу «Динамический остров»", "Turn the Dynamic Island service off and on again"),
                onClick = { Perms.openAccessibility(context) }, color = Tile.Blue,
            ) { Arrow() }
            AppDetailsItem(Pos.Last)
        }

        SectionHeader(tr("Оживление без вашего участия", "Revival without your help"))
        InfoText(
            tr(
                "Android не даёт обычному приложению включать службы доступности. Один раз разрешите это через отладку по Wi-Fi (беспроводная отладка в «Для разработчиков») и выполните на компьютере:\n\nadb pair IP:ПОРТ\nadb connect IP:ПОРТ\nadb shell pm grant app.orubus.island android.permission.WRITE_SECURE_SETTINGS\n\nПосле этого остров сможет сам включаться после сбоя и перезагрузки — без «Спец. возможностей» руками.",
                "Android doesn't let a regular app turn accessibility services on. Allow it once over wireless debugging (Developer options) and run on your computer:\n\nadb pair IP:PORT\nadb connect IP:PORT\nadb shell pm grant app.orubus.island android.permission.WRITE_SECURE_SETTINGS\n\nThen the island turns itself back on after crashes and reboots — no manual accessibility toggling.",
            )
        )
    }
}

/** Бейдж состояния движка: работает / оживляет / упал. */
@Composable
private fun EngineBadge(state: IslandEngine.State) {
    val (label, color) = when (state) {
        IslandEngine.State.UP -> tr("Работает", "Running") to Tints.Green
        IslandEngine.State.RECOVERING -> tr("Оживляет", "Reviving") to Tints.Orange
        IslandEngine.State.DOWN -> tr("Упал", "Down") to Tints.Red
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontSize = 13.sp)
    }
}

private fun engineSubtitle(h: IslandEngine.Health): String = when (h.state) {
    IslandEngine.State.UP ->
        if (h.canSelfHeal) tr("Пульс есть, самолечение включено", "Heartbeat running, self-healing on")
        else tr("Пульс есть. Для самолечения выдайте WRITE_SECURE_SETTINGS (ниже)", "Heartbeat running. Grant WRITE_SECURE_SETTINGS for self-healing (below)")
    IslandEngine.State.RECOVERING -> tr("Перезапускаем службы после сбоя", "Restarting services after a crash")
    IslandEngine.State.DOWN -> tr("Остров не запущен — включите его в спец. возможностях", "The island isn't running — turn it on in Accessibility")
}

/** Пункт «Чтобы остров не закрывался» — в «Разрешениях» и «О приложении». */
@Composable
internal fun KeepAliveItem(pos: Pos, open: (Screen) -> Unit) {
    GroupItem(pos, Icons.Rounded.BatteryChargingFull, tr("Чтобы остров не закрывался", "Keep the island running"),
        tr("Батарея, автозапуск и закрепление в недавних", "Battery, autostart and locking in recent apps"),
        onClick = { open(Screen.KEEPALIVE) }, color = Tile.Green) { Arrow() }
}

/** «О приложении» в настройках Android: ограниченные настройки, разрешения, батарея. */
@Composable
internal fun AppDetailsItem(pos: Pos) {
    val context = LocalContext.current
    GroupItem(pos, Icons.Rounded.Security, tr("О приложении в системе", "App info in Android"),
        tr("Ограниченные настройки, разрешения, батарея", "Restricted settings, permissions, battery"),
        onClick = { Perms.openAppDetails(context) }, color = Tile.Gray)
}

private fun batteryHint(v: Vendor): String = when (v) {
    Vendor.XIAOMI -> tr("«Контроль активности» или «Экономия энергии» → «Нет ограничений»",
        "Battery saver or “Activity control” → “No restrictions”")
    Vendor.SAMSUNG -> tr("«Батарея» → «Без ограничений», и уберите остров из «Приложений в спящем режиме»",
        "Battery → Unrestricted, and remove the island from Sleeping apps")
    Vendor.OPPO -> tr("«Использование батареи» → разрешите работу в фоне", "Battery usage → allow background activity")
    else -> tr("Найдите «Динамический остров» и выберите «Не оптимизировать» или «Без ограничений»",
        "Find Dynamic Island and choose “Don't optimize” or “Unrestricted”")
}

private fun lockHint(v: Vendor): String = when (v) {
    Vendor.XIAOMI -> tr(
        "Откройте недавние приложения, зажмите карточку «Динамический остров» и нажмите на замок. " +
            "Закреплённое приложение HyperOS и MIUI не закрывают, даже когда вы чистите недавние.",
        "Open recent apps, long-press the Dynamic Island card and tap the lock. HyperOS and MIUI don't close a " +
            "locked app, even when you clear recent apps.",
    )
    Vendor.OPPO -> tr(
        "Откройте недавние приложения, нажмите «⋮» над карточкой «Динамический остров» (или потяните её вниз) " +
            "и выберите «Заблокировать».",
        "Open recent apps, tap “⋮” above the Dynamic Island card (or pull it down) and choose “Lock”.",
    )
    Vendor.SAMSUNG -> tr(
        "Откройте недавние приложения, нажмите на значок над карточкой «Динамический остров» и выберите «Не закрывать».",
        "Open recent apps, tap the icon above the Dynamic Island card and choose “Keep open”.",
    )
    else -> tr(
        "Откройте недавние приложения и потяните карточку «Динамический остров» вниз или зажмите её — " +
            "появится замок. Закреплённое приложение не закрывается, когда вы чистите недавние.",
        "Open recent apps and pull the Dynamic Island card down or long-press it — a lock appears. A locked app " +
            "isn't closed when you clear recent apps.",
    )
}
