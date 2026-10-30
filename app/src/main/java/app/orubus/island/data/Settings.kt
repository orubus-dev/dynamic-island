package app.orubus.island.data

import android.content.Context
import android.content.SharedPreferences
import app.orubus.island.core.tr
import app.orubus.island.ui.FontChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Сколько раз в секунду рисуются живые украшения: эквалайзер, светящаяся точка. 0 — с частотой экрана */
@Serializable
enum class FxRate(val fps: Int, private val ru: String, private val en: String) {
    ECO(30, "30 кадров — экономнее", "30 fps — saves power"),
    SMOOTH(60, "60 кадров — плавно", "60 fps — smooth"),
    MAX(0, "Как у экрана — до 120", "Screen rate — up to 120"),
    ;

    val title: String get() = tr(ru, en)
}

@Serializable
enum class ExpandStyle(private val ru: String, private val en: String) {
    FRUIT("Как Фрукт", "Like a Fruit"),
    SPRING("Пружина", "Spring"),
    SMOOTH("Плавно", "Smooth"),
    INSTANT("Без анимации", "No animation"),
    ;

    val title: String get() = tr(ru, en)
}

@Serializable
enum class Elasticity(private val ru: String, private val en: String, val damping: Float) {
    NONE("Без упругости", "No bounce", 1f),
    LOW("Слабая упругость", "Light bounce", 0.8f),
    MEDIUM("Средняя упругость", "Medium bounce", 0.62f),
    HIGH("Сильная упругость", "Strong bounce", 0.48f),
    ;

    val title: String get() = tr(ru, en)
}

@Serializable
enum class TapAction(private val ru: String, private val en: String) {
    EXPAND("Раскрыть капсулу", "Expand the capsule"),
    OPEN_APP("Открыть приложение", "Open the app"),
    PLAY_PAUSE("Пауза / воспроизведение", "Play / pause"),
    NEXT_TRACK("Следующий трек", "Next track"),
    SCREENSHOT("Скриншот без острова", "Screenshot without the island"),
    NOTHING("Ничего", "Nothing"),
    ;

    val title: String get() = tr(ru, en)
}

/** Кнопки раскрытого пустого острова. Подпись на кнопке короче, чем в настройках */
@Serializable
enum class QuickAction(private val ru: String, private val en: String, private val shortRu: String, private val shortEn: String) {
    TIMER("Таймер", "Timer", "Таймер", "Timer"),
    STOPWATCH("Секундомер", "Stopwatch", "Секундомер", "Stopwatch"),
    FLASHLIGHT("Фонарик", "Flashlight", "Фонарик", "Flashlight"),
    SCREENSHOT("Скриншот без острова", "Screenshot without the island", "Скриншот", "Screenshot"),
    CAMERA("Камера", "Camera", "Камера", "Camera"),
    LOCK("Заблокировать экран", "Lock the screen", "Блокировка", "Lock"),
    ;

    val title: String get() = tr(ru, en)
    val short: String get() = tr(shortRu, shortEn)
}

/** Откуда берётся цвет кнопок «Ответить», «Пометить прочитанным» и отправки в карточке уведомления. */
@Serializable
enum class AccentMode(private val ru: String, private val en: String, private val hintRu: String, private val hintEn: String) {
    DEFAULT("Сиреневый", "Lavender", "Цвет приложения", "The app's color"),
    SYSTEM("Системный", "System", "Цвет из обоев Android 12+ (Material You)", "From your Android 12+ wallpaper (Material You)"),
    ADAPTIVE("Из аватара", "From the avatar", "Подбирается по аватару или значку приложения", "Matched to the avatar or app icon"),
    CUSTOM("Свой", "Custom", "Любой цвет из набора", "Any color from the palette"),
    ;

    val title: String get() = tr(ru, en)
    val hint: String get() = tr(hintRu, hintEn)
}

/** Язык приложения и острова. «Как в системе» — русский, если телефон на русском, иначе английский. */
@Serializable
enum class AppLanguage {
    SYSTEM, RU, EN;

    val title: String
        get() = when (this) {
            SYSTEM -> tr("Как в системе", "System default")
            RU -> "Русский"
            EN -> "English"
        }
}

/** Поведение острова в альбомной ориентации. */
@Serializable
enum class LandscapeMode(private val ru: String, private val en: String) {
    FIXED("Фиксированный вырез", "Fix Notch"),
    FLEXIBLE("Гибкий", "Flexible Notch"),
    ;

    val title: String get() = tr(ru, en)
}

/** Стиль анимации разблокировки «Face Unlock»: раскрытая карточка или малый вырез. */
@Serializable
enum class FaceStyle(private val ru: String, private val en: String) {
    EXPANDED("Стиль 1 · Раскрытый", "Style 1 · Expanded"),
    SMALL("Стиль 2 · Малый вырез", "Style 2 · Small notch"),
    ;

    val title: String get() = tr(ru, en)
}

@Serializable
data class IslandSettings(
    val enabled: Boolean = true,
    /** Язык приложения и острова */
    val language: AppLanguage = AppLanguage.SYSTEM,

    // Динамические карточки
    val cardMedia: Boolean = true,
    val cardTorch: Boolean = true,
    val cardProgress: Boolean = true,
    val cardTimer: Boolean = true,
    val cardCharging: Boolean = true,
    val cardCall: Boolean = true,
    val cardMessages: Boolean = true,
    /** Кнопки «Ответить» и «Пометить прочитанным» в карточке уведомления */
    val msgActions: Boolean = true,
    /** Навигация, такси и доставка — «живые активности» из постоянных уведомлений */
    val cardLive: Boolean = true,
    /** Будильник: за 12 часов до него — время подъёма и сколько осталось спать */
    val cardAlarm: Boolean = true,
    /** Кнопки острова для таймера «Часов» нажимают кнопки самих «Часов» (без уведомлений — открывая их на миг) */
    val clockAutoTap: Boolean = true,
    /** «Поставь таймер» от помощника и виджетов можно отдать острову */
    val timerFromAssistant: Boolean = true,
    /** Звонит таймер, будильник или входящий вызов — остров включает экран и не даёт ему погаснуть */
    val wakeForAlerts: Boolean = true,
    /** Особые песни остров встречает эффектами в такт музыке */
    val easterEggs: Boolean = true,

    // События
    val evChargeStart: Boolean = true,
    val evChargeStop: Boolean = true,
    val evLowBattery: Boolean = true,
    val evWifiOn: Boolean = true,
    val evWifiOff: Boolean = true,
    val evHeadsetOn: Boolean = true,
    val evHeadsetOff: Boolean = true,
    val evAirplaneOn: Boolean = true,
    val evAirplaneOff: Boolean = true,
    val evRinger: Boolean = true,
    val evBrightness: Boolean = true,
    val evUsbOn: Boolean = true,
    val evUsbOff: Boolean = true,
    val evUnlock: Boolean = false,
    /** На экране блокировки в капсуле замок; после разблокировки он уходит */
    val evLock: Boolean = true,
    val evDnd: Boolean = true,
    /** Экран оплаты телефоном (T-Pay, SberPay, Mir Pay, Google Кошелёк…) — «Оплата · К терминалу» */
    val evPayReady: Boolean = true,
    /** «К терминалу» сразу при открытии банка или кошелька, а не только на экране оплаты телефоном */
    val payHintOnOpen: Boolean = false,
    /** Пришло уведомление об оплате — «Оплачено» с галочкой */
    val evPaySuccess: Boolean = true,
    /** Поставили будильник — плашка со временем подъёма */
    val evAlarmSet: Boolean = true,
    /** Точка доступа (хотспот) включилась или выключилась */
    val evHotspot: Boolean = true,
    /** VPN подключился или отключился */
    val evVpn: Boolean = true,
    /** Подключены проводные наушники */
    val evWired: Boolean = true,
    /** Телефон зарядился до 100% */
    val evCharged: Boolean = true,
    /** Включили или выключили автоповорот */
    val evRotate: Boolean = false,
    /** Режим энергосбережения включился или выключился */
    val evPowerSave: Boolean = true,
    /** Приложение начало использовать микрофон */
    val evMic: Boolean = true,
    /** Приложение начало использовать камеру */
    val evCamera: Boolean = true,
    /** Точки доступа к микрофону и камере у выреза, пока те заняты */
    val privacyDots: Boolean = true,
    /** Точки видны и на экране блокировки */
    val dotsOnLockscreen: Boolean = true,
    /** Отчёт щита: короткая плашка, когда двойная подавлена */
    val evShield: Boolean = false,
    /** Анимация разблокировки в стиле Face ID (бета) */
    val evFaceUnlock: Boolean = false,
    val faceStyle: FaceStyle = FaceStyle.SMALL,

    // Размер и положение, dp. Камера Android меньше, чем у iPhone (там капсула 126×37), поэтому и остров
    // по умолчанию меньше; ширина и высота настраиваются в «Положении и размере»
    val width: Int = 104,
    val height: Int = 31,
    val offsetX: Int = 0,
    val offsetY: Int = 11,
    /**
     * «Как вырез»: остров прилегает к верхнему краю экрана, верх у него прямой — для телефонов с каплей
     * (waterdrop): капсула закрывает вырез и растёт из него вниз. Отступ сверху тогда не нужен.
     */
    val notch: Boolean = false,
    /** Свободное место над камерой посередине капсулы, куда не заходит содержимое */
    val cameraGap: Int = 34,
    /** Насколько капсула с активностью (музыка, таймер, зарядка) толще пустой */
    val activeExtra: Int = 6,
    /** На сколько свёрнутая капсула с активностью (навигатор, фонарик, музыка) шире спокойной; 0 — такая же */
    val activeWidth: Int = 40,
    /** Радиус углов спокойной/активной капсулы, dp («радиус малого острова») */
    val radiusSmall: Int = 16,
    /** Радиус углов раскрытого острова и плашек, dp («радиус большого острова») */
    val radiusBig: Int = 44,
    /** Высота раскрытого острова и плашек в процентах от обычной («высота раскрытого») */
    val expandedHeightPct: Int = 100,
    /** До какой доли ширины экрана (%) растёт раскрытый остров и плашки — «максимальная ширина» */
    val maxWidthPct: Int = 100,
    // Нет в старых настройках → 1, тогда размеры переносятся на новые
    val layoutVersion: Int = 1,

    // Цвета
    val color: Long = 0xFF000000,
    /** Цвет текста плашек-уведомлений (заголовок и подпись) */
    val textColor: Long = 0xFFFFFFFF,
    val outline: Boolean = false,
    val outlineColor: Long = 0xFF3A3A40,
    val outlineWidth: Float = 1f,
    val outlineFromArt: Boolean = true,
    val shadow: Boolean = false,
    /** Рамка («обводка») спокойной капсулы */
    val borderSmall: Boolean = false,
    /** Рамка раскрытого острова и плашек */
    val borderExpanded: Boolean = false,

    // Цвет кнопок в карточке уведомления
    val accentMode: AccentMode = AccentMode.DEFAULT,
    val accentColor: Long = 0xFF0A84FF,
    /** «Автоцвет фона»: фон плашки подстраивается под значок приложения уведомления */
    val autoBg: Boolean = false,

    // Текст и шрифт острова
    val fontFamily: FontChoice = FontChoice.INTER,
    /** Масштаб текста заголовков, % */
    val textScaleTitle: Int = 100,
    /** Масштаб текста подписей, % */
    val textScaleSub: Int = 100,
    /** Масштаб текста кнопок, % */
    val textScaleButtons: Int = 100,

    // Обновления из GitHub Releases
    val autoUpdateCheck: Boolean = true,
    /** Версия, от которой отказались, — про неё больше не напоминаем */
    val skippedVersion: String = "",
    /** Версия, для которой уже показали «Что нового» */
    val lastSeenVersion: String = "",
    val lastUpdateCheck: Long = 0,
    /** Какую версию остров уже видел: после обновления он один раз показывает «Обновлено» */
    val islandVersion: String = "",

    // Liquid Glass: полупрозрачное стекло с бликами вместо чёрной заливки. По умолчанию выключено
    val liquidGlass: Boolean = false,
    val glassOpacity: Int = 68,
    /** При раскрытии фон под стеклом размывается (Android 12+); без аппаратного размытия тихо не работает */
    val glassBlurBehind: Boolean = true,

    // Анимации
    val expandStyle: ExpandStyle = ExpandStyle.FRUIT,
    val durationPercent: Int = 35,
    /**
     * «Жидкое раскрытие» («Оформление»): содержимое растёт вместе с островом и проявляется из размытия.
     * Длительность и упругость те же, что без него. Выключено — прежнее перетекание
     */
    val liquidExpand: Boolean = false,
    /** Слабая упругость: с сильной остров, как писали в комментариях, слишком «прыгает» */
    val elasticity: Elasticity = Elasticity.LOW,
    val reduceEffects: Boolean = false,
    val haptics: Boolean = true,

    // Эффекты — каждый можно выключить отдельно; «Уменьшить эффекты» выключает их все
    /** Содержимое острова перетекает с размытием (Android 12+) */
    val fxBlur: Boolean = true,
    /** Морозный переход: с Liquid Glass смена содержимого идёт через морозную дымку и блик стекла */
    val fxFrost: Boolean = true,
    /** Полоски эквалайзера пляшут под музыку; выключено — стоят ровно, так экономнее */
    val fxEqualizer: Boolean = true,
    /** Частота кадров эквалайзера и свечения: 60 — плавно и вдвое легче, чем 120 у экрана */
    val fxRate: FxRate = FxRate.SMOOTH,
    /** Мягкое свечение цвета обложки вокруг острова, пока играет музыка */
    val fxGlow: Boolean = false,
    /** Волны NFC вокруг острова, пока ждём терминал, и вспышка с искрами, когда оплата прошла */
    val fxPay: Boolean = true,
    /** Капсула вздрагивает при смене режима звука, колокольчик звонящего таймера качается */
    val fxShake: Boolean = true,
    /** В режиме энергосбережения Android остров обходится без живых эффектов. По умолчанию выключено */
    val batterySaver: Boolean = false,

    // Жесты
    val tapAction: TapAction = TapAction.EXPAND,
    val doubleTapAction: TapAction = TapAction.NOTHING,
    val longPressAction: TapAction = TapAction.OPEN_APP,
    val swipeTracks: Boolean = true,
    val swipeExpand: Boolean = true,
    /** Смахнул уведомление с острова — убрать его и в шторке */
    val swipeClearNotif: Boolean = false,
    /** Раздельные жесты: слева — «Долгое нажатие», справа — ползунок громкости */
    val longPressPartition: Boolean = true,
    /** Долгое нажатие на правой половине и скольжение — ползунок громкости над островом */
    val longPressVolume: Boolean = true,

    // Текст песен
    val lyricsEnabled: Boolean = true,

    // Поведение
    val hideIdle: Boolean = false,
    val hideInLandscape: Boolean = true,
    /** Показывать остров в альбомной ориентации (глобальный выключатель альбомного режима) */
    val showInLandscape: Boolean = false,
    /** В альбомном: «Фиксированный вырез» — остров у камеры, «Гибкий» — полный функционал */
    val landscapeMode: LandscapeMode = LandscapeMode.FLEXIBLE,
    val hideWhenPlayerOpen: Boolean = true,
    /** «По расписанию»: остров виден только с [scheduleFrom] до [scheduleTo] часов (можно через полночь) */
    val scheduleOn: Boolean = false,
    val scheduleFrom: Int = 10,
    val scheduleTo: Int = 23,
    val noticeDurationMs: Int = 2500,
    val timerMinutes: Int = 5,
    /** Через сколько секунд без касаний раскрытый остров сворачивается сам; 0 — не сворачивается */
    val autoCollapseSec: Int = 0,
    /** Вторая активность — кружком рядом с капсулой, как на iPhone */
    val bubble: Boolean = true,
    /** «Двойной остров»: вторая активность — второй вырез вместо кружка */
    val dualNotch: Boolean = false,
    /** «Авто-расширение»: активный остров чуть толще, пока показывает музыку/таймер */
    val autoExpandActive: Boolean = false,
    /** Нажатие на пустой остров раскрывает его: часы, заряд и [quickActions] */
    val idleExpand: Boolean = true,
    /** Кнопки раскрытого пустого острова; пусто — только часы и заряд */
    val quickActions: List<QuickAction> = listOf(QuickAction.TIMER, QuickAction.STOPWATCH, QuickAction.FLASHLIGHT),
    /** «Не выгружать остров»: служба с тихим уведомлением, чтобы остров не закрывался вместе с недавними */
    val keepAlive: Boolean = false,
    /** Движок острова: если службы упали — включить их заново (с WRITE_SECURE_SETTINGS работает сам) */
    val autoHeal: Boolean = true,
    /** Скрывать остров на экране блокировки */
    val hideOnLockscreen: Boolean = false,
    /** Прятать остров, если он без дела: включатель и задержка в секундах */
    val autoHideIdle: Boolean = false,
    val hideIdleSec: Int = 1,
    /** Не показывать уведомления и музыку, пока открыт сам остров (это приложение) */
    val hideWhileForeground: Boolean = true,
    /** Игровой режим: не показывать остров во время игры */
    val gameShowIsland: Boolean = false,
    /** Игровой режим: пропускать ли уведомления во время игры */
    val gameShowNotices: Boolean = false,
    /** Сколько строк текста видно в карточке уведомления */
    val msgLines: Int = 3,
    /** Уведомления приложения, которое сейчас открыто, тоже приходят в остров */
    val msgInApp: Boolean = false,
    /** Мессенджеры, чьи сообщения уже были в острове, и канал их уведомлений — для «Системных плашек» */
    val msgChannels: Map<String, String> = emptyMap(),
    /** Подсказку «Уведомления приходят дважды?» на главной убрали */
    val bannerHintHidden: Boolean = false,

    // Bluetooth: свои значки устройств
    /** Известные наушники: MAC → имя (для «Своих значков Bluetooth») */
    val btKnown: Map<String, String> = emptyMap(),
    /** Свои значки устройств: MAC → имя файла в filesDir/bt_icons */
    val btIcons: Map<String, String> = emptyMap(),
) {
    /** С «Экономией заряда» в энергосбережении Android — как с «Уменьшить эффекты» */
    fun forPowerSave(on: Boolean): IslandSettings = if (on && batterySaver && !reduceEffects) copy(reduceEffects = true) else this

    /** Сейчас часы работы острова: расписание выключено или час [hour] внутри него (22–7 — через полночь) */
    fun inSchedule(hour: Int): Boolean = when {
        !scheduleOn || scheduleFrom == scheduleTo -> true
        scheduleFrom < scheduleTo -> hour in scheduleFrom until scheduleTo
        else -> hour >= scheduleFrom || hour < scheduleTo
    }

    /** Отступ острова сверху: у «Как вырез» остров начинается от самого края экрана */
    val topOffset: Int get() = if (notch) 0 else offsetY

    /** Пляшет ли эквалайзер */
    val liveEqualizer: Boolean get() = fxEqualizer && !reduceEffects
}

object SettingsRepo {
    private const val KEY = "settings_v1"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private var prefs: SharedPreferences? = null

    /** Настройки уже были сохранены до этого запуска: значит, это обновление, а не первая установка. */
    var hadSavedSettings = false
        private set
    private val _state = MutableStateFlow(IslandSettings())
    val state: StateFlow<IslandSettings> = _state.asStateFlow()
    val current: IslandSettings get() = _state.value

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = p
        hadSavedSettings = p.contains(KEY)
        p.getString(KEY, null)?.let { raw ->
            runCatching { json.decodeFromString<IslandSettings>(raw) }.onSuccess { _state.value = it }
        }
        // 1.1 и раньше: размеры по умолчанию были меньше, чем у iPhone — переносим, если их не трогали
        val cur = _state.value
        // 1.4: автоподгонка могла выставить странный вырез камеры — возвращаем по умолчанию
        if (cur.layoutVersion in 2..2) update { copy(cameraGap = IslandSettings().cameraGap, layoutVersion = 3) }
        if (cur.layoutVersion < 2) {
            update {
                copy(
                    width = if (width == 96) 126 else width,
                    height = if (height == 34) 37 else height,
                    offsetY = if (offsetY == 8) 11 else offsetY,
                    layoutVersion = 3,
                )
            }
        }
        // 1.6.2: прежний размер iPhone 126×37 на Android велик — если его не меняли, ставим новый, поменьше
        if (current.layoutVersion < 4) {
            val d = IslandSettings()
            update {
                val old = width == 126 && height == 37
                copy(
                    width = if (old) d.width else width,
                    height = if (old) d.height else height,
                    activeWidth = if (old && activeWidth == 48) d.activeWidth else activeWidth,
                    // Сильная упругость была по умолчанию — переходим на новую, мягче
                    elasticity = if (elasticity == Elasticity.HIGH) d.elasticity else elasticity,
                    layoutVersion = 4,
                )
            }
        }
        // 1.8: «Показывать в альбомной» теперь главный выключатель (раньше был обратный «Скрывать в альбомной»)
        if (current.layoutVersion < 5) {
            update {
                copy(
                    showInLandscape = !hideInLandscape,
                    layoutVersion = 5,
                )
            }
        }
    }

    fun update(block: IslandSettings.() -> IslandSettings) {
        val next = _state.value.block()
        if (next == _state.value) return
        _state.value = next
        prefs?.edit()?.putString(KEY, json.encodeToString(next))?.apply()
    }

    fun reset() = update { IslandSettings() }

    fun resetAnimations() = update {
        val d = IslandSettings()
        copy(
            expandStyle = d.expandStyle,
            durationPercent = d.durationPercent,
            liquidExpand = d.liquidExpand,
            elasticity = d.elasticity,
            reduceEffects = d.reduceEffects,
            noticeDurationMs = d.noticeDurationMs,
        )
    }

    fun resetEffects() = update {
        val d = IslandSettings()
        copy(
            fxBlur = d.fxBlur, fxFrost = d.fxFrost, fxEqualizer = d.fxEqualizer, fxRate = d.fxRate, fxGlow = d.fxGlow,
            fxPay = d.fxPay, fxShake = d.fxShake, batterySaver = d.batterySaver,
        )
    }
}
