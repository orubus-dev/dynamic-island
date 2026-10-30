package app.orubus.island.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile
import app.orubus.island.ui.Ui
import app.orubus.island.ui.glassCard
import app.orubus.island.ui.publicShape
import java.io.File

/**
 * «Свои значки устройств»: фото наушников для плашек Bluetooth и плеера.
 * Список устройств — те, что уже подключались и запомнились островом.
 */
@Composable
fun BluetoothScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s by SettingsRepo.state.collectAsState()
    // Какое устройство ждёт фото — по нему открывается выбор картинки
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val address = pending
        pending = null
        if (uri != null && address != null) {
            runCatching {
                val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@rememberLauncherForActivityResult
                // Квадрат 256×256 по центру — плашка и плеер показывают круглый значок
                val side = minOf(src.width, src.height)
                val x = (src.width - side) / 2
                val y = (src.height - side) / 2
                val square = Bitmap.createBitmap(src, x, y, side, side)
                val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
                val dir = File(context.filesDir, "bt_icons").apply { mkdirs() }
                val name = address.replace(":", "_") + ".png"
                dir.resolve(name).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 90, it) }
                SettingsRepo.update { copy(btIcons = btIcons + (address to name)) }
            }
        }
    }

    Page(tr("Свои значки устройств", "Custom device icons"), onBack,
        subtitle = tr(
            "Загрузите фото для своих наушников — оно появится в плашке подключения и в плеере вместо стандартного значка",
            "Upload a photo for your headphones — it replaces the standard icon in the connection notice and the player",
        )) {
        val devices = s.btKnown
        if (devices.isEmpty()) {
            Group {
                GroupItem(
                    Pos.Single, Icons.Rounded.AddAPhoto,
                    tr("Пока нет устройств", "No devices yet"),
                    tr("Подключите наушники по Bluetooth — остров запомнит их, и здесь появится загрузка фото",
                        "Connect headphones over Bluetooth — the island remembers them and this is where you add a photo"),
                    color = Tile.Gray,
                )
            }
        } else {
            SectionHeader(tr("Устройства", "Devices"))
            Group {
                devices.entries.forEachIndexed { i, (address, name) ->
                    val iconFile = s.btIcons[address]?.let { File(context.filesDir, "bt_icons/$it") }
                    DeviceRow(
                        Pos.entries.getOrElse(i) { Pos.Last },
                        name, address,
                        iconFile,
                        onPick = {
                            pending = address
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onReset = iconFile?.let { file ->
                            {
                                file.delete()
                                SettingsRepo.update { copy(btIcons = btIcons - address) }
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            tr("Название устройства берётся из Bluetooth при первом подключении; чтобы обновить его, переподключите наушники.",
                "The device name comes from Bluetooth on the first connection; reconnect the headphones to refresh it."),
            color = Ui.Sub, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun DeviceRow(pos: Pos, name: String, address: String, iconFile: File?, onPick: () -> Unit, onReset: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassCard(pos.publicShape())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Ui.Primary.copy(alpha = 0.14f))
                .border(1.dp, Ui.GlassBorder, CircleShape)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = androidx.compose.runtime.remember(iconFile?.absolutePath, iconFile?.lastModified()) {
                iconFile?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
            }
            if (bmp != null) {
                Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Rounded.AddAPhoto, null, tint = Ui.Sub, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Ui.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(address, color = Ui.Sub, fontSize = 12.sp)
        }
        if (onReset != null) {
            Icon(
                Icons.Rounded.Restore, tr("Сбросить", "Reset"),
                tint = Ui.Sub,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onReset)
                    .padding(8.dp),
            )
        }
    }
}
