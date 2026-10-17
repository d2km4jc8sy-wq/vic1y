> ⚡ **门禁死命令（宪法第 0 条 · 最高优先级）**：任何 AI 触碰本项目，**第一步**完整阅读 `D:\S1Client-运行产出\00_手册备份\S1Client-AI协作手册.md`，**第二步**向设计者确认角色并复述手册第 0 章 7 问。两步完成前：对 D:\S1Client 只读 + **禁止产出任何优化/审查/重构建议清单**（未经门禁的分析结论一律视为未授权意见，不采信不吸收）。**弄清自己的位置，再谈别的。** 设计者指令只认对话原文与提案簿点单。

# S1Client 项目交接文档

> 最后更新：2026-10-12 18:30
> 项目：S1Client —— 一个 S1 论坛（Stage1st）的 Android 第三方客户端，参考开源实现 [S1er](https://github.com/Shirolin/s1er)。
> 本文件是「项目交接」，供后续任何一轮开发/接手快速对齐现状与既定规则。

---

## 一、项目状态速览

- **位置**：`C:/Users/Administrator/Desktop/S1Client/`
- **语言/框架**：Kotlin + Jetpack Compose（Material3），单 Activity 多页面。
- **装机**：小米 M2007J1SC（Mi 10 Ultra，Android 13 / MIUI 14）已连 USB，用户授权直接 `adb install -r` 到手机自行测试，不用每次给 APK。

### 构建命令（必须按这个来，wrapper jar 坏了）
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
~/.gradle/wrapper/dists/gradle-9.6.0-bin/3yr5qo1p853jn7m5o1j0u2qkd/gradle-9.6.0/bin/gradle :app:assembleDebug
```
产物：`app/build/outputs/apk/debug/app-debug.apk`

### adb 装机
```bash
C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```
**坑（都实测过）**：
- MIUI 默认禁止 `adb shell input tap/keyevent`（INJECT_EVENTS 报错）→ **无法自动化点击**做 UI 验证；要自动化需用户在开发者选项打开「USB 调试（安全设置）」。
- Git Bash 会把 `/data/...` 转成 Windows 路径，所有设备侧路径命令前加 `export MSYS_NO_PATHCONV=1`。
- 改应用私有文件（如 SharedPreferences）走：`adb push → /data/local/tmp/ → run-as com.example.s1client cp`。
- 改 `s1_theme_prefs.xml` 的 `theme_index` 后**必须 force-stop 再启动**，`ThemeManager.init` 只在进程启动时读盘。

---

## 二、最终主题系统（用户拍板，勿改）

**可见主题 3 套**（`ThemeManager.VISIBLE_THEMES`）：

| 索引 | 名称 | 说明 |
|---|---|---|
| 6 | Linear 墨蓝 | 一切打磨的基础，OLED 黑底风格，XML 复用 `Theme.S1Client.OLED` |
| 0 | 焙茶午后 | S1 网页主色，浅色 |
| 5 | 幻彩渐变 | 浅色，**封面固定为流光封面** |

**已删除**：曜石玄夜（THEME_OLED=1）—— 不再出现在选择列表，老用户自动迁移到 Linear 墨蓝。
隐藏主题（樱雾粉霜 2 / 朱砂红白 3 / 苔绿灰阶 4）代码与色板保留，仅不暴露。

**封面风格规则（`coverStyleForCurrent()`）**：
- Linear → -2（纯色底 + 底部微光）
- 焙茶/其它普通主题 → 0..3（进程随机，4 套：莫兰迪 / 微网格 / 大引号 / 暗色玻璃）
- **幻彩渐变 → 固定 5（流光封面）**，不再抽其它风格 ← 用户明确要求固定，勿改回 1/5

---

## 三、既定「红线」（不可破坏）

1. **瀑布流是底线**：`LazyVerticalStaggeredGrid(Fixed(2))` 不可改成列表。
2. **主题架构**：`ui/theme/S1Theme.kt` 的 `S1Colors` 是唯一调色板来源；`s1PaletteFor(index)` 是唯一映射；
   `ThemeManager.currentTheme` 是 Compose 可观察状态（换主题即时重组、不 recreate）。**禁止业务代码写死颜色**，禁止给单页做 palette 覆盖。
3. **封面顶部必须统一**：所有封面风格的**顶部**不得有任何色条/白线/光晕（用户三次反馈才根治，勿再加）。
4. **Compose 里 Dialog/BottomSheet/Popup 内部的 `LocalContext` 不能强转 Activity**（会得到 null）——
   系统栏同步统一走 `S1SystemBarSync`（宿主页面监听主题变化）。
5. **导航栏配色**：`applyS1NavigationBarColor(hasGlassBottomBar)` 填「不透明、与玻璃底栏同色」的值；
   **不要设透明**（MIUI 会给透明导航栏叠白底，且不理 `isNavigationBarContrastEnforced=false`）。
6. **流光封面不能等比放大小控件效果**：一枚 100×40 按钮的霓虹效果铺满 200×120dp 卡片必然变噪声（连做三版全被否的教训）。

---

## 四、本轮（收尾轮）改动

1. 幻彩渐变**固定**用流光封面（`THEME_IRIDESCENT -> 5`），删掉了 `pickIridescentCoverStyle()`。
2. **删除曜石玄夜主题**：移出 `VISIBLE_THEMES`；`migrateHiddenTheme` 里 `THEME_FC/THEME_OLED -> THEME_LINEAR`。
3. 流光封面方向**锁定 A**（12 颗小软斑，主色是主角）；B/C 已实现在 `buildFlowBlobs()` 里，改 `style 5` 分支的 `variant = 0` 为 1/2 即可切换。
4. 删除全部临时调试代码（长按调色板图标切方向、`flow_preview.xml` 偏好、`flowVariant`）。

### 流光封面参数（`MainActivity.kt` 顶部 + `buildFlowBlobs()`）
- **当前实现 = 方向 2（两团光斑）**：主焦点大斑（半径 0.44w，轨道 y 全程 0.10~0.35）
  + 右上角副斑（半径 0.24w）。颜色 `FlowBlobColors` **只有 2 色**：
  暖橙核 `#FF775F` + 品红 `#C03DEA`。
  > ⚠️ 旧版本文档写的「12 颗小软斑 / 内层彩虹四色（橙蓝品红黄）/ 方向 A」**已不成立**，
  > 现在的实现是方向 2 + 双色。别照着旧记录去"修复"成 12 颗。
- 标题用色固定深紫黑 `#2E1B38`（封面永远亮底，不随主题明暗）。
- 光斑走 3 关键帧闭合巡游，`FLOW_CYCLE_SECONDS = 60f`；动画相位只在 `drawBehind` 读
  `State.value`（每帧重绘、不重组）。

---

## 五、本项目已完成的打磨（全部用户确认过）

- 登录页主题自适应（原来固定深色）、主题选择器左右滑动轮播（即时生效）
- 底部导航栏白边根治（跨机型）、底栏瘦身（90.2dp→68.4dp）
- 正宗 iOS 转场（整屏位移 + 视差，`pathInterpolator(0.32,0.72,0,1)` 350ms）
- 退出登录闪烁修复（去掉 `loggedIn=false` 先置空）
- 封面顶部装饰全部清零（Linear / 莫兰迪 / 微网格 / 暗色玻璃）
- 导航栏主题同步（`S1SystemBarSync`）

---

## 六、待办（尚未做）

- ~~`UX_BACKLOG.md`（9 条）~~ → **已全部落地**（P0 三处死入口做成了
  `DraftListActivity` / `ReadHistoryActivity` / `QuickActionsSheet`；P1 骨架屏、
  Snackbar 撤销、已读态、长按 Peek 均已实现）。
- ~~`BUG_REPORT.md` 5 个 🔴~~ → **已全部修复**。文档已过期，勿按它排期。
- **`MainActivity.kt` 仍 2670 行**（流光绘制 + 卡片 + 首页 + 底栏全在一个文件）。
  其中流光绘制嵌在 `XhsPostCard` 的 `drawBehind` 里，抽离需重写绘制逻辑，
  **属于 UI 改动，必须先出预览让用户确认再动**（2026-10-13 夜间未做，原因见下）。
- **零 instrumentation 测试**（`app/src/` 原先只有 `main`）。
  现已引入 `app/src/test/` JVM 单测保护分页算法，见下节。
- 上架相关：无隐私政策、release 签名已移出源码但**尚未配置 CI/发版流程**。

---

## 六之二、2026-10-13 夜间改动（bug 修复 + 工程化）

### 「翻页只能到 n-1 页」——根因与修法
**根因**：`PageAdapter.getItemCount()` 返回 `maxPage`，而 `maxPage` 只在**真正拿到数据**时
才 +1。ViewPager2 的可滑动范围由 itemCount 决定，所以**最后一页的位置压根不存在**，
用户物理上滑不过去，预加载链一断就永远卡在 n-1 页。
类注释里写了「尚未加载到的页码先渲染成占位」，但这个占位从来没被创建过。

**修法**：
1. `PageAdapter.reserveUpTo(totalPages)` —— 总页数算出后先把位置预留出来（空槽，
   由既有的 `onBindViewHolder` 占位分支 + `onPageMissing` 惰性拉取）。
2. **`addPage()` 已删除**。它是「写进 `maxPage+1`」，预留槽位后会写错页码，
   是这次 bug 的一部分。改用显式 `setPage(page, posts)`。
3. `onPageMissing` 加 `pageInRange` 守卫（预留槽位后滑过界会回调到不存在的页码）。
4. 顺带修 **BUG-8**：LRU 淘汰跳过 `maxPage`，否则末页内容被摘成空槽。

> ⚠️ **教训**：动 `PageAdapter` 的任何"自动页码"逻辑前，先确认
> `maxPage` 是否已被 `reserveUpTo` 提前推到总页数。

### 分页算法已抽成纯函数 + 单测
`util/Paging.kt`：`totalPages()` / `locateFloor()` / `pageInRange()` / `morePagesToLoad()`。
`ThreadDetailActivity` 全部改调它，**口径只有一处**。
`app/src/test/java/com/example/s1client/util/PagingTest.kt` 15 个用例覆盖
整除/余数/非法 perPage/回复数缺失/末页可达性。跑 `:app:testDebugUnitTest`。

### 签名口令移出源码
`app/build.gradle.kts` 原本把 `storePassword` 明文写着，且有**两个 `buildTypes` 块**
（后者覆盖前者）。现在合并为单个块，口令从 `local.properties` 读
（`s1.storePassword` 等，该文件已加进新建的 `.gitignore`）。缺失时 release 不套签名，
不会悄悄用弱口令打包。

### Cookie 改为加密存储
`util/SecureStore.kt`：Android Keystore + AES-GCM，**零新增依赖**（没引
androidx.security，避免夜间拉依赖失败）。`RetrofitClient` 存取 Cookie 走它，
旧明文数据会自动降级读取并在下次写入时升级为密文。
解密失败（改锁屏密码导致 Keystore 失效等）会丢弃该项 → 表现为需重新登录，
不会崩溃。

---

## 七、关键文件地图

| 文件 | 职责 |
|---|---|
| `MainActivity.kt`（约 2670 行） | 首页瀑布流、卡片 `XhsPostCard`、底栏、顶栏、流光封面绘制 |
| `util/Paging.kt` | **分页纯算法**（总页数/楼层换算/范围判断），有单测，改分页先看它 |
| `util/SecureStore.kt` | Keystore + AES-GCM 加密存储（当前只用于 Cookie） |
| `ui/PageAdapter.kt` | ViewPager2 适配器；**注意 `reserveUpTo` 与 `maxPage` 的关系** |
| `ui/theme/S1Theme.kt` | 调色板 `S1Colors`、`s1PaletteFor`、4 套主题色板 |
| `util/ThemeManager.kt` | 主题索引、封面风格、迁移、可观察状态 |
| `util/ThemeEdge.kt` | edge-to-edge、导航栏配色 `applyS1NavigationBarColor` |
| `ui/S1SystemBarSync.kt` | 主题变化 → 系统栏同步 |
| `BaseActivity.kt` | View 体系页面的基类（edge-to-edge + 主题） |
| 各 `*Activity.kt` | 帖子详情 / 登录 / 我的 / 通知 / 私信 / 搜索 / 发帖 等 |

历史备份：`_backup/`（linear_v1 / round3 / round4）。APK 归档按 `S1Client-vN-说明.apk` 命名放在项目根目录。
