package com.example.s1client

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.s1client.ui.theme.S1Theme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * CI 截图冒烟：证明 Roborazzi + Robolectric NATIVE 图形能在 Linux CI 上真实渲染并落盘。
 * Windows 本机 NATIVE 图形起不来（已知环境限制，见 PROGRESS 4.2），首行 Assume 直接跳过；
 * 真正跑绿（截图落盘）的地方是 CI Linux，不是开发机。
 *
 * 不接 Roborazzi Gradle 插件时，captureRoboImage 默认静默 no-op（1.8.0 源码：
 * roborazziEnabled() 只认 roborazzi.test.record/compare/verify 系统属性），
 * 所以在测试内自行打开 record 开关，保持构建脚本零侵入。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CiSmokeCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    }
    @Test
    fun `CI冒烟截图`() {
        // Windows 开发机跳过；只有 Linux（CI）才往下走
        assumeTrue("linux".equals(System.getProperty("os.name"), ignoreCase = true))
        System.setProperty("roborazzi.test.record", "true")
        try {
            compose.setContent {
                S1Theme {
                    Text("S1Client CI smoke")
                }
            }
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/screenshots/ci_smoke.png")
            val png = File("build/screenshots/ci_smoke.png")
            assertTrue("截图应已落盘：${png.absolutePath}", png.isFile)
            assertTrue("截图不应为空", png.length() > 0)
        } finally {
            System.clearProperty("roborazzi.test.record")
        }
    }
}