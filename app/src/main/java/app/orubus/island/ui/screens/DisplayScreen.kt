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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.data.LandscapeMode
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.ChipRow
import app.orubus.island.ui.FontChoice
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.GroupSlider
import app.orubus.island.ui.GroupToggle
import app.orubus.island.ui.Page
import app.orubus.island.ui.RadioDot
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.SectionLabel
import app.orubus.island.ui.Pos
import app.orubus.island.ui.StepperSlider
import app.orubus.island.ui.Tile
import app.orubus.island.ui.Ui
import app.orubus.island.ui.glassCard
import app.orubus.island.ui.posOf

private val bgColors = listOf(
    0xFF000000, 0xFF111114, 0xFF1C1C22, 0xFF26233A, 0xFF1A2238, 0xFF102A2A,
    0xFF2B1730, 0xFF3A1A1A, 0xFF2E2A1A, 0xFF3D3D45, 0xFFF2F2F7, 0xFF9C9CA6,
)
private val textColors = listOf(
    0xFFFFFFFF, 0xFFF2F2F7, 0xFFC9C9D2, 0xFF9C9CA6, 0xFF6E6E78, 0xFF000000,
)
private val borderColors = listOf(
    0xFF3A3A40, 0xFF8E8E93, 0xFFFFFFFF, 0xFF0A84FF, 0xFF34C759, 0xFFFF9F0A, 0xFFFF453A,
)

/** Цветные точки выбора цвета, как в референсе: колесо + стрелка. */
@Composable
private fun ColorRow(modifier: Modifier, title: String, current: Long, colors: List<Long>, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .glassCard(RoundedCornerShape(22.dp))
            .clickable { open = true }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(title, color = Ui.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(current))
                    .border(3.dp, Ui.Divider, CircleShape),
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ui.Sub)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = Ui.Card,
            titleContentColor = Ui.Text,
            title = { Text(title) },
            text = { Swatches(colors, current, onPick) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(tr("Готово", "Done"), color = Ui.Primary) } },
        )
    }
}

/** Мини-превью большого острова с уведомлением — как «Big Notch Preview» в референсе. */
@Composable
private fun NotchPreviewCard(s: app.orubus.island.data.IslandSettings) {
    val tf = s.fontFamily.family()
    val tScale = s.textScaleTitle / 100f
    val sScale = s.textScaleSub / 100f
    val bScale = s.textScaleButtons / 100f
    val bg = Color(s.color)
    val fg = Color(s.textColor)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .border(1.dp, Ui.GlassBorder, RoundedCornerShape(30.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Ui.Primary))
            Spacer(Modifier.width(8.dp))
            Text("ORUBUS", color = fg, fontSize = 12.sp * bScale, fontWeight = FontWeight.Bold, fontFamily = tf)
            Text("  ·  ${tr("2 мин назад", "2 min ago")}", color = fg.copy(alpha = 0.55f), fontSize = 11.sp * bScale, fontFamily = tf)
            Spacer(Modifier.weight(1f))
            Text(
                tr("Очистить всё", "CLEAR ALL"),
                color = fg.copy(alpha = 0.8f), fontSize = 10.sp * bScale, fontWeight = FontWeight.SemiBold, fontFamily = tf,
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Ui.Primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ChatBubble, null, tint = fg, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tr("Заголовок уведомления", "Notification Title"),
                    color = fg, fontSize = 17.sp * tScale, fontWeight = FontWeight.SemiBold, fontFamily = tf, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tr("Плашки, музыка и события — в одном стекле", "Notices, music and events — in one glass"),
                    color = fg.copy(alpha = 0.65f), fontSize = 14.sp * sScale, lineHeight = 18.sp * sScale, fontFamily = tf,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(tr("Ответить", "REPLY"), tr("Прочитано", "MARK AS READ"), tr("Тихо", "MUTE")).forEach { b ->
                Text(
                    b, color = fg, fontSize = 11.sp * bScale, fontWeight = FontWeight.SemiBold, fontFamily = tf,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/** Экран «Display»: стиль, размер, положение, радиусы, текст, рамка, альбомный, игры и другое. */
@Composable
fun DisplayScreen(onBack: () -> Unit, open: (app.orubus.island.ui.Screen) -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    val d = app.orubus.island.data.IslandSettings()
    var borderDialog by remember { mutableStateOf(false) }

    Page(tr("Display", "Display"), onBack, action = {
        Text(
            tr("Сброс", "Reset"),
            color = Ui.Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    SettingsRepo.update {
                        copy(
                            width = d.width, height = d.height, offsetX = d.offsetX, offsetY = d.offsetY,
                            radiusSmall = d.radiusSmall, radiusBig = d.radiusBig, maxWidthPct = d.maxWidthPct,
                            color = d.color, textColor = d.textColor, autoBg = d.autoBg,
                            textScaleTitle = d.textScaleTitle, textScaleSub = d.textScaleSub, textScaleButtons = d.textScaleButtons,
                            fontFamily = d.fontFamily, outline = d.outline, outlineColor = d.outlineColor,
                            showInLandscape = d.showInLandscape, landscapeMode = d.landscapeMode,
                            autoExpandActive = d.autoExpandActive, dualNotch = d.dualNotch,
                            hideOnLockscreen = d.hideOnLockscreen, autoHideIdle = d.autoHideIdle, hideIdleSec = d.hideIdleSec,
                            hideWhileForeground = d.hideWhileForeground,
                            gameShowIsland = d.gameShowIsland, gameShowNotices = d.gameShowNotices,
                        )
                    }
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }) {
        // ── STYLE ──
        SectionLabel(tr("Стиль", "Style"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StyleCard(
                Modifier.weight(1f), tr("Капсула", "Capsule"),
                previewNotch = false, selected = !s.notch,
            ) { SettingsRepo.update { copy(notch = false) } }
            StyleCard(
                Modifier.weight(1f), tr("Как вырез", "Notch"),
                previewNotch = true, selected = s.notch,
            ) { SettingsRepo.update { copy(notch = true) } }
        }

        // ── SIZE ──
        SectionLabel(tr("Размер", "Size"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 14.dp)) {
            StepperSlider(tr("Ширина", "Width"), s.width, 70..170, "dp") { v -> SettingsRepo.update { copy(width = v) } }
            Spacer(Modifier.height(6.dp))
            StepperSlider(tr("Высота", "Height"), s.height, 22..46, "dp") { v -> SettingsRepo.update { copy(height = v) } }
        }

        // ── POSITION ──
        SectionLabel(tr("Положение", "Position"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 14.dp)) {
            StepperSlider(tr("По горизонтали", "Horizontal"), s.offsetX, -200..200, "dp") { v -> SettingsRepo.update { copy(offsetX = v) } }
            Spacer(Modifier.height(6.dp))
            StepperSlider(tr("По вертикали", "Vertical"), s.offsetY, 0..120, "dp") { v -> SettingsRepo.update { copy(offsetY = v) } }
            Text(
                tr("Живая подгонка — в «Калибровке»", "Live calibration lives in \"Calibration\""),
                color = Ui.Sub, fontSize = 12.sp,
                modifier = Modifier.padding(start = 96.dp + 44.dp, top = 2.dp),
            )
        }

        // ── RADIUS BY NOTCH TYPE ──
        SectionLabel(tr("Радиус по типу острова", "Radius by notch type"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 14.dp)) {
            StepperSlider(tr("Малый", "Small"), s.radiusSmall, 4..40, "dp") { v -> SettingsRepo.update { copy(radiusSmall = v) } }
            Spacer(Modifier.height(6.dp))
            StepperSlider(tr("Большой", "Big"), s.radiusBig, 12..56, "dp") { v -> SettingsRepo.update { copy(radiusBig = v) } }
            Text(
                tr("Круглота углов малого и большого острова", "Roundness of the small and big notch corners"),
                color = Ui.Sub.copy(alpha = 0.7f), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        // ── BIG/EXPANDED NOTCH SIZE ──
        SectionLabel(tr("Размер большого острова", "Big/expanded notch size"))
        Group {
            GroupSlider(
                posOf(0, 1), Icons.Rounded.OpenInFull, tr("Максимальная ширина", "Big Notch Maximum Width"),
                s.maxWidthPct.toFloat(), 60f..100f, "${s.maxWidthPct}%", steps = 7, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(maxWidthPct = v.toInt()) } }
            GroupSlider(
                Pos.Last, Icons.Rounded.Height, tr("Высота раскрытого", "Expanded Notch Height"),
                s.bigHeight.toFloat(), 80f..180f, "${s.bigHeight}%", steps = 19, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(bigHeight = v.toInt()) } }
            GroupToggle(
                Pos.Last, Icons.Rounded.Bolt, tr("Линия зарядки", "Charging progress line"),
                tr("Тонкая линия прогресса вдоль нижней кромки плашки зарядки", "A thin progress line along the bottom edge of the charging notice"),
                s.chargeProgress, color = Tile.Green,
            ) { v -> SettingsRepo.update { copy(chargeProgress = v) } }
            GroupToggle(
                Pos.Last, Icons.Rounded.MusicNote, tr("Строка песни на острове", "Lyric line on the island"),
                tr("Свёрнутый остров показывает строку песни, которая звучит сейчас", "The compact island shows the line that's playing right now"),
                s.compactLyrics, color = Tile.Purple,
            ) { v -> SettingsRepo.update { copy(compactLyrics = v) } }
        }
        Text(
            tr("Насколько широко и высоко разворачиваются уведомления и события", "How wide and tall the notch bar expands notifications & alerts"),
            color = Ui.Sub.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp),
        )

        // ── BIG NOTCH PREVIEW ──
        SectionLabel(tr("Превью большого острова", "Big notch preview"))
        NotchPreviewCard(s)

        // ── TEXT & COLOR ──
        SectionLabel(tr("Текст и цвет", "Text & color"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorRow(Modifier.weight(1f), tr("Цвет фона", "Bg Color"), s.color, bgColors) { c -> SettingsRepo.update { copy(color = c) } }
            ColorRow(Modifier.weight(1f), tr("Цвет текста", "Text Color"), s.textColor, textColors) { c -> SettingsRepo.update { copy(textColor = c) } }
        }
        Group {
            GroupToggle(
                posOf(0, 1), Icons.Rounded.ColorLens, tr("Автоцвет фона", "Auto Background Color"),
                tr("Фон плашки подстраивается под значок приложения", "Automatically match the background color with the app icon"),
                s.autoBg, color = Tile.Gray, badge = tr("Новое", "New"),
            ) { v -> SettingsRepo.update { copy(autoBg = v) } }
        }

        // ── THEME PRESETS ──
        SectionLabel(tr("Готовые темы", "Theme presets"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            presets.forEachIndexed { i, p ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(p.bg))
                        .border(1.dp, Ui.GlassBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            SettingsRepo.update {
                                copy(
                                    color = p.bg, textColor = p.fg, liquidGlass = p.glass,
                                    glassOpacity = p.opacity, outline = p.rim, outlineColor = p.rimColor,
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(p.title, color = Color(p.fg), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        Text(
            tr("Один тап — и цвет, текст и стекло настроены. Потом можно докрутить руками",
                "One tap sets color, text and glass. Fine-tune everything after"),
            color = Ui.Sub.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp),
        )

        // ── FONT SIZE ──
        SectionLabel(tr("Размер шрифта", "Font size"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 14.dp)) {
            StepperSlider(tr("Заголовки", "Title"), s.textScaleTitle, 70..150, "%") { v -> SettingsRepo.update { copy(textScaleTitle = v) } }
            Spacer(Modifier.height(6.dp))
            StepperSlider(tr("Подписи", "Subtext"), s.textScaleSub, 70..150, "%") { v -> SettingsRepo.update { copy(textScaleSub = v) } }
            Spacer(Modifier.height(6.dp))
            StepperSlider(tr("Кнопки", "Buttons"), s.textScaleButtons, 70..150, "%") { v -> SettingsRepo.update { copy(textScaleButtons = v) } }
        }

        // ── TEXT STYLE ──
        SectionLabel(tr("Стиль текста", "Text style"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(22.dp)).padding(16.dp)) {
            ChipRow(
                FontChoice.entries.map { it.title },
                s.fontFamily.title,
            ) { picked ->
                FontChoice.entries.firstOrNull { it.title == picked }?.let { f ->
                    SettingsRepo.update { copy(fontFamily = f) }
                }
            }
        }

        // ── LANDSCAPE MODE ──
        SectionLabel(tr("Альбомный режим", "Landscape mode"))
        Group {
            GroupToggle(
                posOf(0, 1), Icons.Rounded.StayCurrentLandscape, tr("Показывать в альбомной", "Show in landscape"),
                null, s.showInLandscape, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(showInLandscape = v) } }
        }
        if (s.showInLandscape) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LandscapeCard(Modifier.weight(1f), LandscapeMode.FIXED, s.landscapeMode) {
                    SettingsRepo.update { copy(landscapeMode = LandscapeMode.FIXED) }
                }
                LandscapeCard(Modifier.weight(1f), LandscapeMode.FLEXIBLE, s.landscapeMode) {
                    SettingsRepo.update { copy(landscapeMode = LandscapeMode.FLEXIBLE) }
                }
            }
            Text(
                if (s.landscapeMode == LandscapeMode.FIXED) {
                    tr("Фиксированный вырез: остров у камеры, лишнее скрыто — чище вид.", "Fix Notch: the island stays near the camera and hides non-essential features for a cleaner view.")
                } else {
                    tr("Гибкий: все функции в альбомном виде, остров подстраивается под ориентацию.", "Flexible Notch: all features in landscape, the island adjusts to the orientation.")
                },
                color = Ui.Sub, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }

        // ── AUTO EXPAND ──
        SectionLabel(tr("Авто-расширение", "Auto expand notch"))
        Column(Modifier.fillMaxWidth().glassCard(RoundedCornerShape(22.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DoubleArrow, null, tint = Ui.Text, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Text(tr("Расширять активный остров", "Auto Expand Active Notch"), color = Ui.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                app.orubus.island.ui.IslandSwitch(s.autoExpandActive, { v -> SettingsRepo.update { copy(autoExpandActive = v) } })
            }
            Text(
                tr("Активный остров чуть толще, пока играет музыка или идёт событие", "The notch slightly expands while it shows music or an activity"),
                color = Ui.Sub, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }

        // ── NOTCH BORDER ──
        SectionLabel(tr("Рамка острова", "Notch border"))
        Group {
            GroupToggle(
                posOf(0, 3), Icons.Rounded.BorderColor, tr("Малая рамка", "Small Notch Border"),
                tr("Обводка спокойной капсулы", "Outline of the compact capsule"),
                s.borderSmall, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(borderSmall = v, outline = v || borderExpanded) } }
            GroupToggle(
                posOf(1, 3), Icons.Rounded.BorderColor, tr("Рамка раскрытого", "Expanded Notch Border"),
                tr("Обводка раскрытого острова и плашек", "Outline of the expanded island and notices"),
                s.borderExpanded, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(borderExpanded = v, outline = v || borderSmall) } }
            Row(
                Modifier
                    .fillMaxWidth()
                    .glassCard(RoundedCornerShape(6.dp, 6.dp, 28.dp, 28.dp))
                    .clickable { borderDialog = true }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.BorderColor, null, tint = Ui.Text, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(22.dp))
                Text(tr("Цвет рамки", "Notch Border Color"), color = Ui.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(22.dp).clip(CircleShape).background(Color(s.outlineColor)).border(2.dp, Ui.Divider, CircleShape))
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ui.Sub)
            }
        }

        // ── GAME MODE ──
        SectionLabel(tr("Игровой режим", "Game mode"))
        Group {
            GroupToggle(
                posOf(0, 2), Icons.Rounded.SportsEsports, tr("Показывать остров в играх", "Show Notch During Games"),
                null, s.gameShowIsland, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(gameShowIsland = v) } }
            GroupToggle(
                posOf(1, 2), Icons.Rounded.Gamepad, tr("Показывать уведомления в играх", "Show Notch Notifications"),
                tr("Плашки-уведомления приходят и во время игры", "Notice pop-ups arrive during games too"),
                s.gameShowNotices, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(gameShowNotices = v) } }
        }

        // ── OTHER ──
        SectionLabel(tr("Другие настройки острова", "Other notch settings"))
        Group {
            GroupToggle(
                posOf(0, 5), Icons.Rounded.ContentCopy, tr("Двойной остров", "Enable Dual Notch"),
                tr("Вторая плашка, если идут и музыка, и уведомление", "Shows a second notch if multiple tasks are running"),
                s.dualNotch, color = Tile.Gray, badge = tr("Новое", "New"),
            ) { v -> SettingsRepo.update { copy(dualNotch = v) } }
            GroupToggle(
                posOf(1, 5), Icons.Rounded.Lock, tr("Прятать на экране блокировки", "Hide Notch on Lock Screen"),
                tr("Остров скрывается, когда телефон заблокирован", "Automatically hide the notch when the device is locked"),
                s.hideOnLockscreen, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(hideOnLockscreen = v) } }
            Row(
                Modifier.fillMaxWidth().glassCard(RoundedCornerShape(6.dp)).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VisibilityOff, null, tint = Ui.Text, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(20.dp))
                        Text(tr("Прятать без дела", "Auto Hide Inactive Notch"), color = Ui.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        app.orubus.island.ui.IslandSwitch(s.autoHideIdle, { v -> SettingsRepo.update { copy(autoHideIdle = v) } })
                    }
                    if (s.autoHideIdle) {
                        Spacer(Modifier.height(10.dp))
                        StepperSlider(
                            tr("Секунд", "Seconds"), s.hideIdleSec, 1..30, "s",
                        ) { v -> SettingsRepo.update { copy(hideIdleSec = v) } }
                        Text(
                            tr("Остров прячется через это время и возвращается с новым уведомлением", "The notch hides after this time and reappears when new notifications arrive"),
                            color = Ui.Sub.copy(alpha = 0.7f), fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
            GroupToggle(
                posOf(3, 5), Icons.Rounded.Layers, tr("Прятать, пока открыт сам остров", "Hide Notch While Foreground"),
                tr("Уведомления и музыка не показываются поверх этого приложения", "The notch hides its content while this app is in the foreground"),
                s.hideWhileForeground, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(hideWhileForeground = v) } }
            GroupToggle(
                posOf(4, 5), Icons.Rounded.Vibration, tr("Вибрация", "Vibration / haptic feedback"),
                null, s.haptics, color = Tile.Gray,
            ) { v -> SettingsRepo.update { copy(haptics = v) } }
        }

        // ── APPEARANCE (остальное оформление) ──
        SectionHeader(tr("Оформление", "Appearance"))
        Group {
            GroupItem(posOf(0, 5), Icons.Rounded.PhoneAndroid, tr("Калибровка", "Calibration"),
                tr("Живая подгонка под камеру телефона", "Live fit under your phone's camera"), color = Tile.Gray) { open(app.orubus.island.ui.Screen.SIZE) }
            GroupItem(posOf(1, 5), Icons.Rounded.RoundedCorner, tr("Размер с активностью", "Size with activity"),
                null, color = Tile.Gray) { open(app.orubus.island.ui.Screen.LOOK) }
            GroupItem(posOf(2, 5), Icons.Rounded.ColorLens, tr("Стекло и тень", "Color & glass"),
                tr("Liquid Glass, прозрачность, тень", "Liquid Glass, opacity, shadow"), color = Tile.Gray) { open(app.orubus.island.ui.Screen.COLORS) }
            GroupItem(posOf(3, 5), Icons.Rounded.ScreenRotation, tr("Анимации", "Animations"),
                tr("Раскрытие, длительность, показ событий", "Expansion, duration, event display"), color = Tile.Gray) { open(app.orubus.island.ui.Screen.ANIM) }
            GroupItem(posOf(4, 5), Icons.Rounded.AutoAwesome, tr("Эффекты", "Effects"),
                tr("Размытие, эквалайзер, свечение, волны оплаты", "Blur, equalizer, glow, payment waves"), color = Tile.Gray) { open(app.orubus.island.ui.Screen.EFFECTS) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tr("Живое превью острова — на главном экране и в калибровке", "The live island preview is on Home and in Calibration"),
            color = Ui.Sub.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IslandPreview()
    }

    if (borderDialog) {
        AlertDialog(
            onDismissRequest = { borderDialog = false },
            containerColor = Ui.Card,
            titleContentColor = Ui.Text,
            title = { Text(tr("Цвет рамки", "Notch Border Color")) },
            text = { Swatches(borderColors, s.outlineColor) { c -> SettingsRepo.update { copy(outlineColor = c) } } },
            confirmButton = { TextButton(onClick = { borderDialog = false }) { Text(tr("Готово", "Done"), color = Ui.Primary) } },
        )
    }
}

/** Карточка выбора стиля острова: две формы с превью и радио-точкой. */
@Composable
private fun StyleCard(modifier: Modifier, label: String, previewNotch: Boolean, selected: Boolean, onPick: () -> Unit) {
    Column(
        modifier
            .glassCard(RoundedCornerShape(22.dp))
            .clickable(onClick = onPick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PreviewShape(notch = previewNotch, active = true)
        Spacer(Modifier.height(4.dp))
        PreviewShape(notch = previewNotch, active = false)
        Spacer(Modifier.height(12.dp))
        RadioDot(selected)
        Spacer(Modifier.height(8.dp))
        Text(label, color = Ui.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Мини-формы острова в карточке стиля: капсула или «вырез» с прямыми углами. */
@Composable
private fun PreviewShape(notch: Boolean, active: Boolean) {
    val h = if (active) 26.dp else 18.dp
    val w = if (active) 120.dp else 96.dp
    val shape = if (notch) {
        RoundedCornerShape(4.dp, 4.dp, 14.dp, 14.dp)
    } else {
        RoundedCornerShape(50)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = w, height = h)
                .clip(shape)
                .background(Color(0xFF131316))
                .border(1.dp, Ui.GlassHi, shape),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.padding(start = 5.dp).size(if (active) 16.dp else 11.dp).clip(CircleShape).background(Color(0xFF7A5AC2)))
            if (active) {
                Row(
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    listOf(5, 9, 13, 9, 5).forEach { hgt ->
                        Box(Modifier.size(2.dp, hgt.dp).clip(CircleShape).background(Color(0xFF31C4F3)))
                    }
                }
            }
        }
    }
}

/** Карточка режима альбомной ориентации: Fix / Flexible с картинкой телефона. */
@Composable
private fun LandscapeCard(modifier: Modifier, mode: LandscapeMode, selected: LandscapeMode, onPick: () -> Unit) {
    val on = mode == selected
    Column(
        modifier
            .glassCard(RoundedCornerShape(20.dp))
            .clickable(onClick = onPick)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Ui.IconBg.copy(alpha = 0.5f))
                .border(1.dp, if (on) Ui.GlassHi else Ui.GlassBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (mode == LandscapeMode.FIXED) {
                // Остров прижат к левому (камерному) краю
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 14.dp)
                        .size(width = 34.dp, height = 12.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Ui.Text.copy(alpha = 0.85f)),
                )
                Box(Modifier.align(Alignment.Center).size(6.dp).clip(CircleShape).background(Ui.Sub))
            } else {
                // Гибкий: остров по центру, полностью функциональный
                Box(
                    Modifier.size(width = 64.dp, height = 14.dp)
                        .clip(RoundedCornerShape(7.dp)).background(Ui.Text.copy(alpha = 0.85f)),
                )
                Box(Modifier.align(Alignment.CenterStart).padding(start = 18.dp).size(5.dp).clip(CircleShape).background(Ui.Sub))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (mode == LandscapeMode.FIXED) tr("Фиксированный вырез", "Fix Notch") else tr("Гибкий", "Flexible Notch"),
            color = Ui.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        RadioDot(on)
    }
}

/** Готовая тема: цвет фона, текста, стекло и рамка — одним тапом. */
private data class IslandPreset(val title: String, val bg: Long, val fg: Long, val glass: Boolean, val opacity: Int, val rim: Boolean, val rimColor: Long)

private val presets = listOf(
    IslandPreset("Noir", 0xFF000000, 0xFFFFFFFF, glass = false, opacity = 68, rim = false, rimColor = 0xFF3A3A40),
    IslandPreset("Graphite", 0xFF1C1C20, 0xFFF2F2F5, glass = true, opacity = 62, rim = true, rimColor = 0xFF45454E),
    IslandPreset("Snow", 0xFFF4F4F6, 0xFF111114, glass = false, opacity = 68, rim = true, rimColor = 0xFFD8D8DE),
    IslandPreset("Frost", 0xFF12121A, 0xFFFFFFFF, glass = true, opacity = 74, rim = true, rimColor = 0xFF54546A),
)
