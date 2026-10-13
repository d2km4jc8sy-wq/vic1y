package com.example.s1client

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.example.s1client.data.api.RetrofitClient
import com.example.s1client.data.cache.ForumDataCache
import com.example.s1client.util.S1Notify
import com.example.s1client.util.ThemeManager
import com.example.s1client.util.applyS1NavigationBarColor
import com.example.s1client.util.isS1LightSystemBarIcons

/**
 * 所有 Activity 的基类。
 * 统一初始化 ThemeManager / RetrofitClient / ForumDataCache，
 * 并处理主题切换（变了就 recreate）+ 全面屏 edge-to-edge。
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(this)
        setTheme(ThemeManager.getThemeRes())
        ThemeManager.markApplied(javaClass.simpleName)
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        ForumDataCache.init(this)
        // setupEdgeToEdge 需在子类 setContentView 之后调用，这里不调
    }

    override fun onResume() {
        super.onResume()
        if (ThemeManager.hasThemeChanged(javaClass.simpleName)) {
            recreate()
            return
        }
        onResumeInternal()
    }

    /** 子类重写此方法代替 onResume。 */
    protected open fun onResumeInternal() {}

    /**
     * 统一风格化通知（替代裸 Toast）。
     * - INFO：普通提示（配对轻震）
     * - SUCCESS：成功（配对 medium 震）
     * - ERROR：失败/破坏（配对 heavy 震）
     * 视觉走玻璃卡片 + 语义色，全 App 一致。
     */
    protected fun toast(
        message: String,
        type: S1Notify.Type = S1Notify.Type.INFO,
        silent: Boolean = false
    ) {
        S1Notify.show(this, message, type, silent)
    }

    /**
     * 开启全面屏 edge-to-edge：
     * - 系统栏透明
     * - decor 不再自动 fit insets（子类需手动用 applyTopInset / applyBottomInset 给顶栏/底栏加 padding）
     * - 图标颜色跟随主题（按 s1BgPage 底色亮度自动选深浅）
     * 幂等：重复调用安全。
     */
    protected fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        // 导航栏填「页面渐变收尾色」的不透明值 —— 不用透明，理由见
        // applyS1NavigationBarColor 的注释（MIUI 会给透明导航栏叠一层近白底）。
        // View 体系的页面底部没有玻璃底栏，所以传 false。
        applyS1NavigationBarColor(hasGlassBottomBar = false)
        // 图标明暗按当前主题 s1BgPage 底色亮度自动：亮底深色图标 / 暗底浅色图标
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = isS1LightSystemBarIcons()
        controller.isAppearanceLightNavigationBars = isS1LightSystemBarIcons()
    }

    /** 给顶栏加 statusBars 高度的 paddingTop。会在原 paddingTop 基础上叠加 inset。 */
    protected fun applyTopInset(view: View) {
        val initialPaddingTop = view.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = initialPaddingTop + sb.top)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    /** 给底栏加 navigationBars 高度的 paddingBottom。会在原 paddingBottom 基础上叠加 inset。 */
    protected fun applyBottomInset(view: View) {
        val initialPaddingBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = initialPaddingBottom + nb.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    /**
     * 输入栏专用：键盘弹起时按键盘高度垫、收起时按手势导航条高度垫（取两者最大值）。
     * decorFitsSystemWindows=false 后 ADJUST_RESIZE 不再自动压缩布局，必须手动消费 IME insets。
     */
    protected fun applyImeBottomInset(view: View) {
        val initialPaddingBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = initialPaddingBottom + maxOf(ime.bottom, nb.bottom))
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}