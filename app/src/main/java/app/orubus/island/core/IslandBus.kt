package app.orubus.island.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import app.orubus.island.data.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Общее состояние острова. Службы пишут сюда, капсула и приложение читают.
 * Всё живёт в одном процессе, поэтому хватает обычного синглтона.
 */
object IslandBus {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val serviceRunning = MutableStateFlow(false)
    val expanded = MutableStateFlow(false)
    /** id активности, которую пользователь выбрал (например, кликом по пузырю) */
    val focus = MutableStateFlow<String?>(null)

    val media = MutableStateFlow<MediaInfo?>(null)
    val lyrics = MutableStateFlow<Lyrics?>(null)
    /** Как идёт поиск текста для трека с ключом first */
    val lyricsStatus = MutableStateFlow<Pair<String, LyricsStatus>?>(null)
    val progress = MutableStateFlow<Map<String, ProgressInfo>>(emptyMap())
    val chrono = MutableStateFlow<Map<String, ChronoInfo>>(emptyMap())
    val calls = MutableStateFlow<Map<String, CallInfo>>(emptyMap())
    /** Входящий звонок смахнули — в острове его нет, пока он не закончится или не станет разговором */
    val callHidden = MutableStateFlow(false)
    /** Что может сделать только сама служба спец. возможностей; без службы команда теряется */
    val commands = MutableSharedFlow<ServiceCommand>(extraBufferCapacity = 4)
    val live = MutableStateFlow<Map<String, LiveInfo>>(emptyMap())
    /** Экран оплаты телефоном на виду — «Оплата · К терминалу», пока не заплатили или не ушли (PayMonitor) */
    val payReady = MutableStateFlow<PayReady?>(null)
    /** Ближайший будильник любого приложения «Часы» */
    val nextAlarm = MutableStateFlow<AlarmInfo?>(null)
    /** Звонящие будильники и таймеры других приложений («Часы»): ключи их уведомлений */
    val ringingAlarms = MutableStateFlow<Set<String>>(emptySet())
    val torch = MutableStateFlow(TorchState())
    val timer = MutableStateFlow<TimerState?>(null)
    val stopwatch = MutableStateFlow<StopwatchState?>(null)
    val notice = MutableStateFlow<Notice?>(null)
    /** Идёт ввод ответа на уведомление: окну острова нужен фокус, чтобы показать клавиатуру */
    val reply = MutableStateFlow<ReplyState?>(null)
    /** Наклон телефона от -1 до 1: по нему сдвигаются блики Liquid Glass */
    val glassTilt = MutableStateFlow(0f)
    /** До какого момента (elapsedRealtime) показывать пример активности — пока двигают ползунок ширины */
    val widthPreviewUntil = MutableStateFlow(0L)
    /** Человек читает или отвечает — уведомление не убирается по таймеру */
    val noticeHold = MutableStateFlow(false)
    val battery = MutableStateFlow(BatteryInfo())
    val foregroundPackage = MutableStateFlow<String?>(null)
    val landscape = MutableStateFlow(false)
    /** Вне часов «По расписанию» остров не показывается */
    val offSchedule = MutableStateFlow(false)
    /** Экран включён. Пока он погашен, датчик наклона и чтение экрана навигатора не работают — бережём батарею */
    val screenOn = MutableStateFlow(true)
    /** Включено энергосбережение Android — с «Экономией заряда» остров без живых эффектов */
    val powerSave = MutableStateFlow(false)
    /** Телефон заблокирован PIN-кодом, графическим ключом или паролем: тексты сообщений и кнопки скрыты */
    val locked = MutableStateFlow(false)
    /** На экране — экран блокировки (с PIN-кодом или без): в капсуле замок, если он включён в «Событиях» */
    val keyguard = MutableStateFlow(false)
    /** Открыт экран «Размер и положение»: остров красный, чтобы подогнать его под камеру */
    val calibrating = MutableStateFlow(false)
    /** Какое-то приложение сейчас пишет звук (PrivacyMonitor) */
    val micActive = MutableStateFlow(false)
    /** Какое-то приложение сейчас держит камеру (PrivacyMonitor) */
    val camActive = MutableStateFlow(false)
    /** Точка доступа (хотспот) включена (PrivacyMonitor) */
    val hotspotOn = MutableStateFlow(false)
    /** Подключённые наушники: имя, заряд, MAC — плеер показывает их в раскрытом острове */
    val headset = MutableStateFlow<HeadsetInfo?>(null)
    /** На переднем плане игра — для игрового режима */
    val gameForeground = MutableStateFlow(false)
    /** Играет та самая песня — остров празднует (пасхалка, Egg.kt) */
    val egg = MutableStateFlow(false)
    /** Последнее касание раскрытого острова — чтобы не сворачивать его из-под пальца */
    @Volatile
    var lastTouch = 0L

    private val queue = Channel<Notice>(capacity = 6, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        scope.launch {
            for (n in queue) {
                notice.value = n
                val duration = SettingsRepo.current.noticeDurationMs + n.extraMs
                // Уведомление уходит по времени или раньше, если его смахнули кликом
                withTimeoutOrNull(duration) { notice.first { it?.id != n.id } }
                // Пока отвечают или листают переписку, карточка остаётся; потом ещё чуть-чуть
                if (notice.value?.id == n.id && noticeHold.value) {
                    while (notice.value?.id == n.id && noticeHold.value) delay(200)
                    if (notice.value?.id == n.id) withTimeoutOrNull(1500) { notice.first { it?.id != n.id } }
                }
                if (notice.value?.id == n.id) notice.value = null
                // Короткая пауза между плашками, чтобы они не слипались, — но без заметного ожидания
                delay(110)
            }
        }
    }

    /**
     * Гаситель двойных плашек, два окна:
     *  • [DEDUP_MS] — одна и та же плашка (заголовок + текст + стиль) не показывается второй раз,
     *    пока не прошло 4,5 секунды; мессенджеры любят перезаливать уведомление (прочитано,
     *    реакции, дубли под другим ключом) — без гасителя остров мигал бы дважды;
     *  • [BURST_MS] — мгновенное подавление в те же миллисекунды: повтор с тем же заголовком
     *    и стилем, пришедший быстрее 900 мс (даже с изменившимся текстом), гасится сразу.
     */
    private val recent = object : LinkedHashMap<String, Long>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 40
    }

    /** Те же плашки крупными мазками (без текста) — для мгновенного окна [BURST_MS] */
    private val coarse = HashMap<String, Long>()

    /** Сколько одинаковых плашек погашено с запуска — для отладки «почему не показалось» */
    @Volatile
    var deduped = 0
        private set

    /** «Пружинка из выреза»: время запроса (elapsedRealtime). 0 — запроса нет; гаснет за 1,5 с */
    @Volatile
    var notchPopAt = 0L

    private fun signature(n: Notice): String {
        val text = n.message?.text ?: n.state
        return "${n.title}\u0000$text\u0000${n.style}"
    }

    fun post(n: Notice) {
        val sig = signature(n)
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(recent) {
            val last = recent[sig]
            val burst = coarse["${n.title}\u0000${n.style}"]
            if (last != null && now - last < DEDUP_MS || burst != null && now - burst < BURST_MS) {
                deduped++
                reportShield()
                return
            }
            recent[sig] = now
            coarse["${n.title}\u0000${n.style}"] = now
            if (coarse.size > 40) coarse.remove(coarse.keys.first())
        }
        queue.trySend(n)
    }

    @Volatile
    private var lastShieldNotice = 0L

    /** «Дубль заблокирован»: короткий отчёт щита, если его включили в «Системных оповещениях» */
    private fun reportShield() {
        if (!SettingsRepo.current.evShield) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastShieldNotice < 6_000) return
        lastShieldNotice = now
        scope.launch {
            kotlinx.coroutines.delay(450)
            post(
                Notice(
                    app.orubus.island.core.tr("Щит", "Shield"),
                    app.orubus.island.core.tr("дубль заблокирован", "duplicate blocked"),
                    Icons.Rounded.Security,
                    Tints.Blue,
                    style = NoticeStyle.PILL,
                ),
            )
        }
    }

    private const val DEDUP_MS = 4_500L

    /** Окно мгновенного подавления: повторы, пришедшие в эти миллисекунды, гасятся всегда */
    private const val BURST_MS = 900L
}

/** Команды службе: скриншот без острова, блокировка экрана, выключение острова (для банков). */
enum class ServiceCommand { SCREENSHOT, LOCK, TURN_OFF }
