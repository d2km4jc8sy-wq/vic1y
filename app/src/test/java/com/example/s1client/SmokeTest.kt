package com.example.s1client

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.s1client.util.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 冒烟测试：App 能初始化、MainActivity 能启动并进入前台。
 * 网络在 Robolectric 下会失败——失败走页面的错误态 UI，冒烟只断言"启动成功"。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class SmokeTest {

    @Test
    fun `S1App 初始化不崩溃`() {
        val app = ApplicationProvider.getApplicationContext<S1App>()
        assertNotNull(app)
    }

    @Test
    fun `MainActivity 能启动并进入 Resumed`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
            assertEquals(
                "MainActivity 应处于 RESUMED 状态",
                androidx.lifecycle.Lifecycle.State.RESUMED,
                scenario.state
            )
        }
    }


    /** Intent 路由冒烟：ThreadDetailActivity 带 tid 能启动（挂了会立刻暴露） */
    @Test
    fun `ThreadDetailActivity 带参启动冒烟`() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ThreadDetailActivity::class.java
        ).apply {
            putExtra("tid", "2206666")
            putExtra("subject", "冒烟测试帖")
        }
        ActivityScenario.launch<ThreadDetailActivity>(intent).use { scenario ->
            assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
