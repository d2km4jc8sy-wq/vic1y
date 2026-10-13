# 新主题模板（只加一个调色板分支，不碰组件）

## 步骤
1. `ui/theme/S1Theme.kt`：`s1PaletteFor()` 的 when 增加一个分支，返回填好的 `S1Colors(...)`。
   必填：accent/onAccent/textMain/textSub/cardBg/glassCard 系/bgTop/bgBottom/isDark/flowCoverColors。
   **flowCoverColors 首尾必须同色**（流动封面平铺不变量，ThemePaletteTest 会查）。
2. `util/ThemeManager.kt`：加 `const val THEME_XXX = <下一个编号>`；
   `coverStyleForCurrent()` 加分支（文字封面选 1/2 之一；有专属封面设计才自定义）。
3. `res/values/styles.xml`：加 `Theme_S1Client_XXX`（Android 层状态栏/窗口底色，深浅对齐 isDark）。
4. 主题面板：`ui/ThemePickerSheet.kt` 的 getVisibleThemes() 加入新条目（若无特殊封面可复用现有展示）。
5. 跑 `gradle testDebugUnitTest`：ThemePaletteTest 会验证结构完整性与色带不变量。

## 禁止
- 在组件里写 `Color(0x…)` 适配新主题（一切经 palette 字段下发）
- 给新主题单独写组件分支（组件只认 token，不认主题名——幻彩 cobra 是唯一既定特例）
