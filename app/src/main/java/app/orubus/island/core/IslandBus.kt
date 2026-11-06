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
    /**
     * Свернутые уведомления: значок последнего уведомления со счётчиком у выреза. Новое сообщение
     * попадает сюда сразу, как только показалось плашкой, — остров не пустеет, когда плашка ушла.
     * Живут, пока их не откроют, не смахнут или пока уведомление не исчезнет из шторки.
     */
    val noticeStack = MutableStateFlow<List<Notice>>(emptyList())
    /** Какое уведомление из стопки сейчас показывают свайпом, -1 — самое последнее */
    val stackBrowse = MutableStateFlow(-1)
    /** Куда листнули последний раз: 1 — к более новому, -1 — к более старому. Для анимации смены */
    val stackDir = MutableStateFlow(1)
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

    // Очередь с запасом: шквал сообщений из мессенджеров не должен теряться. DROP_OLDEST остаётся
    // предохранителем на случай потока событий, но 24 плашки — больше, чем успеют показать
    private val queue = Channel<Notice>(capacity = 24, onBufferOverflow = BufferOverflow.DROP_OLDEST)

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
     *  • [DEDUP_MS] — одна и та же плашка (приложение + заголовок + текст + стиль) не показывается
     *    второй раз, пока не прошло 4,5 секунды; мессенджеры любят перезаливать уведомление
     *    (прочитано, реакции, дубли под другим ключом) — без гасителя остров мигал бы дважды;
     *  • [BURST_MS] — мгновенное подавление в те же миллисекунды: повтор того же уведомления
     *    (по его системному ключу) или та же пара «заголовок + стиль» без текста, пришедшие
     *    быстрее 900 мс, гасятся сразу даже с изменившимся текстом. Другое уведомление того же
     *    отправителя (другой ключ, другой текст) проходит — это живое сообщение, а не дубль.
     */
    private val recent = object : LinkedHashMap<String, Long>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 40
    }

    /** Те же плашки крупными мазками — для мгновенного окна [BURST_MS]. Вместе с временем — текст,
     *  чтобы живое сообщение с новым текстом не гасилось: раньше два сообщения одного чата подряд
     *  превращались в одно, потому что ключ у них один и тот же */
    private val coarse = HashMap<String, Pair<Long, String?>>()

    /** Сколько одинаковых плашек погашено с запуска — для отладки «почему не показалось» */
    @Volatile
    var deduped = 0
        private set

    /** «Пружинка из выреза»: время запроса (elapsedRealtime). 0 — запроса нет; гаснет за 1,5 с */
    @Volatile
    var notchPopAt = 0L

    private fun signature(n: Notice): String {
        val text = n.message?.text ?: n.state
        // Приложение-источник обязательно: без него чужие острова гасят друг друга
        return "${n.message?.pkg.orEmpty()}\u0000${n.title}\u0000$text\u0000${n.style}"
    }

    private fun burstKey(n: Notice): String =
        // У сообщений ключ уведомления: повтор того же уведомления гасим, новое — пропускаем
        "${n.message?.pkg.orEmpty()}\u0000${n.title}\u0000${n.style}\u0000${n.message?.key.orEmpty()}"

    fun post(n: Notice) {
        // Тихие часы: остров молчит — плашки не показываются (уведомления остаются в шторке)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (SettingsRepo.current.inQuietHours(hour)) return
        val sig = signature(n)
        val burst = burstKey(n)
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(recent) {
            val last = recent[sig]
            val coarseHit = coarse[burst]
            // У событий текст не проверяем — гасим всё подряд в окне [BURST_MS]. У сообщений другой
            // правило: тот же ключ с НОВЫМ текстом — живое сообщение, оно должно показаться
            val burstAlive = coarseHit != null && now - coarseHit.first < BURST_MS &&
                (n.message == null || coarseHit.second == n.message.text)
            if (last != null && now - last < DEDUP_MS || burstAlive) {
                deduped++
                reportShield()
                return
            }
            recent[sig] = now
            coarse[burst] = now to n.message?.text
            if (coarse.size > 40) coarse.remove(coarse.keys.first())
        }
        queue.trySend(n)
    }

    /**
     * Смахнули плашку вверх: уведомление не исчезает, а остаётся у выреза значком со счётчиком.
     * Оно могло попасть в стопку ещё при появлении плашки (авто-стопка) — тогда заменяем, а не дублируем.
     */
    fun minimizeNotice(n: Notice) {
        if (reply.value?.noticeId == n.id) reply.value = null
        val key = n.message?.key
        noticeStack.value = (noticeStack.value.filterNot { key != null && it.message?.key == key } + n).takeLast(STACK_MAX)
        stackBrowse.value = -1
        stackDir.value = 1
        // Сама плашка уходит со сцены — её место сразу занимает значок со счётчиком
        if (notice.value?.id == n.id) notice.value = null
    }

    /**
     * Уведомление приложения только что показалось плашкой — и сразу живёт в стопке:
     * капсула со значком и счётчиком ждёт под плашкой и остаётся, когда та уйдет.
     * Тот же ключ в стопке заменяется (мессенджеры перезаливают уведомление с новым текстом).
     */
    fun pushToStack(n: Notice) {
        val key = n.message?.key ?: return // авто-стопка — только для уведомлений приложений
        noticeStack.value = (noticeStack.value.filterNot { it.message?.key == key } + n).takeLast(STACK_MAX)
        stackBrowse.value = -1
        stackDir.value = 1
    }

    /** Достать уведомление из стопки: последнее, если не просмотривают, иначе — то, на которое смотрят */
    fun takeFromStack(): Notice? {
        val list = noticeStack.value
        if (list.isEmpty()) return null
        val i = stackBrowse.value.takeIf { it in list.indices } ?: list.lastIndex
        val n = list[i]
        removeFromStack(n)
        return n
    }

    /** Уведомление открыли или убрали из стопки по одному */
    fun removeFromStack(n: Notice) {
        noticeStack.value = noticeStack.value.filterNot { it.id == n.id }
        stackBrowse.value = -1
    }

    /** Листают стопку свайпом: влево — к более новым, вправо — к более старым */
    fun browseStack(next: Boolean) {
        val list = noticeStack.value
        if (list.size < 2) {
            stackBrowse.value = -1
            return
        }
        stackDir.value = if (next) 1 else -1
        val from = stackBrowse.value.takeIf { it in list.indices } ?: list.lastIndex
        val to = ((from + if (next) 1 else -1) + list.size) % list.size
        stackBrowse.value = to
    }

    /** Уведомление убрали из шторки — если оно жило в значке, убрать и оттуда */
    fun dropStackKey(key: String) {
        if (noticeStack.value.none { it.message?.key == key }) return
        noticeStack.value = noticeStack.value.filterNot { it.message?.key == key }
        stackBrowse.value = -1
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

    /** Сколько уведомлений максимум живет в значке: самые свежие, старые вытесняются */
    private const val STACK_MAX = 8
}

/** Команды службе: скриншот без острова, блокировка экрана, выключение острова (для банков). */
enum class ServiceCommand { SCREENSHOT, LOCK, TURN_OFF }
