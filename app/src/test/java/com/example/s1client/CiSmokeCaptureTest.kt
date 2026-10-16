package com.example.s1client

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.s1client.ui.theme.S1Theme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * CI 截图冒烟：证明 Roborazzi + Robolectric NATIVE 图形能在 Linux CI 上真实渲染并落盘。
 * Windows 本机 NATIVE runtime 缺失（Robolectric 4.11.1 只发布 Linux/macOS 的 nativeruntime），
 * 真正跑绿（截图落盘）的地方是 CI Linux，不是开发机。
 *
 * 跳过闸门必须放在 **RuleChain 外层**：createComposeRule 内部是 ActivityScenarioRule，
 * 会在测试体语句之前启动 Activity —— Windows 上 NATIVE 初始化当场崩，
 * 测试体里的 Assume 根本来不及执行（子任务实测）。
 *
 * 不接 Roborazzi Gradle 插件时，captureRoboImage 默认静默 no-op（1.8.0 源码：
 * roborazziEnabled() 只认 roborazzi.test.record/compare/verify 系统属性），
 * 所以在测试内自行打开 record 开关，保持构建脚本零侵入。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CiSmokeCaptureTest {

    private val composeRule = createComposeRule()

    @get:Rule
    val chain: TestRule = RuleChain.outerRule(LinuxOnlyRule(composeRule)).around(composeRule)

    @Test
    fun `CI冒烟截图`() {
        System.setProperty("roborazzi.test.record", "true")
        try {
            composeRule.setContent {
                S1Theme {
                    Text("S1Client CI smoke")
                }
            }
            // 不调 waitForIdle：Robolectric 上帧时钟可能永不静默（ComposeTimeoutException 实测），
            // 静态内容 setContent 后 captureToImage 自带一次真实绘制，无需等待空闲
            // 阶段二④调整（2026-10-17）：compileSdk 35 下 roborazzi 1.8.0 的
            // captureRoboImage 扩展解析失败（版本代差嫌疑），改用 compose-ui-test
            // 原生 captureToImage + PNG 落盘 —— 同样证明 NATIVE 图形真实渲染并产出产物；
            // Roborazzi 依赖保留（后续对比矩阵再启用其 capture/verify）。
            val bmp = composeRule.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            val png = File(dir, "ci_smoke.png")
            png.outputStream().use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            assertTrue("截图应已落盘：${png.absolutePath}", png.isFile)
            assertTrue("截图不应为空", png.length() > 0)
        } finally {
            System.clearProperty("roborazzi.test.record")
        }
    }
}

/**
 * 仅 Linux（CI）放行的外层闸门：Windows 上把整个测试按 skipped 处理
 * （AssumptionViolatedException 在内层 ActivityScenarioRule 启动**之前**抛出，
 * 不碰 NATIVE 图形栈）。
 */
private val NOOP_RUNNABLE = Runnable {}

private class LinuxOnlyRule(private val inner: TestRule) : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        GatedStatement(base)

    // 注意：这里不能把 Runnable{} 字面量直接写进超类构造器——
    // object 体的花括号会被解析成尾随 lambda 报「Too many arguments」。
    private class GatedStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            assumeTrue(
                "NATIVE 图形截图仅在 Linux CI 上运行（Windows 无 nativeruntime）",
                "linux".equals(System.getProperty("os.name"), ignoreCase = true)
            )
            base.evaluate()
        }
    }
}
