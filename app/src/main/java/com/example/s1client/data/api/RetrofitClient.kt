package com.example.s1client.data.api

import android.content.Context
import android.util.Log
import com.example.s1client.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.s1client.data.store.SecureStore

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "https://stage1st.com/2b/"
    internal const val MAIN_HOST = "stage1st.com"
    private const val COOKIE_PREF = "s1_cookie_prefs"
    private const val COOKIE_KEY = "cookies"
    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private var appContext: Context? = null

    // ===== 网络参数（阶段四 P8，2026-10-17）：DNS 覆盖 + 基础网址重写 =====
    // 快照由 NetworkParamsActivity 写入；Dns 与重写拦截器每次请求实时读取——
    // 参数变化即时生效，无需重建客户端。null = 未设置（全默认直连主域）。
    data class NetworkOverride(
        val forceBaseUrl: Boolean,
        val baseUrlHost: String,
        val baseUrlScheme: String,
        val baseUrlPort: Int,
        val forceIp: Boolean,
        val ips: List<String>
    )

    @Volatile
    private var networkOverride: NetworkOverride? = null

    fun applyNetworkOverride(o: NetworkOverride?) {
        networkOverride = o
    }

    /** 该 host 是否走论坛语义（S1 头/Cookie 域判定）；含基础网址替换后的自定义域 */
    private fun isForumHost(host: String): Boolean {
        if (host.endsWith("stage1st.com")) return true
        val o = networkOverride ?: return false
        return o.forceBaseUrl && host == o.baseUrlHost
    }

    /** DNS 覆盖：强制 IP 模式下，论坛域解析到指定 IP（SNI/证书仍按原域名校验） */
    private fun pinnedIps(hostname: String): List<java.net.InetAddress>? {
        val o = networkOverride ?: return null
        if (!o.forceIp || o.ips.isEmpty()) return null
        if (!isForumHost(hostname)) return null
        val pinned = o.ips.mapNotNull { raw ->
            runCatching {
                val addr = java.net.InetAddress.getByName(raw)
                java.net.InetAddress.getByAddress(hostname, addr.address)
            }.getOrNull()
        }
        return pinned.ifEmpty { null }
    }

    internal fun parseNetworkOverride(
        forceBaseUrl: Boolean,
        baseUrlRaw: String,
        forceIp: Boolean,
        ipRaw: String
    ): NetworkOverride? {
        var host: String? = null
        var scheme = "https"
        var port = 443
        if (forceBaseUrl) {
            val uri = runCatching { java.net.URI(baseUrlRaw.trim()) }.getOrNull() ?: return null
            host = uri.host?.lowercase()
            if (host.isNullOrBlank()) return null
            scheme = uri.scheme?.lowercase()?.ifBlank { "https" } ?: "https"
            port = if (uri.port > 0) uri.port else if (scheme == "https") 443 else 80
        }
        val ips = if (forceIp)
            ipRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else emptyList()
        if (forceIp && ips.isEmpty()) return null
        return NetworkOverride(
            forceBaseUrl = forceBaseUrl,
            baseUrlHost = host.orEmpty(),
            baseUrlScheme = scheme,
            baseUrlPort = port,
            forceIp = forceIp,
            ips = ips
        )
    }
    private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (!url.host.endsWith("stage1st.com")) return
            val hostMap = cookieStore.getOrPut(url.host) { mutableMapOf() }
            for (c in cookies) {
                if (c.value.isEmpty()) hostMap.remove(c.name) else hostMap[c.name] = c
            }
            persistCookies()
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            if (!url.host.endsWith("stage1st.com")) return emptyList()
            val cookies = mutableListOf<Cookie>()
            cookieStore[url.host]?.values?.let { cookies.addAll(it) }
            if (url.host != "stage1st.com" && url.host.endsWith(".stage1st.com")) {
                cookieStore["stage1st.com"]?.values?.let { cookies.addAll(it) }
            }
            return cookies
        }
    }

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            loadCookies()
        }
    }

    // ===== Cookie 持久化：JSON =====

    private fun persistCookies() {
        val ctx = appContext ?: return
        try {
            val arr = JSONArray()
            for ((host, map) in cookieStore) {
                for ((_, c) in map) {
                    val o = JSONObject()
                    o.put("host", host)
                    o.put("name", c.name)
                    o.put("value", c.value)
                    o.put("expiresAt", c.expiresAt)
                    o.put("path", c.path)
                    o.put("secure", c.secure)
                    o.put("httpOnly", c.httpOnly)
                    arr.put(o)
                }
            }
            ctx.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE).let { prefs ->
                // Cookie 等于账号凭据：优先加密落盘，失败才退回明文（保住登录态优先）
                if (!com.example.s1client.data.store.SecureStore.putEncrypted(prefs, COOKIE_KEY, arr.toString())) {
                    prefs.edit().putString(COOKIE_KEY, arr.toString())
                        .putBoolean("$COOKIE_KEY.enc", false).apply()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "persistCookies failed", e)
        }
    }

    private fun loadCookies() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE)
        // SecureStore 内部会自动识别是否加密，未标记的旧数据按明文读 → 天然兼容迁移
        val data = com.example.s1client.data.store.SecureStore.getString(prefs, COOKIE_KEY).orEmpty()
        if (data.isBlank()) return
        try {
            val arr = JSONArray(data)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val host = o.getString("host")
                val builder = Cookie.Builder()
                    .name(o.getString("name"))
                    .value(o.getString("value"))
                    .hostOnlyDomain(host)
                    .path(o.optString("path", "/"))
                val exp = o.optLong("expiresAt", Long.MAX_VALUE)
                if (exp in 1 until Long.MAX_VALUE) builder.expiresAt(exp)
                if (o.optBoolean("secure", false)) builder.secure()
                if (o.optBoolean("httpOnly", false)) builder.httpOnly()
                val cookie = builder.build()
                cookieStore.getOrPut(host) { mutableMapOf() }[cookie.name] = cookie
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadCookies: JSON parse failed, fallback to legacy format", e)
            loadCookiesLegacy(data)
        }
    }

    private fun loadCookiesLegacy(data: String) {
        for (line in data.split("\n")) {
            if (line.isBlank()) continue
            val parts = line.split("\t")
            if (parts.size < 5) continue
            try {
                val cookie = Cookie.Builder()
                    .name(parts[1]).value(parts[2])
                    .hostOnlyDomain(parts[0]).path(parts[4])
                    .expiresAt(parts[3].toLongOrNull() ?: Long.MAX_VALUE)
                    .build()
                cookieStore.getOrPut(parts[0]) { mutableMapOf() }[parts[1]] = cookie
            } catch (e: Exception) {
                Log.w(TAG, "loadCookiesLegacy: skip bad line", e)
            }
        }
    }

    fun clearCookies() {
        cookieStore.clear()
        appContext?.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE)
            ?.edit()?.clear()?.apply()
    }

    // ===== 主 HttpClient（用于 S1 API）=====
    val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    return pinnedIps(hostname) ?: Dns.SYSTEM.lookup(hostname)
                }
            })
            .addInterceptor { chain ->
                // 基础网址重写（P8）：主域请求整 host 换到备用域（证书/SNI 按备用域自带，Cookie 按域隔离）
                var request = chain.request()
                networkOverride?.let { o ->
                    if (o.forceBaseUrl && request.url.host == MAIN_HOST) {
                        runCatching {
                            request = request.newBuilder()
                                .url(request.url.newBuilder()
                                    .scheme(o.baseUrlScheme)
                                    .host(o.baseUrlHost)
                                    .port(o.baseUrlPort)
                                    .build())
                                .build()
                        }
                    }
                }
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val isS1 = isForumHost(original.url.host)
                val b = original.newBuilder()
                    .header("User-Agent", DESKTOP_UA)
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                if (isS1) {
                    // 调用方指定了 Referer 就别覆盖（上传附件要用发帖页当来源）
                    if (original.header("Referer") == null) {
                        b.header("Referer", "https://stage1st.com/2b/")
                    }
                    b.header("Origin", "https://stage1st.com")
                } else {
                    b.header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    b.removeHeader("Referer")
                    b.removeHeader("Origin")
                }
                chain.proceed(b.build())
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            // 长贴（几百楼）单页响应体很大，弱网下 20s readTimeout 很容易踩到 ——
            // 表现为"进入帖子提示加载失败"。callTimeout 兜住整个调用（连接+读+写+重试）。
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // ===== 图片专用 HttpClient（强制 HTTP/1.1，更长超时）=====
    val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .protocols(listOf(Protocol.HTTP_1_1))
            .dns(object : Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    return pinnedIps(hostname) ?: Dns.SYSTEM.lookup(hostname)
                }
            })
            .addInterceptor { chain ->
                var request = chain.request()
                networkOverride?.let { o ->
                    if (o.forceBaseUrl && request.url.host == MAIN_HOST) {
                        runCatching {
                            request = request.newBuilder()
                                .url(request.url.newBuilder()
                                    .scheme(o.baseUrlScheme)
                                    .host(o.baseUrlHost)
                                    .port(o.baseUrlPort)
                                    .build())
                                .build()
                        }
                    }
                }
                chain.proceed(request)
            }
            .addInterceptor { chain ->
                val original = chain.request()
                val isS1 = isForumHost(original.url.host)
                val b = original.newBuilder()
                    .header("User-Agent", DESKTOP_UA)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                if (isS1) {
                    b.header("Referer", "https://stage1st.com/2b/")
                }
                chain.proceed(b.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val api: S1Api by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(S1Api::class.java)
    }

    /**
     * 拉取 HTML。
     * 注意：失败会抛异常，调用方请用 safeApiCall 包裹。
     */
    suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP " + resp.code)
            resp.body?.string().orEmpty()
        }
    }
}
