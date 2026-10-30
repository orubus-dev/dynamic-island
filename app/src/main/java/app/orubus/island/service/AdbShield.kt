package app.orubus.island.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.CRC32

/**
 * Экранный щит — свой встроенный adb, как у Shizuku или LADB, только внутри острова.
 *
 * Зачем: система показывает свои всплывающие плашки поверх шторки — они и есть «двойные поп-апы».
 * Гаситель в [IslandBus] убирает дубли внутри острова, а команда
 * `settings put global heads_up_notifications_enabled 0` выключает системные плашки целиком —
 * и тогда ни одно приложение больше не может показать двойное всплывание.
 *
 * Два пути:
 *  1. Выданное однажды разрешение WRITE_SECURE_SETTINGS (скрипт из репозитория или прошлый запуск) —
 *     тогда щит включается прямо из приложения, без отладки и без компьютера.
 *  2. Свой ADB-клиент по классическому протоколу (TCP + RSA): эмулятор, `adb tcpip 5555`,
 *     «Беспроводная отладка» на Android 10 и старше. Порт TLS-спарки Android 11+ классическим
 *     клиентом не подключается — приложение честно сообщает об этом и не делает вид, что получилось.
 */
object AdbShield {
    const val PKG = "app.orubus.island"
    const val LISTENER = "$PKG/app.orubus.island.service.NotifListener"
    private const val HEADS_UP = "heads_up_notifications_enabled"

    /** Команды, которые щит выполняет на подключённом устройстве. */
    val SHIELD_COMMANDS = listOf(
        "settings put global $HEADS_UP 0",
        "cmd notification allow_listener $LISTENER",
        "pm grant $PKG android.permission.WRITE_SECURE_SETTINGS",
        "dumpsys deviceidle whitelist +$PKG",
        "appops set $PKG RUN_IN_BACKGROUND allow",
    )

    class AdbException(message: String) : Exception(message)

    /** Результат одной команды: [ok] и вывод adb. */
    data class Step(val command: String, val ok: Boolean, val output: String)

    // ─────────────────────────── Состояние щита ───────────────────────────

    /** Системные всплывающие плашки выключены? null — не удалось прочитать. */
    fun headsUpOff(cr: android.content.ContentResolver): Boolean? {
        val v = runCatching { Settings.Global.getInt(cr, HEADS_UP, -1) }.getOrNull() ?: return null
        return v == 0
    }

    /** WRITE_SECURE_SETTINGS уже выдан — щит включается прямо из приложения. */
    fun canApplyDirectly(context: Context): Boolean =
        runCatching {
            context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    /**
     * Включить щит без adb: выключить системные плашки и вписать слушатель уведомлений в
     * безопасные настройки. Работает только с WRITE_SECURE_SETTINGS.
     */
    fun applyDirectly(context: Context): Boolean {
        if (!canApplyDirectly(context)) return false
        val cr = context.contentResolver
        val headsUp = runCatching { Settings.Global.putInt(cr, HEADS_UP, 0) }.getOrDefault(false)
        // Слушатель уведомлений: дописываем себя к уже включённым, не трогая чужие
        val listener = runCatching {
            val current = Settings.Secure.getString(cr, "enabled_notification_listeners") ?: ""
            if (LISTENER in current) true
            else Settings.Secure.putString(
                cr,
                "enabled_notification_listeners",
                if (current.isBlank()) LISTENER else "$current:$LISTENER",
            )
        }.getOrDefault(false)
        if (listener) {
            // Просим систему перепривязать слушателя, чтобы он заработал сразу
            runCatching {
                val nm = context.getSystemService(android.app.NotificationManager::class.java)
                android.app.NotificationManager::class.java
                    .getMethod("requestRebind", ComponentName::class.java)
                    .invoke(nm, ComponentName(context, NotifListener::class.java))
            }
        }
        return headsUp || listener
    }

    /** Свой IP в Wi-Fi — чтобы подставить его в поле адреса. */
    fun ownAddress(): String? = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces()?.toList()
            ?.filter { it.isUp && !it.isLoopback }
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    // ─────────────────────────── Ключ RSA ───────────────────────────

    /** Свой ключ adb: как ~/.android/adbkey, только внутри приложения (файлы приложения приватны). */
    private fun keyPair(context: Context): KeyPair {
        val file = File(context.filesDir, "adb_key")
        if (file.exists()) {
            runCatching {
                val factory = KeyFactory.getInstance("RSA")
                val private = factory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(file.readText(), Base64.NO_WRAP)))
                // Экспонента у созданного нами ключа всегда 65537, но у CRT-ключа она есть явно
                val e = (private as? java.security.interfaces.RSAPrivateCrtKey)?.publicExponent ?: BigInteger.valueOf(65537)
                val public = factory.generatePublic(java.security.spec.RSAPublicKeySpec((private as java.security.interfaces.RSAKey).modulus, e))
                return KeyPair(public, private)
            }
        }
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val pair = generator.generateKeyPair()
        runCatching {
            file.writeText(Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP))
        }
        return pair
    }

    /**
     * Публичный ключ в формате adbd (struct RSAPublicKey из android_pubkey.c):
     * little-endian: длина в словах, Montgomery-коэффициент, модуль, R², экспонента.
     */
    private fun adbPubkeyBlob(key: KeyPair): ByteArray {
        val pub = key.public as RSAPublicKey
        val n = pub.modulus
        val bits = n.bitLength()
        val bytes = bits / 8
        val n0inv = n.negate().modInverse(BigInteger.ONE.shiftLeft(32)).toInt()
        val rr = BigInteger.TWO.pow(2 * bits).mod(n)
        val buf = ByteBuffer.allocate(4 + 4 + bytes + bytes + 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(bits / 32)
        buf.putInt(n0inv)
        buf.put(leBytes(n, bytes))
        buf.put(leBytes(rr, bytes))
        buf.putInt(pub.publicExponent.toInt())
        val name = "orubus@" + Build.MODEL?.replace(" ", "_").let { it ?: "island" }
        val b64 = Base64.encodeToString(buf.array(), Base64.NO_WRAP)
        return (b64 + " " + name + "\u0000").toByteArray(Charsets.UTF_8)
    }

    /** BigInteger в [len] байтов little-endian без знака. */
    private fun leBytes(v: BigInteger, len: Int): ByteArray {
        val out = ByteArray(len)
        val big = v.toByteArray() // big-endian, со знаковым байтом
        var i = big.size - 1
        var j = 0
        while (i >= 0 && j < len) {
            out[j] = big[i]
            j++
            i--
        }
        return out
    }

    // ─────────────────────────── Протокол ADB ───────────────────────────

    private const val CNXN = 0x4E584E43
    private const val AUTH = 0x48545541
    private const val OPEN = 0x4E45504F
    private const val OKAY = 0x59414B4F
    private const val WRTE = 0x45545257
    private const val CLSE = 0x45534C43
    private const val VERSION = 0x01000001

    private class Frame(val cmd: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private class Conn(socket: Socket) {
        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        var maxData: Int = 4096

        fun send(cmd: Int, arg0: Int, arg1: Int, data: ByteArray = ByteArray(0)) {
            val crc = CRC32().apply { if (data.isNotEmpty()) update(data) }
            output.writeLe(cmd)
            output.writeLe(arg0)
            output.writeLe(arg1)
            output.writeLe(data.size)
            output.writeLe(crc.value.toInt())
            output.writeLe(cmd.inv())
            if (data.isNotEmpty()) output.write(data)
            output.flush()
        }

        fun read(): Frame {
            val header = ByteArray(24)
            input.readFully(header)
            val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val cmd = buf.int
            val arg0 = buf.int
            val arg1 = buf.int
            val len = buf.int
            buf.position(16) // crc пропускаем, magic проверяем — повреждённый поток дальше не читаем
            val magic = buf.int
            if (magic != cmd.inv()) throw AdbException("broken frame")
            val data = if (len > 0) {
                if (len > 1 shl 22) throw AdbException("frame too big")
                val payload = ByteArray(len)
                input.readFully(payload)
                payload
            } else ByteArray(0)
            return Frame(cmd, arg0, arg1, data)
        }

        private fun DataOutputStream.writeLe(v: Int) {
            write(v and 0xFF)
            write(v shr 8 and 0xFF)
            write(v shr 16 and 0xFF)
            write(v shr 24 and 0xFF)
        }
    }

    private class Session(private val socket: Socket, key: KeyPair) {
        private val conn = Conn(socket)
        private var localId = 1

        init {
            handshake(key)
        }

        /** Рукопожатие: CNXN → AUTH(токен) → подпись → при отказе — публичный ключ (диалог на устройстве). */
        private fun handshake(key: KeyPair) {
            conn.send(CNXN, VERSION, 1024 * 1024, "host::features=shell_v2,cmd,stat_v2,ls_v2,ack".toByteArray())
            var sentKey = false
            var deadline = SystemClock.elapsedRealtime() + HANDSHAKE_MS
            while (true) {
                if (SystemClock.elapsedRealtime() > deadline) {
                    throw AdbException(
                        "Нет ответа adbd: на Android 11+ этот порт требует TLS-спарку (adb pair). " +
                            "Классический протокол подключается к эмулятору, adb tcpip 5555 или Android 10 и старше.",
                    )
                }
                val f = conn.read()
                when (f.cmd) {
                    CNXN -> {
                        conn.maxData = f.arg1.coerceAtLeast(4096)
                        socket.soTimeout = SHELL_TIMEOUT_MS.toInt()
                        return
                    }
                    AUTH -> when (f.arg0) {
                        1 -> {
                            val token = f.data
                            if (!sentKey) {
                                // Сначала пробуем существующий ключ: если устройство его уже знает — диалога не будет
                                val sig = Signature.getInstance("SHA1withRSA")
                                sig.initSign(key.private)
                                sig.update(token)
                                conn.send(AUTH, 2, 0, sig.sign())
                                sentKey = true
                            } else {
                                conn.send(AUTH, 3, 0, adbPubkeyBlob(key))
                                // Устройство показало диалог «Разрешить отладку?» — даём человеку время
                                deadline = SystemClock.elapsedRealtime() + AUTH_DIALOG_MS
                            }
                        }
                        else -> throw AdbException("Неизвестный тип аутентификации: ${f.arg0}")
                    }
                    else -> Unit
                }
            }
        }

        /** shell-команда: OPEN → OKAY → WRTE(ы) → CLSE. Возвращает собранный вывод. */
        fun shell(command: String, timeoutMs: Long = SHELL_TIMEOUT_MS): String {
            val id = localId++
            conn.send(OPEN, id, 0, "shell:$command".toByteArray(Charsets.UTF_8))
            val out = StringBuilder()
            var remote = -1
            val deadline = SystemClock.elapsedRealtime() + timeoutMs
            while (true) {
                if (SystemClock.elapsedRealtime() > deadline) throw AdbException("Таймаут команды: $command")
                val f = conn.read()
                when (f.cmd) {
                    OKAY -> remote = f.arg0
                    WRTE -> {
                        out.append(String(f.data, Charsets.UTF_8))
                        conn.send(OKAY, id, remote)
                    }
                    CLSE -> return out.toString().trim()
                    else -> Unit
                }
            }
        }

        fun close() {
            runCatching { conn.output.close() }
            runCatching { conn.input.close() }
        }
    }

    /**
     * Подключиться и выполнить команды щита по одной. [onNote] получает промежуточные сообщения
     * («устройство просит разрешить отладку» и т. п.). Возвращает результат каждого шага.
     */
    suspend fun runShield(
        context: Context,
        host: String,
        port: Int,
        onNote: (String) -> Unit = {},
    ): List<Step> = withContext(Dispatchers.IO) {
        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, port), CONNECT_MS)
            socket.soTimeout = HANDSHAKE_MS
            val session = Session(socket, keyPair(context.applicationContext))
            try {
                SHIELD_COMMANDS.map { cmd ->
                    val out = runCatching { session.shell(cmd) }
                    Step(cmd, out.isSuccess, out.getOrElse { e -> e.message ?: "ошибка" })
                }
            } finally {
                session.close()
            }
        } catch (e: AdbException) {
            throw e
        } catch (e: Exception) {
            throw AdbException(
                when (e) {
                    is java.net.SocketTimeoutException -> "Нет ответа adbd: этот порт, скорее всего, ждёт TLS-спарку (Android 11+). Классический ADB работает с эмулятором, adb tcpip 5555 и Android 10 и старше."
                    is java.net.ConnectException -> "Не удалось подключиться к $host:$port — проверьте адрес и порт."
                    else -> e.message ?: "Сбой подключения"
                },
            )
        } finally {
            runCatching { socket.close() }
        }
    }

    private const val CONNECT_MS = 4_000
    private const val HANDSHAKE_MS = 5_000
    private const val AUTH_DIALOG_MS = 60_000
    private const val SHELL_TIMEOUT_MS = 10_000L
}
