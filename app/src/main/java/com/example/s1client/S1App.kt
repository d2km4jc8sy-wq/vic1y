package com.example.s1client

import android.app.Activity
import com.example.s1client.data.store.SettingStore
import android.app.Application
import android.os.Bundle
import com.example.s1client.data.api.RetrofitClient
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.data.store.BlockedAuthorManager
import com.example.s1client.data.store.ExpandedFloors
import com.example.s1client.util.Haptic
import com.example.s1client.data.store.HiddenFloors
import com.example.s1client.data.store.HiddenPostManager
import com.example.s1client.data.store.PostBookmarkManager
import com.example.s1client.data.store.ThreadMutedUsers
import com.example.s1client.util.ThemeManager

class S1App : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        // 全局初始化（避免每个 Activity 重复调用）
        RetrofitClient.init(this)
        ForumDataCache.init(this)
        ThemeManager.init(this)
        com.example.s1client.util.NetworkMonitor.init(this)
        // 图标切换（2026-10-18 设计者澄清后恢复：上一版误解需求删了功能）。
        // 设计者本意只是「桌面图标和其他 App 不搭，复位成默认」，切换功能本身保留。
        // 本版本一次性把图标复位为默认；之后照常走 ensureValid 自愈。
        val iconPrefs = getSharedPreferences("s1_client_prefs", MODE_PRIVATE)
        if (iconPrefs.getBoolean("reset_default_icon_once_1018", true)) {
            iconPrefs.edit().putBoolean("reset_default_icon_once_1018", false).apply()
            com.example.s1client.util.AppIconManager.apply(this, "default")
        } else {
            com.example.s1client.util.AppIconManager.ensureValid(this)
        }
        BlockedAuthorManager.init(this)
        com.example.s1client.data.store.FavoriteManager.initLocal(this)
        HiddenPostManager.init(this)
        ThreadMutedUsers.init(this)
        ExpandedFloors.init(this)
        HiddenFloors.init(this)
        PostBookmarkManager.init(this)
        Haptic.loadEnabled(this)

        // 配置 Coil 用图片专用 HttpClient，并收紧缓存上限
        // 默认磁盘缓存上限高达 250MB、内存缓存 25% RAM，S1 原图单张 200~450KB，很容易吃满
        val imageMemoryCache = coil.memory.MemoryCache.Builder(this)
            .maxSizePercent(0.15)
            .build()
        val imageDiskCache = coil.disk.DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            // 缓存上限档位化（R1 低成本批）：设置→数据管理可调，默认 100MB 与历史行为一致
            .maxSizeBytes(SettingStore.cacheCapMb(this).coerceAtLeast(50).toLong() * 1024L * 1024L)
            .build()

        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(this)
                .okHttpClient { RetrofitClient.imageHttpClient }
                .crossfade(150)
                .memoryCache(imageMemoryCache)
                .diskCache(imageDiskCache)
                .components {
                    // 只用 GifDecoder 处理 GIF，其他图片走 Coil 默认解码器
                    add(coil.decode.GifDecoder.Factory())
                }
                .build()
        )

        registerActivityLifecycleCallbacks(this)

        // 诊断兜底：任何未捕获异常都落盘到 Android/data/.../files/crash_trace.txt，
        // 真机崩溃时即使 logcat 没逮到，重连后也能 adb pull 出来定位（稳定后删除）。
        installCrashTracer()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // 深色跟随系统（阶段四 2026-10-17）：系统深浅变化时重映射主题
        // （Application 级回调不依赖 Activity 的 configChanges 声明，必定收到 uiMode）
        ThemeManager.onSystemNightModeChanged(
            (newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        )
    }

    private fun installCrashTracer() {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val f = File(dir, "crash_trace.txt")
                // 大小上限（2026-10-16）：超过 512KB 滚动为 .old，避免常年使用无限增长
                if (f.exists() && f.length() > 512L * 1024) {
                    val old = File(dir, "crash_trace.old.txt")
                    old.delete()
                    f.renameTo(old)
                }
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                f.appendText("==== crash @ $ts thread=${thread.name} ====\n")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                f.appendText(sw.toString())
                f.appendText("\n")
            } catch (_: Exception) { /* 兜底失败也不影响原异常处理 */ }
            def?.uncaughtException(thread, throwable)
        }
    }

    // ============ Activity 生命周期 ============

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // 统一登记：Compose Activity 不继承 BaseActivity、不会自己 markApplied，
        // 不在这里登记的话，选完主题返回首页时检测不到变化、不会 recreate
        ThemeManager.markApplied(activity.javaClass.simpleName)
    }

    override fun onActivityStarted(activity: Activity) {
        // 什么都不做
    }

    override fun onActivityResumed(activity: Activity) {
        // 关键：Activity resumed 时检查主题是否变了
        if (ThemeManager.hasThemeChanged(activity.javaClass.simpleName)) {
            // 用 post 避免在当前回调里直接 recreate 导致的时序问题
            activity.window.decorView.post {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.recreate()
                }
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // 什么都不做
    }

    override fun onActivityStopped(activity: Activity) {
        // 什么都不做
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // 什么都不做
    }

    override fun onActivityDestroyed(activity: Activity) {
        // 什么都不做
    }
}
