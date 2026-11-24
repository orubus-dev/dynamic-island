package app.orubus.island.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.orubus.island.core.IslandBus
import app.orubus.island.data.SettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL

/**
 * Реклама продуктов Orubus — только свои: Relay, MANGACA, Payworth, Sol, Buks, Fossil, OmniHub,
 * Gene. Никаких сторонних сетей: список лежит в репозитории (store/ads.json), а если владелец
 * вписал проект Supabase в сборку, список тянется оттуда — любая таблица island_ads в уже
 * существующем проекте, новый не создаётся (SQL для неё — supabase/island_ads.sql).
 *
 * Показ — карточка на главной. Без трекеров, без сторонних SDK: одна HTTPS-загрузка раз в шесть
 * часов, кэш в памяти приложения. Выключатель — «Показывать новости Orubus» рядом с карточкой.
 */
object Ads {
    /** Одна запись: что, куда ведёт и насколько часто показывать. */
    data class Ad(
        val id: String,
        val title: String,
        val text: String,
        val url: String,
        val image: String? = null,
        val weight: Int = 1,
    )

    val ads = MutableStateFlow<List<Ad>>(emptyList())

    private const val REPO_FEED = "https://raw.githubusercontent.com/orubus-dev/dynamic-island/main/store/ads.json"
    private const val CACHE_MS = 6 * 60 * 60 * 1000L

    private var lastFetch = 0L

    /** Что показывать: случайная запись с весами; нет сети — нет карточки, ошибка молчит. */
    fun pick(): Ad? = ads.value.let { list ->
        if (list.isEmpty()) return null
        val total = list.sumOf { it.weight }
        var r = (0 until total.coerceAtLeast(1)).random()
        list.forEach { ad ->
            r -= ad.weight
            if (r < 0) return ad
        }
        list.last()
    }

    /** Загрузка: сначала Supabase (если вписан в сборку), потом фид из репозитория. */
    suspend fun refresh(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!SettingsRepo.current.adsEnabled && !force) return@withContext
        val now = System.currentTimeMillis()
        if (!force && ads.value.isNotEmpty() && now - lastFetch < CACHE_MS) return@withContext
        lastFetch = now
        val fetched = fetchSupabase() ?: fetchRepo()
        if (fetched.isNotEmpty()) ads.value = fetched
    }

    /** От Supabase: BuildConfig.SUPABASE_URL + ANON_KEY задаются в secrets сборки владельцем. */
    private fun fetchSupabase(): List<Ad>? {
        val url = ADS_SUPABASE_URL ?: return null
        val key = ADS_SUPABASE_ANON_KEY ?: return null
        return runCatching {
            val conn = (URL("$url/rest/v1/island_ads?select=id,title,text,url,image,weight&active=eq.true&order=weight.desc")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $key")
            }
            conn.connect()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            parse(body)
        }.getOrNull()
    }

    /** Фид из репозитория — работает всегда, это и есть «реклама своих продуктов». */
    private fun fetchRepo(): List<Ad>? = runCatching {
        val conn = (URL(REPO_FEED).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        conn.connect()
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        parse(body)
    }.getOrNull()

    private fun parse(body: String): List<Ad> = runCatching {
        Json.parseToJsonElement(body).jsonArray.mapNotNull { el ->
            val o = el.jsonObject
            val title = o["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val url = o["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            Ad(
                id = o["id"]?.jsonPrimitive?.contentOrNull ?: title,
                title = title,
                text = o["text"]?.jsonPrimitive?.contentOrNull ?: "",
                url = url,
                image = o["image"]?.jsonPrimitive?.contentOrNull,
                weight = o["weight"]?.jsonPrimitive?.longOrNull?.toInt() ?: 1,
            )
        }
    }.getOrDefault(emptyList())

    /** Открыть карточку: браузер сам, остров ни при чём. */
    fun open(context: Context, ad: Ad) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ad.url))) }
        IslandBus.scope.launch {
            IslandBus.post(
                app.orubus.island.core.Notice(
                    ad.title,
                    null,
                    app.orubus.island.island.Ios.load(context, app.orubus.island.R.drawable.ios_arrow_up),
                    app.orubus.island.core.Tints.Blue,
                    style = app.orubus.island.core.NoticeStyle.PILL,
                ),
            )
        }
    }

    /** Вписанный в сборку проект Supabase: только URL и анонимный ключ, таблица уже существует. */
    var ADS_SUPABASE_URL: String? = System.getenv("ISLAND_SUPABASE_URL")
    var ADS_SUPABASE_ANON_KEY: String? = System.getenv("ISLAND_SUPABASE_ANON_KEY")
}
