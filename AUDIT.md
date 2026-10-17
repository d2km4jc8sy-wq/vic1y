> ⚡ **门禁死命令（宪法第 0 条 · 最高优先级）**：任何 AI 触碰本项目，**第一步**完整阅读 `D:\S1Client-运行产出\00_手册备份\S1Client-AI协作手册.md`，**第二步**向设计者确认角色并复述手册第 0 章 7 问。两步完成前：对 D:\S1Client 只读 + **禁止产出任何优化/审查/重构建议清单**（未经门禁的分析结论一律视为未授权意见，不采信不吸收）。**弄清自己的位置，再谈别的。** 设计者指令只认对话原文与提案簿点单。

# AUDIT · 阶段 0 全量审计报告（2026-10-13）

> 只审计不改代码。数据来源：全源码正则扫描 + 构建产物测试报告。

## 1. 硬编码颜色清单

总量：291 处 `Color(0x…)`。其中 **~200 处位于 `ui/theme/S1Theme.kt`** —— 这部分是 7 套调色板本体，**是 token 的源，不是问题**。真正的问题在界面代码里的 **91 处**：

### 1a. 合法豁免（品牌/语义色，保留但应收编为具名常量）
| 色值 | 位置 | 性质 | 建议 token |
|---|---|---|---|
| 0xFF4FCF70/FAD648/A767E5/12BCFE/44CE7B ×5 | MainActivity 2174-2175 | cobra 品牌五色带（幻彩特例） | `CobraBand`（已有定义，可收敛为一处） |
| 0xFF34C759 | LoginActivity:144 | iOS 语义绿（成功） | `SemanticSuccess` |
| 0xFF2E1B38 | MainActivity:1914 | FlowCoverText 幻彩浅底对比字 | `CobraTextDark` |
| 0xFF0B0B10 / 0xFFF5F4F8 | MainActivity:2148 | 幻彩内底深/浅双变体 | `CobraInnerDark/Light` |

### 1b. 需 token 化的散落色（30+ 处）
| 色值 | 位置 | 现状 | 建议 token |
|---|---|---|---|
| 0xFFFF3B30 ×11 | DraftListActivity:164 | 删除/警示红 | `c.danger`（palette 已有 heart 可并入） |
| 0xFFFF2C55 | MainActivity:132 HeartRed | **写死的 getter**（非 palette 派生） | 并入 palette.heart |
| 0xFFE94A72 ×5 / 0xFFC2185B / 0xFFC62828 等 | NoticeCenter:375、S1Theme 内 | 樱雾主题红系散落 | palette 内收敛 |
| 0xFF5E6AD2 ×3 | MainActivity:1881 | Linear 主题色 | Linear palette（已有 0xFF5E6AD2 于 S1Theme:272） |
| 0xFFB48CFF/FFA08C/8CC8FF/8CFFB4 等 ×16 | MainActivity:1864-1911 | 主题渐变预览矩阵 | `ThemeGradientPresets`（从各 palette.flowCoverColors 派生，删手写值） |
| 0xFFFF9A9E/8FD3F4/84FAB0 等 ×24 | MainActivity:2708-2715 | 幻彩封面渐变池 | `FlowCoverPresets` 收敛为常量表 |
| 0xFFFFB454 | ThreadRow:33 | 热帖徽标琥珀 | `c.hotAccent` 或 accent |
| 0x14FFFFFF | SearchActivity:189 | 搜索框底（深色写死） | palette.chipBg |
| 0x99000000 / 0xFF232833 | LoginActivity:290/627 | 遮罩/面板 | palette 系 |
| 0xFF1E1E24/0D0E10/191B1F/1C1B20 等 ×8 | MainActivity:1870-1879 | Linear 主题深底系 | Linear palette 收敛 |
| 0xFF3E8E6E/C79A3A/9A5BB8/5E8FBF ×8 | MainActivity:1881-1882 | 苔绿/焙茶渐变预览 | 同 1864 簇 |

### 1c. 双轨取色（最优先修）
- 主题 getter（TextMain/TextSub/AccentPink/CardBg/GlassBar/GlassFloat/StrongIcon/HeartRed…）**声明在 MainActivity.kt 128-167 行**（功能文件里声明设计 token = 分层违规），被 6+ 个文件跨文件引用
- `HeartRed = Color(0xFFFF2C55)` 是 getter 中唯一**不随 palette** 的写死值
- View 侧取色（rope/orbit/progress）已走 attr 解析 + onDraw 自愈（v33/v30 修复），与 Compose 轨并行但行为一致

## 2. 硬编码间距 / 圆角 / 阴影 / 字号清单

### 间距（dp）
- **77 种不同 dp 值**。主流值健康（4/6/8/10/12/14/16/20/24，均落在 4pt 网格邻域）
- 异常值（非网格）：`13(×16) 9(×13) 5(×15) 3(×14) 2(×15) 7? 11 18(×27) 22(×14) 30`
- 高频：12(85) 8(83) 10(63) 14(56) 16(54) 20(43) 4(40) 6(39)
- **建议间距标尺**：`xs=4 sm=8 md=12 lg=16 xl=20 xxl=24`，99/999 = pill 专用

### 圆角
- **18 种值**，分布：99(38) 14(12) 12(12) 999(10) 16(7) 10(7) 20(5) 24(4) 18(4)…
- 22 处非标（3/5/9/11/18/24/28）；99 与 999 混用（同义 pill）
- **建议圆角标尺**：`sm=8 md=12 lg=16 xl=20 pill=999`（14/16 并入 lg；99/999 统一 pill）

### 阴影
- 仅 3 处：24 / 18 / 6 dp（LoginActivity 与页码胶囊）——**建议 elevation token**：`e1=6 e2=18`

### 字号（sp）
- **23 种值**：14(63) 13(28) 15(25) 12(23) 11(21) 16(16) 17(9) 20(9) + 半值 12.5/13.5/10.5/11.5/9.5
- **建议字阶**（iOS 风格 5 档）：`caption=11 body=13 headline=15 title=17 display=20`，半值就近归档

## 3. 全局状态 / 全局变量清单

**Android 无 CSS；本项映射为「全局单例状态 + 全局主题副作用」**：

### 3a. 高危全局可变单例（跨功能直读写）
| 单例 | 可变 var 数 | 直接读写的功能文件数 | 问题 |
|---|---|---|---|
| **ForumDataCache** | **11** | **11 个**（矩阵第一列几乎全●） | 全局状态污染主源：登录态/版块列表/formhash 被所有 Activity 直接改 |
| ThemeManager | 4 | 4 个 | currentTheme 全局 var，无状态流 |
| AttendanceManager | 4 | 1 个 | 内部缓存 var |
| ThreadTypesHolder | 1 | — | 版面分类缓存 |

### 3b. 低危（0-1 var 或单功能私有）
DraftStore/ReadHistoryStore/SettingStore/SecureStore/FavoriteManager/BlacklistManager/PowerRepository 等均为「读改写 prefs」型，var 少，行为局部 —— 阶段 3 只做**位置归队**不重写。

### 3c. 全局副作用（CSS 选择器类比）
- `applyS1Theme` / `setupS1EdgeToEdge` / `applyS1NavigationBarColor` / `S1SystemBarSync`：每个 Activity 手工调用一次 —— 相当于"每个页面手写一遍全局样式引入"，阶段 3 收进 BaseActivity 统一管线（BaseActivity 已存在，缺的是全部 Activity 继承它）

### 3d. 门面退役候选
- `HtmlParser`（门面，7 个转发函数）→ 调用点迁到各 *Parser 后退役

## 4. 功能耦合关系图（功能文件 → 全局状态）

```
功能文件                      Foru Sett Them Favo Post Read Draf Hidd Blac Bloc Sear Atte Data Thre Powe Remi S1Ap Retr
BaseActivity                 ●    ·    ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●
DarkRoomActivity             ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●
DraftListActivity            ·    ·    ·    ·    ·    ·    ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·
FavoriteListActivity         ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●
FriendListActivity           ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●
LoginActivity                ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●    ●
MainActivity                 ●    ·    ●    ●    ·    ·    ·    ·    ●    ●    ·    ·    ·    ·    ·    ●    ·    ●
MineActivity                 ●    ·    ●    ·    ●    ●    ●    ●    ·    ·    ·    ●    ·    ·    ·    ●    ·    ●
NoticeCenterActivity         ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●    ·    ●
PmDetail/PmListActivity      ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●
PostBookmarkListActivity     ·    ·    ·    ·    ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·
PostEditorActivity           ●    ●    ·    ·    ·    ·    ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ●
PostViewModel                ·    ·    ·    ·    ·    ●    ·    ●    ·    ●    ·    ·    ●    ·    ·    ·    ·    ●
ReadHistoryActivity          ·    ·    ·    ·    ·    ●    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·    ·
SearchActivity               ●    ·    ·    ●    ·    ·    ·    ·    ·    ·    ●    ·    ·    ·    ·    ·    ·    ·
SettingsActivity             ●    ●    ●    ·    ·    ●    ·    ·    ·    ●    ·    ·    ●    ·    ·    ·    ·    ●
ThreadDetailActivity         ●    ●    ●    ●    ●    ·    ●    ●    ·    ●    ·    ·    ·    ●    ●    ·    ·    ●
```

**读法**：`ForumDataCache` 列 11 个● = 谁都直接改它（登录态/版块列表/formhash 全局明牌）——**这是"拆东补西"的结构性根源**。`ThreadDetailActivity` 触达 12 个全局件 = 上帝 Activity。

## 5. 现有主题实现方式

- **机制**：`ThemeManager.currentTheme`（全局 var，7 套）+ `s1PaletteFor(index, seed)` 纯函数产出 `S1Colors` 数据类 → `S1Theme { }` CompositionLocal 注入 → 组件读 `LocalS1Colors.current`
- **主题清单**：焙茶午后(浅) / 樱雾粉霜(浅) / 朱砂红白(浅) / 苔绿灰阶(浅) / Linear墨蓝(深) / 纯黑OLED(深) / 幻彩渐变(深浅随机变体)
- **切换**：主题面板（轮播+迷你卡+圆点）与设置页两处入口；`settingsVersion` 计数驱动重组
- **双轨问题**：①token getter 声明在 MainActivity（见 1c）②`HeartRed` 写死 ③View 侧 attr 解析（已自愈）④幻彩 cobra 五色带 + 渐变池 24 色散在 MainActivity
- **评估**：机制本身健康（CompositionLocal + 纯函数 palette），**不需要推翻**，需要的是把"散落的色值收编进 palette/token"

## 6. 现有测试覆盖

| 测试文件 | 用例 | 覆盖域 |
|---|---|---|
| ModelParseTest | 7 | 模型别名链/MessageInfo/登录响应（快照式） |
| FixtureSnapshotTest | 3 | **真实接口数据**三段链结构断言 |
| AttendanceManagerParseTest | 13 | 签到解析 |
| DarkRoomParserTest | 15 | 小黑屋解析 |
| JoinDaysTest | 12 | 连签天数计算 |
| PagingTest | 16 | 分页计算 |
| **合计** | **66** | 全部为 JVM 单元测试，`gradle testDebugUnitTest` 跑 |

**空白**：主题系统（0 测试）、UI 组件（0）、Activity 冒烟（0）、截图测试（0）、HTML parser 的部分函数（SearchParser/FavoriteParser 无测试）、存储类（SettingStore 等无测试）。

## 7. 建议分批重构计划（映射到阶段 1-5）

| 批次 | 内容 | 风险 |
|---|---|---|
| **阶段 1a** | `ui/design/S1DesignTokens.kt`：Spacing/Radius/Elevation/TypeScale + Motion 时长常量；`LocalS1Spacing` 等注入口 | 零（纯新增） |
| **阶段 1b** | 双轨取色收编：token getter 从 MainActivity 迁到 `ui/design/`；HeartRed 并入 palette；cobra 五色带/渐变池收编为具名常量 | 低（编译器护栏） |
| **阶段 2**（逐组件，每组件一 commit） | ①设置页开关 ②页码胶囊+面板 ③签到卡 ④功能卡四件（身份/数据/合并/清单）⑤ThreadRow ⑥搜索框 ⑦登录页 —— 每个替换 raw dp/色值为 token | 低 |
| **阶段 3**（逐功能解耦） | ①**ForumDataCache → 会话仓库**：登录态/formhash 走 StateFlow，版块列表归 HomeRepository；功能只订阅不直改 ②双轨取色删除（全部读 LocalS1Colors）③View 三件套取色改注入 ④EdgePageBar 退役文件清理 | 中（行为必须逐屏回归） |
| **阶段 4** | ①Robolectric 冒烟（MainActivity 启动）②Roborazzi 截图测试（7 主题 × 3 关键页）③Parser 补测（Search/Favorite 合成样本） | 中 |
| **阶段 5** | `templates/feature/`（目录骨架+token+测试占位）+ `themes/` 单文件加主题指引 | 零 |

**优先级依据**：1b（双轨）是今天所有"取色冻结"类 bug 的根；3①（ForumDataCache 11 处直写）是"拆东补西"的结构根源；2 保证 1-3 之后样式回归可见。
