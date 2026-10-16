package com.example.s1client

import com.example.s1client.feature_changelog.ChangelogActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * CI 截图冒烟（阶段二④，2026-10-17 定稿版）：
 * 用 Robolectric 直接启动真实的 ChangelogActivity，手动 measure/layout/draw，
 * 把整帧渲染成 PNG 落盘，并断言**像素多样性**（整帧唯一颜色数 > 阈值）——
 * 这是「画面真的渲染出来了」的最强证据，不依赖 compose-ui-test 的空闲等待
 * （waitForIdle / captureToImage 在 Robolectric 上会 ComposeTimeoutException，CI 实测）。
 *
 * Windows 本机 NATIVE runtime 缺失（Robolectric 4.11.1 只发 Linux/macOS nativeruntime），
 * 测试体首行 Assume 按环境跳过——没有 Activity 规则抢跑，跳过是干净的。
 * 真正跑绿（截图落盘 + 像素断言）的地方是 CI Linux。
 *
 * 不接 Roborazzi Gradle 插件时，Roborazzi 的 capture 系 API 会静默 no-op
 * （roborazziEnabled() 只认 roborazzi.test.record 系统属性），因此本测试
 * 自行落盘 PNG，不经 Roborazzi 的捕获路径；Roborazzi 依赖保留给后续对比矩阵。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CiSmokeCaptureTest {

    @Test
    fun `CI冒烟_ChangelogActivity真实渲染落盘`() {
        // Windows 开发机跳过；只有 Linux（CI）才往下走
        assumeTrue(
            "NATIVE 图形仅在 Linux CI 上运行（Windows 无 nativeruntime）",
            "linux".equals(System.getProperty("os.name"), ignoreCase = true)
        )
        System.setProperty("roborazzi.test.record", "true")
        try {
            val controller = Robolectric.buildActivity(ChangelogActivity::class.java)
            val activity = controller.setup().get()

            val w = 1080
            val h = 2000
            val decor = activity.window.decorView
            decor.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, w, h)
            val bmp = android.graphics.Bitmap.createBitmap(
                w, h, android.graphics.Bitmap.Config.ARGB_8888
            )
            decor.draw(android.graphics.Canvas(bmp))

            val dir = File("build/screenshots").apply { mkdirs() }
            val png = File(dir, "ci_smoke.png")
            png.outputStream().use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            assertTrue("截图应已落盘：${png.absolutePath}", png.isFile)
            assertTrue("截图不应为空", png.length() > 0)

            // 像素多样性：整帧若只渲染出背景色（Compose 未挂载），颜色数会趋近个位数。
            // 取样前 5 万像素，要求唯一颜色数 > 50 —— 顶栏/卡片/文字/分隔线齐备时轻松过百。
            val px = IntArray(w * h)
            bmp.getPixels(px, 0, w, 0, 0, w, h)
            val sampled = px.take(50000).toHashSet()
            assertTrue("渲染应为真实内容（唯一颜色数=${sampled.size}）", sampled.size > 50)

            controller.destroy()
        } finally {
            System.clearProperty("roborazzi.test.record")
        }
    }
}
