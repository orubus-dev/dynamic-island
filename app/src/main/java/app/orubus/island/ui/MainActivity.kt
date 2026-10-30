package app.orubus.island.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orubus.island.R
import app.orubus.island.core.LangScope
import app.orubus.island.core.tr
import app.orubus.island.data.SettingsRepo
import app.orubus.island.ui.screens.AnimScreen
import app.orubus.island.ui.screens.AppInfoScreen
import app.orubus.island.ui.screens.BannersScreen
import app.orubus.island.ui.screens.BanksScreen
import app.orubus.island.ui.screens.BehaviorScreen
import app.orubus.island.ui.screens.BluetoothScreen
import app.orubus.island.ui.screens.CardsScreen
import app.orubus.island.ui.screens.ColorsScreen
import app.orubus.island.ui.screens.DiagScreen
import app.orubus.island.ui.screens.DisplayScreen
import app.orubus.island.ui.screens.EffectsScreen
import app.orubus.island.ui.screens.FeaturesScreen
import app.orubus.island.ui.screens.GesturesScreen
import app.orubus.island.ui.screens.HomeScreen
import app.orubus.island.ui.screens.IdeaScreen
import app.orubus.island.ui.screens.InstructionScreen
import app.orubus.island.ui.screens.KeepAliveScreen
import app.orubus.island.ui.screens.LookScreen
import app.orubus.island.ui.screens.LyricsScreen
import app.orubus.island.ui.screens.NotificationsScreen
import app.orubus.island.ui.screens.PayDiagScreen
import app.orubus.island.ui.screens.PermissionsScreen
import app.orubus.island.ui.screens.PrivacyScreen
import app.orubus.island.ui.screens.SizeScreen
import app.orubus.island.ui.screens.SystemAlertsScreen
import app.orubus.island.ui.screens.TestsScreen
import app.orubus.island.ui.screens.WhatsNewScreen
import app.orubus.island.service.KeepAliveService
import app.orubus.island.update.UpdateChecker

enum class Screen {
    CARDS, EVENTS, GESTURES, LYRICS, SIZE, COLORS, INSTRUCTION, ABOUT, DIAG, LOOK, ANIM, EFFECTS, BANNERS,
    PERMISSIONS, TESTS, NOTIFICATIONS, BEHAVIOR, PAYDIAG, KEEPALIVE, BANKS, DISPLAY, WHATSNEW, PRIVACY, FEATURES, BT,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsRepo.init(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { IslandTheme { AppRoot() } }
    }

    override fun onResume() {
        super.onResume()
        // «Не выгружать остров»: из фона Android может не запустить службу — запускаем, пока приложение открыто
        KeepAliveService.sync(this)
    }
}

@Composable
private fun AppRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Стек экранов сохраняется: поворот экрана и смерть процесса не сбрасывают навигацию
    var stackNames by rememberSaveable { mutableStateOf(listOf<String>()) }
    var forward by rememberSaveable { mutableStateOf(true) }
    val stack = remember(stackNames) { stackNames.mapNotNull { n -> runCatching { Screen.valueOf(n) }.getOrNull() } }
    val open: (Screen) -> Unit = {
        forward = true
        stackNames = stackNames + it.name
    }
    val back: () -> Unit = {
        forward = false
        stackNames = stackNames.dropLast(1)
    }
    BackHandler(enabled = stackNames.isNotEmpty(), onBack = back)

    // Раз в 3 часа спрашиваем GitHub, не вышла ли новая версия; после обновления — «Что нового»
    val context = LocalContext.current
    LaunchedEffect(Unit) { UpdateChecker.check(context.applicationContext, force = false) }
    // Сменили язык — экраны рисуются заново на нём, а открытая вкладка и экран остаются
    LangScope {
        WhatsNewDialog()

        Box(Modifier.fillMaxSize().background(Ui.Bg)) {
            val key = stackNames.lastOrNull() ?: "tab$tab"
            AnimatedContent(
                targetState = key,
                transitionSpec = {
                    if (targetState.startsWith("tab") && initialState.startsWith("tab")) {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                    } else if (forward) {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 8 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 8 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                    }
                },
                label = "nav",
            ) { k ->
                when (k) {
                    "tab0" -> HomeScreen(open)
                    "tab1" -> AppInfoScreen(open)
                    Screen.DISPLAY.name -> DisplayScreen(back, open)
                    Screen.EVENTS.name -> SystemAlertsScreen(back, open)
                    Screen.WHATSNEW.name -> WhatsNewScreen(back)
                    Screen.PRIVACY.name -> PrivacyScreen(back)
                    Screen.FEATURES.name -> FeaturesScreen(open)
                    Screen.CARDS.name -> CardsScreen(back) { open(Screen.INSTRUCTION) }
                    Screen.GESTURES.name -> GesturesScreen(back)
                    Screen.LYRICS.name -> LyricsScreen(back)
                    Screen.SIZE.name -> SizeScreen(back)
                    Screen.COLORS.name -> ColorsScreen(back)
                    Screen.INSTRUCTION.name -> InstructionScreen(back)
                    Screen.ABOUT.name -> IdeaScreen(back)
                    Screen.DIAG.name -> DiagScreen(back)
                    Screen.PAYDIAG.name -> PayDiagScreen(back)
                    Screen.KEEPALIVE.name -> KeepAliveScreen(back)
                    Screen.BANKS.name -> BanksScreen(back)
                    Screen.LOOK.name -> LookScreen(back)
                    Screen.ANIM.name -> AnimScreen(back)
                    Screen.EFFECTS.name -> EffectsScreen(back)
                    Screen.BANNERS.name -> BannersScreen(back)
                    Screen.PERMISSIONS.name -> PermissionsScreen(back, open)
                    Screen.TESTS.name -> TestsScreen(back)
                    Screen.NOTIFICATIONS.name -> NotificationsScreen(back)
                    Screen.BEHAVIOR.name -> BehaviorScreen(back)
                    Screen.BT.name -> BluetoothScreen(back)
                    // Неизвестный ключ (например, экран из старой версии) — пустой слой, не падаем
                    else -> Box(Modifier.fillMaxSize())
                }
            }

            AnimatedVisibility(
                visible = stackNames.isEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RefractionDock(tab) { tab = it }
            }
        }
    }
}

/** Плавающий док перенесён в Dock.kt — жидкое стекло с преломлением. */
