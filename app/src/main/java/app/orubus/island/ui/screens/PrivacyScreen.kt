package app.orubus.island.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.core.tr
import app.orubus.island.ui.Group
import app.orubus.island.ui.GroupItem
import app.orubus.island.ui.Page
import app.orubus.island.ui.Pos
import app.orubus.island.ui.SectionHeader
import app.orubus.island.ui.Tile
import app.orubus.island.ui.Ui
import app.orubus.island.update.UpdateChecker

/** Абзац текста политики. */
@Composable
private fun Para(text: String) {
    Text(
        text,
        color = Ui.Sub,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

/** Политика конфиденциальности: что остров делает и чего не делает с данными. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Page(tr("Политика конфиденциальности", "Privacy Policy"), onBack) {
        Para(
            tr(
                "Действует с 30 октября 2026 года. Коротко: Orubus Island ничего не собирает. Ни аккаунтов, ни аналитики, ни рекламы, ни серверов.",
                "Effective October 30, 2026. In short: Orubus Island collects nothing. No accounts, no analytics, no ads, no servers.",
            )
        )
        SectionHeader(tr("Что происходит на телефоне", "What happens on your phone"))
        Para(
            tr(
                "Остров читает уведомления, чтобы показать их в плашке, и состояние экрана, зарядки, наушников и точки доступа — чтобы показывать события. Всё это обрабатывается только на устройстве и никуда не отправляется.",
                "The island reads notifications to show them as pop-ups, and the state of the screen, charging, headphones and the hotspot — to show events. All of it is processed on the device only and never sent anywhere.",
            )
        )
        SectionHeader(tr("Единственные обращения в сеть", "The only network requests"))
        Para(
            tr(
                "1. Проверка обновлений: приложение спрашивает GitHub Releases, не вышла ли новая версия. 2. Текст песен: если включены тексты, название трека и исполнитель уходят в LRCLIB или NetEase — только эти два поля, ничего больше. Ответы на уведомления, чтение NFC-оплат и всё остальное — локальны.",
                "1. Update check: the app asks GitHub Releases whether a new version is out. 2. Lyrics: if lyrics are enabled, the track title and artist go to LRCLIB or NetEase — only those two fields, nothing else. Replying to notifications, NFC payment reading and everything else are local.",
            )
        )
        SectionHeader(tr("Разрешения", "Permissions"))
        Para(
            tr(
                "Специальные возможности — рисовать остров и читать экран для навигации и оплаты. Доступ к уведомлениям — плашки и сообщения. Телефон — входящие вызовы. Контакты — имена звонящих вместо номеров. Bluetooth — заряд наушников. Каждое разрешение можно отозвать в настройках Android.",
                "Accessibility — drawing the island and reading the screen for navigation and payments. Notification access — pop-ups and messages. Phone — incoming calls. Contacts — caller names instead of numbers. Bluetooth — headphone battery. Every permission can be revoked in Android settings.",
            )
        )
        SectionHeader(tr("Чего мы не делаем", "What we never do"))
        Para(
            tr(
                "Не продаём данные. Не встраиваем рекламу и трекеры. Не просим вход в аккаунт. Не храним содержимое уведомлений после показа плашки. Пароли, коды из СМС и банковские приложения показываются так же, как пришли, — с вашего телефона на ваш экран.",
                "We don't sell data. No ads or trackers. No account sign-in. We don't store notification content after the pop-up is shown. Passwords, SMS codes and banking apps are shown as they arrived — from your phone to your screen.",
            )
        )
        SectionHeader(tr("Проект", "Project"))
        Group {
            GroupItem(Pos.Single, Icons.Rounded.Code, tr("Исходный код", "Source code"),
                "github.com/${UpdateChecker.REPO}", color = Tile.Gray) {
                UpdateChecker.openGithub(context)
            }
            GroupItem(Pos.Single, Icons.Rounded.Gavel, tr("Лицензии", "Licenses"),
                tr("Свободные шрифты Inter, Nunito, Lora, Bayon (OFL)", "The free fonts Inter, Nunito, Lora, Bayon (OFL)"), color = Tile.Gray) {
                UpdateChecker.openGithub(context)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tr("Вопросы о приватности — напишите issue в репозитории проекта.", "Privacy questions — open an issue in the project repository."),
            color = Ui.Sub, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 6.dp).clickable { UpdateChecker.openGithub(context) },
        )
    }
}
