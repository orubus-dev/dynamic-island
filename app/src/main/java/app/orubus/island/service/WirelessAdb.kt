package app.orubus.island.service

import android.util.Base64
import io.github.muntashirakon.crypto.spake2.Spake2Context
import io.github.muntashirakon.crypto.spake2.Spake2Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.conscrypt.Conscrypt
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPair
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * «Беспроводная отладка» Android 11+ — та самая, с шестизначным кодом спарки.
 *
 * Как adb pair, только внутри острова: TLS 1.3 с клиентским сертификатом, экспорт ключевого
 * материала сеанса, SPAKE2 (как в BoringSSL — edwards25519), обмен SPAKE2-сообщениями и
 * зашифрованными PeerInfo, затем классический протокол adb поверх TLS — команды щита
 * (AdbShield.SHIELD_COMMANDS) выполняются на самом телефоне, без компьютера.
 *
 * Протокол 1:1 повторяет AOSP (packages/modules/adb): pairing_connection.cpp — рамки пакета
 * {version u8, type u8, payload u32 BE} и обмен PeerInfo; pairing_auth.cpp — SPAKE2 с именами
 * "adb pair client"/"adb pair server" (с нулевым байтом) и дополнением пароля материалом TLS;
 * aes_128_gcm.cpp — HKDF-SHA256 по info "adb pairing_auth aes-128-gcm key" и AES-128-GCM
 * со счётчиком в nonce; tls_connection.cpp — метка экспорта "adb-label" с нулевым байтом.
 *
 * SPAKE2 — портирован в этот репозиторий (spake2-java, LGPL-3.0, © Muntashir Al-Islam) —
 * пакет io.github.muntashirakon.crypto. TLS — Conscrypt (boringssl): только он умеет отдавать
 * ключевой материал сеанса, который вплетается в пароль — так спарку нельзя украсть.
 */
object WirelessAdb {
    const val PKG = AdbShield.PKG

    /** Метка экспорта ключевого материала TLS — с нулевым байтом, как sizeof у adb. */
    private const val TLS_EXPORT_LABEL = "adb-label\u0000"
    private const val EXPORTED_KEY_SIZE = 64
    private const val PAIRING_HEADER_VERSION = 1
    private const val PACKET_SPAKE2_MSG = 0
    private const val PACKET_PEER_INFO = 1
    private const val PEER_INFO_SIZE = 8192
    private const val PEER_INFO_TYPE_RSA = 0
    private const val HKDF_INFO = "adb pairing_auth aes-128-gcm key"
    private const val CLIENT_NAME = "adb pair client\u0000"
    private const val SERVER_NAME = "adb pair server\u0000"

    class PairingException(message: String) : Exception(message)

    // ─────────────────────────── Сертификат ───────────────────────────

    /**
     * Самоподписанный сертификат того же ключа, что и adb-ключ: adbd после спарки запоминает
     * наш публичный ключ (он уходит в PeerInfo), и этим же ключом подписываются TLS-сеансы.
     */
    private fun certificatePem(context: android.content.Context, key: KeyPair): Pair<String, String> {
        val certFile = File(context.filesDir, "adb_cert.pem")
        val pem = if (certFile.exists()) certFile.readText() else {
            val now = ZonedDateTime.now()
            val holder = X509v3CertificateBuilder(
                X500Name("CN=Orubus Island"),
                BigInteger(64, SecureRandom()),
                Date.from(now.minusDays(1).toInstant()),
                Date.from(now.plus(30, ChronoUnit.DAYS).toInstant()),
                X500Name("CN=Orubus Island"),
                SubjectPublicKeyInfo.getInstance(key.public.encoded),
            ).build(JcaContentSignerBuilder("SHA256withRSA").build(key.private))
            val enc = holder.encoded
            val text = "-----BEGIN CERTIFICATE-----\n" +
                Base64.encodeToString(enc, Base64.NO_WRAP).chunked(64).joinToString("\n") +
                "\n-----END CERTIFICATE-----\n"
            runCatching { certFile.writeText(text) }
            text
        }
        return pem to pemKeyPem(key)
    }

    private fun pemKeyPem(key: KeyPair): String =
        "-----BEGIN PRIVATE KEY-----\n" +
            Base64.encodeToString(key.private.encoded, Base64.NO_WRAP).chunked(64).joinToString("\n") +
            "\n-----END PRIVATE KEY-----\n"

    private fun certFromPem(pem: String): X509Certificate {
        val b64 = pem.replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "").replace("\n", "").trim()
        val der = Base64.decode(b64, Base64.NO_WRAP)
        return runCatching {
            java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(der.inputStream()) as X509Certificate
        }.getOrElse { throw PairingException("Свой сертификат повреждён — переключите отладку и попробуйте снова") }
    }

    /** TLS 1.3 поверх Conscrypt: единственный провайдер на Android, умеющий отдавать материал сеанса. */
    private fun sslContext(context: android.content.Context, key: KeyPair): SSLContext {
        runCatching { Security.insertProviderAt(Conscrypt.newProvider(), 1) }
        val (certPem, keyPem) = certificatePem(context, key)
        val cert = certFromPem(certPem)
        val ks = KeyStore.getInstance("PKCS12").let { store ->
            store.load(null, null)
            store.setKeyEntry("island", key.private, CharArray(0), arrayOf(cert))
            store
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, CharArray(0))
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ctx = SSLContext.getInstance("TLSv1.3")
        ctx.init(kmf.keyManagers, arrayOf<TrustManager>(trustAll), SecureRandom())
        return ctx
    }

    private fun connectTls(context: android.content.Context, host: String, port: Int, timeoutMs: Int): SSLSocket {
        val raw = Socket()
        raw.tcpNoDelay = true
        raw.connect(InetSocketAddress(host, port), timeoutMs)
        raw.soTimeout = timeoutMs
        val key = AdbShield.keyPair(context)
        val factory = sslContext(context, key).socketFactory
        val tls = factory.createSocket(raw, host, port, true) as SSLSocket
        tls.enabledProtocols = arrayOf("TLSv1.3")
        tls.startHandshake()
        return tls
    }

    // ─────────────────────────── AES-128-GCM + HKDF ───────────────────────────

    private fun hkdfAesKey(keyMaterial: ByteArray): ByteArray {
        // HKDF-SHA256: salt пустой, info "adb pairing_auth aes-128-gcm key", 16 байт — как в AOSP
        val prk = Mac.getInstance("HmacSHA256").let { mac ->
            mac.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
            mac.doFinal(keyMaterial)
        }
        return Mac.getInstance("HmacSHA256").let { mac ->
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(HKDF_INFO.toByteArray(Charsets.US_ASCII))
            mac.doFinal().copyOf(16)
        }
    }

    private class Gcm(keyMaterial: ByteArray) {
        private val key = hkdfAesKey(keyMaterial)
        private var encSeq = 0L
        private var decSeq = 0L

        private fun nonce(seq: Long): ByteArray {
            val n = ByteArray(12)
            for (i in 0..7) n[i] = (seq ushr (8 * i)).toByte()
            return n
        }

        fun encrypt(data: ByteArray): ByteArray {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce(encSeq)))
            encSeq++
            return c.doFinal(data)
        }

        fun decrypt(data: ByteArray): ByteArray {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce(decSeq)))
            decSeq++
            return c.doFinal(data)
        }
    }

    // ─────────────────────────── Кадры протокола ───────────────────────────

    private fun readFully(input: DataInputStream, size: Int): ByteArray {
        val buf = ByteArray(size)
        input.readFully(buf)
        return buf
    }

    private fun readHeader(input: DataInputStream): Triple<Int, Int, Int> {
        val h = readFully(input, 6)
        val version = h[0].toInt() and 0xFF
        val type = h[1].toInt() and 0xFF
        val payload = ((h[2].toInt() and 0xFF) shl 24) or ((h[3].toInt() and 0xFF) shl 16) or
            ((h[4].toInt() and 0xFF) shl 8) or (h[5].toInt() and 0xFF)
        if (version != PAIRING_HEADER_VERSION) {
            throw PairingException("Версия протокола спарки не та: $version")
        }
        return Triple(version, type, payload)
    }

    private fun writeHeader(output: DataOutputStream, type: Int, payloadSize: Int) {
        output.write(PAIRING_HEADER_VERSION)
        output.write(type)
        output.write(payloadSize ushr 24)
        output.write(payloadSize ushr 16)
        output.write(payloadSize ushr 8)
        output.write(payloadSize)
        output.flush()
    }

    // ─────────────────────────── Спарка ───────────────────────────

    /**
     * Спариться с «Беспроводной отладкой»: [pairingPort] и [code] с экрана спарки, [mainPort] —
     * порт самой отладки. После спарки adb-ключ острова записан в системе, и команды щита
     * выполняются по [mainPort]. Возвращает результат каждой команды (как классический adb).
     */
    suspend fun pairAndRun(
        context: android.content.Context,
        host: String,
        pairingPort: Int,
        mainPort: Int,
        code: String,
        onNote: (String) -> Unit = {},
    ): List<AdbShield.Step> = withContext(Dispatchers.IO) {
        val ctx = context.applicationContext
        val tls = connectTls(ctx, host, pairingPort, CONNECT_MS)
        try {
            val key = AdbShield.keyPair(ctx)
            val spake = Spake2Context(Spake2Role.Alice,
                CLIENT_NAME.toByteArray(Charsets.US_ASCII),
                SERVER_NAME.toByteArray(Charsets.US_ASCII))
            // Пароль = код спарки + материал сеанса TLS: соединение нельзя перехватить и переиграть
            val exported = Conscrypt.exportKeyingMaterial(tls, TLS_EXPORT_LABEL, null, EXPORTED_KEY_SIZE)
                ?: throw PairingException("TLS не отдал ключевой материал — обновите Conscrypt")
            if (exported.size != EXPORTED_KEY_SIZE) {
                throw PairingException("TLS отдал ${exported.size} байт вместо $EXPORTED_KEY_SIZE")
            }
            val password = code.trim().toByteArray(Charsets.US_ASCII) + exported

            val myMsg = spake.generateMessage(password)
            onNote("Обмен сообщениями SPAKE2…")
            val input = DataInputStream(BufferedInputStream(tls.getInputStream()))
            val output = DataOutputStream(BufferedOutputStream(tls.getOutputStream()))

            writeHeader(output, PACKET_SPAKE2_MSG, myMsg.size)
            output.write(myMsg)
            output.flush()

            val (_, theirType, theirPayload) = readHeader(input)
            if (theirType != PACKET_SPAKE2_MSG) throw PairingException("Ожидалось SPAKE2-сообщение, пришёл тип $theirType")
            val theirMsg = readFully(input, theirPayload)
            val keyMaterial = spake.processMessage(theirMsg)
                ?: throw PairingException("Код спарки не подошёл — шесть цифр те? Порт тот?")
            val gcm = Gcm(keyMaterial)

            // PeerInfo: тип ADB_RSA_PUB_KEY + публичный ключ в формате adbd — его adbd запишет в adb_keys
            onNote("Обмен ключами…")
            val blob = AdbShield.adbPubkeyBlob(key)
            val peerInfo = ByteArray(PEER_INFO_SIZE)
            peerInfo[0] = PEER_INFO_TYPE_RSA.toByte()
            if (blob.size > PEER_INFO_SIZE - 1) throw PairingException("Публичный ключ не влез в PeerInfo")
            blob.copyInto(peerInfo, 1)
            val encrypted = gcm.encrypt(peerInfo)
            writeHeader(output, PACKET_PEER_INFO, encrypted.size)
            output.write(encrypted)
            output.flush()

            val (_, infoType, infoPayload) = readHeader(input)
            if (infoType != PACKET_PEER_INFO) throw PairingException("Ожидался PeerInfo, пришёл тип $infoType")
            val decrypted = gcm.decrypt(readFully(input, infoPayload))
            if (decrypted.size != PEER_INFO_SIZE) {
                throw PairingException("PeerInfo не того размера: ${decrypted.size}")
            }
            onNote("Спарка удалась — ключ острова записан в системе")
        } finally {
            runCatching { tls.close() }
        }

        // Теперь классический adb поверх TLS на основном порту: команды щита
        runCommandsTls(ctx, host, mainPort)
    }

    /** Команды щита по «Беспроводной отладке»: TLS 1.3, затем классический протокол adb. */
    suspend fun runCommandsTls(context: android.content.Context, host: String, mainPort: Int): List<AdbShield.Step> =
        withContext(Dispatchers.IO) {
            val tls = connectTls(context.applicationContext, host, mainPort, CONNECT_MS)
            try {
                val session = AdbShield.Session(tls, AdbShield.keyPair(context.applicationContext))
                try {
                    AdbShield.SHIELD_COMMANDS.map { cmd ->
                        val out = runCatching { session.shell(cmd) }
                        AdbShield.Step(cmd, out.isSuccess, out.getOrElse { e -> e.message ?: "ошибка" })
                    }
                } finally {
                    session.close()
                }
            } catch (e: AdbShield.AdbException) {
                throw e
            } catch (e: Exception) {
                throw AdbShield.AdbException(
                    when (e) {
                        is java.net.SocketTimeoutException ->
                            "Нет ответа adbd: проверьте, что «Беспроводная отладка» включена и порт главный (не от спарки)"
                        is java.net.ConnectException ->
                            "Не удалось подключиться к $host:$mainPort — проверьте адрес и порт"
                        else -> e.message ?: "Сбой подключения"
                    },
                )
            } finally {
                runCatching { tls.close() }
            }
        }

    private const val CONNECT_MS = 6_000
}
