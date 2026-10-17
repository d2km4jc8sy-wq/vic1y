package com.example.s1client

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.example.s1client.feature_changelog.ChangelogActivity
import com.example.s1client.util.ThemeManager
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 7 主题 × 3 关键页截图矩阵（复用 CiSmokeCaptureTest 已验证的管线）：
 * Robolectric 直启真实 Activity + decorView 手动 measure(1080×2000 EXACTLY)/layout/
 * draw(Canvas(bitmap)) + PNG 落盘 app/build/screenshots/theme{N}_{页}.png + 像素唯一
 * 颜色数断言（首 5 万像素）。不用 compose-ui-test 规则、不调 waitForIdle
 * （waitForIdle/captureToImage 在 Robolectric 上会 ComposeTimeoutException，CI 实证）。
 *
 * 主题切换手法：每组组合前调 ThemeManager.setTheme(N)（public API，立即生效并持久化），
 * 随后 buildActivity(X).setup()——Activity 以该主题完成整棵树的首渲染。
 * ThemeManager 是进程内单例：同一 @Test 方法（同一 Robolectric sandbox）里 21 次
 * 迭代状态连续，后一次 setTheme 覆盖前值，无需重置。
 *
 * 循环前先预热一次 ChangelogActivity（setup 后立即 destroy）：S1App.onCreate →
 * ThemeManager.init() 全程只跑一次；若首次 setTheme 发生在 init 之前，appContext
 * 尚为空、持久化被静默跳过，init 随后会用存档默认值（焙茶午后）覆盖内存值，
 * 首组 Linear 就会错渲染成浅色。预热让 init 先落定，之后每组 setTheme 全程有效。
 *
 * 已知行为（ThemeManager.applyTheme → migrateHiddenTheme）：隐藏主题索引在 setTheme
 * 入口收敛——2(樱雾粉霜)/4(苔绿灰阶) → 焙茶午后(0)；1(曜石玄夜)/3(朱砂红白) →
 * Linear 墨蓝(6)。故 theme1/3 实渲染 Linear、theme2/4 实渲染焙茶午后；落盘文件仍按
 * 请求索引命名，未来放开隐藏主题时矩阵自动接管真实渲染，无需改测试。
 *
 * 断言阈值按页分档（首 5 万像素唯一颜色数，实测可调）：
 * main ≥ 10（离线态 PostViewModel 降级 Loading 骨架，颜色偏少属正常）、
 * settings ≥ 30、changelog ≥ 50。另有：文件存在 + 大小 > 0 字节。
 * 单组失败不中断矩阵：记录后继续跑完 21 组，最后一次性汇总，CI 一轮看全貌。
 *
 * Windows 本机 NATIVE runtime 缺失（Robolectric 4.11.1 只发 Linux/macOS
 * nativeruntime），测试体首行 Assume 按环境跳过，跳过是干净的；真正跑绿
 * （21 张截图落盘 + 像素断言）在 CI Linux。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeMatrixCaptureTest {

    /** 一项 = 目标 Activity + 落盘标签 + 唯一颜色数下限（首 5 万像素采样，与页面复杂度相称） */
    private data class Page(
        val tag: String,
        val minUniqueColors: Int,
        val clazz: Class<out Activity>
    )

    @Test
    fun `主题矩阵_7主题x3关键页_真实渲染落盘`() {
        // Windows 开发机跳过；只有 Linux（CI）才往下走
        assumeTrue(
            "NATIVE 图形仅在 Linux CI 上运行（Windows 无 nativeruntime）",
            "linux".equals(System.getProperty("os.name"), ignoreCase = true)
        )

        val pages = listOf(
            Page("main", 10, MainActivity::class.java),
            Page("settings", 30, SettingsActivity::class.java),
            Page("changelog", 50, ChangelogActivity::class.java),
        )
        val themes = ThemeManager.THEME_LIGHT..ThemeManager.THEME_LINEAR
        val totalCombos = themes.count() * pages.size
        val w = 1080
        val h = 2000
        val dir = File("build/screenshots").apply { mkdirs() }
        val failures = mutableListOf<String>()

        // 预热：Application 先建好、ThemeManager.init() 落定（appContext 就位），
        // 之后循环内的 setTheme 才能立即生效并持久化，不被存档默认值覆盖。
        Robolectric.buildActivity(ChangelogActivity::class.java).setup().destroy()

        for (theme in themes) {
            for (page in pages) {
                val name = "theme${theme}_${page.tag}"
                var controller: ActivityController<*>? = null
                try {
                    ThemeManager.setTheme(theme)
                    controller = Robolectric.buildActivity(page.clazz)
                    val activity = controller.setup().get()

                    val decor = activity.window.decorView
                    decor.measure(
                        View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
                    )
                    decor.layout(0, 0, w, h)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    decor.draw(Canvas(bmp))

                    val png = File(dir, "$name.png")
                    png.outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    assertTrue("$name 截图应已落盘: ${png.absolutePath}", png.isFile)
                    assertTrue("$name 截图不应为空", png.length() > 0)

                    // 像素多样性：整帧若只渲染出背景色（Compose 未挂载），颜色数趋近个位数。
                    // 与 CiSmokeCaptureTest 相同取样（首 5 万像素），阈值按页分档：
                    // main 离线态是 Loading 骨架、颜色偏少属正常。
                    val px = IntArray(w * h)
                    bmp.getPixels(px, 0, w, 0, 0, w, h)
                    val sampled = px.take(50000).toHashSet()
                    println(
                        "MATRIX $name req=$theme effective=${ThemeManager.currentTheme} " +
                            "pngBytes=${png.length()} uniqueColors=${sampled.size}"
                    )
                    assertTrue(
                        "$name 渲染应为真实内容（唯一颜色数=${sampled.size}，下限=${page.minUniqueColors}）",
                        sampled.size >= page.minUniqueColors
                    )
                } catch (t: Throwable) {
                    // 矩阵诊断优先：记下本组组合继续跑，最后一次性汇总，避免 21 组首败即断
                    failures.add("$name: ${t.javaClass.simpleName}: ${t.message}")
                    println("MATRIX $name FAILED ${t.javaClass.simpleName}: ${t.message}")
                } finally {
                    try {
                        controller?.destroy()
                    } catch (_: Throwable) {
                        // 销毁失败不掩盖主断言
                    }
                }
            }
        }

        assertTrue(
            "矩阵存在未达标组合（${failures.size}/$totalCombos）:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }
}
