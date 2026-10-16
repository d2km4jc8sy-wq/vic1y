# PROGRESS · 架构与设计系统重构

> 规则：不新增功能、不改行为；每切片 = 测试/构建 → commit → 本文件更新。
> 基线：tag `v35-baseline`（阶段 1 数据层拆分已完成：commit 89d6f8e..d1d9b1b）

## 状态板

| 阶段 | 内容 | 状态 |
|---|---|---|
| 0 | 全量审计（颜色/间距/全局状态/耦合/主题/测试/分批计划） | ✅ 完成 → AUDIT.md |
| 1 | 设计 token + 主题机制 | ✅ 完成（S1DesignTokens + S1ColorTokens） |
| 1 | 设计 token + 主题机制 | 未开始 |
| 2 | 组件逐个 token 化 | ✅ 完成（切片① MainActivity pill×5 ② SearchActivity 胶囊×4 ③ LoginActivity 胶囊×5；ThreadRow 已有 token；非标值 24dp/语义绿按审计记录保留） |
| 3 | 功能逐个解耦 | ✅ 完成（P1 ForumDataCache StateFlow 化 8d1cdd0；P2 HeartRed 统一 8d1cdd0；P4 主题入口收敛；P3 见测试） |
| 4 | 测试补齐 | ✅ 4.1 主题✅3 4.2 冒烟✅3 4.3 组件✅3（共 47 测试 0 失败）；4.4 集成测试（3-5 条核心路径）与截图测试待下轮（Roborazzi 依赖 NATIVE 图形模式，本机不可用→见 PROBLEMS P5） |
| 5 | 新功能模板 + 新主题模板 | ✅ 完成（templates/FEATURE_TEMPLATE.md + THEME_TEMPLATE.md） |
| 3 | 功能逐个解耦 | 未开始 |
| 4 | 测试补齐（冒烟/截图/组件/集成） | 未开始 |


## 日志
- 2026-10-13 20:50 阶段 0 审计启动。
- 2026-10-13 20:56 阶段 0 完成：AUDIT.md（91 处散落色 / 77 种 dp / 23 种字号 / ForumDataCache 11 文件直写 / 双轨取色实锤 / 66 测试盘点 / 六批次计划）。
- 2026-10-13 21:05 阶段 1 完成：S1DesignTokens（Spacing/Radius/Elevation 标尺）+ S1ColorTokens（21 颜色 getter 从 MainActivity 迁出，HeartRed 写死值按不改行为原则保留并标注）。构建+测试绿。

- 2026-10-13 21:22 阶段 2 完成：切片①②③全绿（pill 圆角统一 14 处；ThreadRow 既有 token 确认；登录页 24dp 卡角/语义绿按审计保留）。
- 2026-10-13 21:22 阶段 3 启动：先盘点，行为决策项入 PROBLEMS.md。
- 2026-10-13 21:29 阶段 4.1 完成：ThemePaletteTest 3 用例（校准时发现并记录两个行为事实：Linear 色带无循环约束因 coverStyle=-2；幻彩两 seed 差异在封面色板）。全项目 41 测试 0 失败。
- 2026-10-13 21:45 阶段 4.2 完成：SmokeTest 3 用例（S1App 初始化/MainActivity RESUMED/详情页带参启动）。环境事实：Robolectric NATIVE 图形模式本机不可用（RenderNode UnsatisfiedLink），必须 @GraphicsMode(LEGACY)。全项目 44 测试 0 失败。
- 2026-10-13 21:52 阶段 4.3 完成：ComponentTest 3 用例（开关双主题渲染/切换 + token 双轨验证；CompositionLocal 注入绕开 currentTheme 私有 setter）。全项目 47 测试 0 失败。
- 2026-10-13 21:55 阶段 5 完成：templates/FEATURE_TEMPLATE.md（目录骨架+5 规则+测试要求）+ templates/THEME_TEMPLATE.md（5 步骤+2 禁止）。
- PROBLEMS 追加 P5：截图测试（Roborazzi）依赖 Robolectric NATIVE 图形模式，本机不可用→环境阻塞项。
- 2026-10-13 22:30 P1-P4 全部落地：P1 ForumDataCache 内部 StateFlow（对外 API 零变化+响应式出口）；P2 HeartRed→palette.heart（三主题 heart 收敛 FF2C55）；P3 FakeAuthRepository（MockK）+AuthFlowTest 4 用例；P4 主题入口收敛到设置页、我的页主题卡→浏览历史。47+4=51 测试 0 失败。
- 2026-10-13 22:45 终验：testDebugUnitTest + assembleRelease 双绿，全项目 79 测试 0 失败。
- 遗留（可选增强，非阻塞）：4.4 深度集成测试（3-5 条跨页核心路径，可复用 FakeAuthRepository）、截图测试（P5 环境阻塞）。
- 2026-10-13 22:55 真机验收（OPPO 6a8a1c57，v36-重构验收.apk = 终验 APK）：
  ✅ 启动无崩溃（P1 StateFlow 初始化路径），首页瀑布流正常
  ✅ P4-① 我的页浏览历史卡（动态 130 条副标题），主题卡已退役
  ✅ P4-② 浏览历史页打开正常（130 条+逐条删除+清空）
  ✅ P4-③ 设置页外观组主题行保留，当前主题名「幻彩渐变」显示正确
  ✅ 主题面板打开+当前卡高亮；logcat 全程 0 FATAL
  ⚠ 轮播翻页对 adb 合成滑动手势无响应（测试工具限制；真手滑动已由用户今日多次验证，palette 层有 ThemePaletteTest 护航）
  验收截图：acceptance/01-06*.png
- 2026-10-13 23:45 MainActivity 拆解完成（4 切片全绿）：2779 行 → 679 行壳 + HomeNav(301)/HomeDialogs(756)/XhsPostCard(765)/HomeFeed(850) 四文件，同包零 import 变更，79 测试 0 失败 + release 构建绿（3aff865/4dfed24/126eeb6/3fdccf7）。
- 拆解方法沉淀：同包剪切 = 零 import 变更；单遍标记剪切（防双重删除）；切分边界会吞 @Composable 注解需回补；拆出文件顶层 private → internal（等价原文件私有语义）。
- 2026-10-14 00:50 ThreadDetailActivity 拆解完成（4 切片）：2072 行 → 723 行壳（onCreate/分页/页脚/生命周期保留类内）+ ThreadDetailMenuActions(447)/RatingActions(458)/ShareNav(417)/ReplyActions(430) 四域扩展函数文件。同包零 import；onKeyDown/onBackPressed/onResume 生命周期方法保留类内。79 测试 0 失败（13cb3d2/45e57a7/d822c93）。
- 至此两大上帝文件全部拆解完毕：MainActivity 2779→679、ThreadDetailActivity 2072→723。
- 拆解配方（View 体系 Activity 专用）：方法组→同包扩展函数；成员 private→internal（等价类内私有）；嵌套类限定名；lambda 内 this@Activity → 函数顶 val act = this；suspend 修饰词保留；override/生命周期方法不迁。
- 2026-10-14 01:05 GitHub 备份完成：40+ commit 全历史推送 github.com/d2km4jc8sy-wq/vic1y（P6 关闭；新仓 Initial commit 用 force-with-lease 覆盖，已明示）。
- 2026-10-14 01:05 项目宪法入库 CONSTITUTION.md（含机械重构豁免条款），已推送。
- 2026-10-14 01:07 HtmlParser 门面退役：调用点直连各域解析器（14 文件），util/HtmlParser.kt 删除。⚠ 事故记录：首推含一次红码提交（链式命令 grep 返回值未拦截 && 链），下一 commit 修复并推送绿码——宪法第 3 条的执行教训：push 前必须显式校验 BUILD SUCCESSFUL 再 commit。
- 2026-10-14 01:15 阶段 4.4 完成：IntegrationPathTest 5 用例（阅读历史去重置顶/楼层收藏增删/草稿恢复清除/登录登出状态机含落盘往返/formhash 会话一致性，真实 Store prefs 链）。全项目 84 测试 0 失败。行为事实记录：clearUser 不清 formhash（页面级令牌语义）。
- 路线图收尾状态：0-5 全部完成；剩余仅 P5 截图测试（环境阻塞）。
- 2026-10-14 01:55 打磨 R1（用户真机反馈 5 项）：①进度线分母 bug（数学错误，长期存在）②长按退出三态反馈 ③登出直达登录页 ④回帖跳转末页重取兜底 ⑤回帖链路异常可见化+诊断日志（第 5 项为诊断版，待用户复现取 logcat）。v38 装机验证。
- 2026-10-14 02:55 打磨 R3（用户反馈第二轮，v40 装机）：①表情面板中文合并分类 8 页（常用/萌宠/卡通/高达/装置/大鹅/经典/怀旧，*2017 短码优先）②点选表情后焦点回输入框 ③我的页数据条三卡可点击 ④外观组整组迁出设置页→我的页（含身份卡右侧 palette 快捷入口，与头像点击职责分离）⑤宪法新增第 12 条（图标美学：间隙/对齐/尺寸/居中）+ 第 13 条（单线条图标死命令）。
- 遗留 R4：表情输入框内联实时预览（BasicTextField visualTransformation + OffsetMapping 映射表，独立切片）；Issue5 编辑后回帖卡顿（待用户复现抓 S1Reply 日志）。
- 2026-10-14 03:40 打磨 R4 完成：表情实时预览条（输入框上方，短码/[img] 双支持 + 计数 + 点图撤销）；Issue5 卡顿经用户复测消失（关闭）；外观组按用户拍板冻结（版面大改前不动）。v41 装机（ae7b83a）。
- 2026-10-15 图标落地（用户指令「根据你刚才产出的图去做」）：启动图标 5 档密度(mdpi~xxxhdpi) ← 豆包风格 app icon(12-44-12)；登录吉祥物 s1_mascot.png(睁眼)/s1_mascot_shy.png(捂眼) ← 粉圆底头像(12-44-10 / 12-52-49)。纯资产替换，AAPT 资源合并通过；提交 4e1fcb2。⚠ 全量 assembleDebug 因项目既有 WIP 编译失败（EdgePageBar.kt 被删但 XhsPostCard/PostPeekDialog/ThreadDetailActivity 仍引用，与本次无关），真机验收待用户修复构建后做。原图备份 app/src/main/.icon_backup/。
- 待确认（非阻塞）：登录页吉祥物 orbSize=size*0.62f 偏小，用户曾反馈「应用内头像太小」；若要放大需改 LoginActivity 布局（UI 改动，按宪法先确认再动手）。
- 2026-10-16 20:17 （另一会话）根目录减负第一段：产物/截图/日志/预览归档 outputs 分类子目录，脚本入 tools，gitignore 补全（c86bfe5，重写前哈希 119dbe7）；宪法新增第 23 条 UI 调试先验清单（重写前 2194951）。
- 2026-10-16 21:00 根目录减负收尾（61802a7）：acceptance/_backup/_素材 归档 outputs，gitignore 补 /acceptance/ /_backup/ /_素材/。根目录现仅剩 app/ + 文档 + gradle 全家 + s1client.jks + local.properties。
- 2026-10-16 21:10 工具链：本机此前无 git，安装 MinGit 2.47.1 至 C:\Tools\MinGit 并加入用户 PATH（versionCode 自动化与后续 git 操作的前提）。发现既有事实：build.gradle 的 signingConfig 读不到 local.properties（findProperty 无 s1.* 键），历来 release 实际是「gradle 出 unsigned → zipalign + apksigner 签名」的两段式，.idsig 即 apksigner 产物；宪法第 5 条补充该流程说明。
- 2026-10-16 21:20 git 历史瘦身：先打全量备份 bundle（D:\S1Client-repo-backup-20261016.bundle，168.6MB）→ git-filter-repo 剥离 516 条产物/预览/截图路径 → .git 从 170.5MB 降至 5.8MB，历史 116→91 提交（25 个纯垃圾提交剪除）。⚠ 事故记录：剥离清单口径失误把根目录 11 个 *.md 也当垃圾剥掉（保留正则漏了文档），工作区文档随之消失；已从重写前 bundle 全量取回并重新入库（363db20），宪法第 23 条等今日新内容零丢失。教训入账：filter-repo 的 keep 白名单必须含文档与所有不可再生文件，重写后必须核对 git ls-files 与重写前一致再继续。
- 2026-10-16 21:40 切片A（659c2fe，报告 Phase1-①②③）：versionCode=git 提交数自动递增（ProcessBuilder 直调 + 回退兜底），versionName 升 1.1.0；proguard 剥离 Log.v/d（保留 w/e）+ 行号表；manifest allowBackup=false、LoginActivity exported=false；lifecycle-runtime-ktx 双声明去重。
- 2026-10-16 21:40 切片B（9dc25bc，报告 Phase1-④⑤）：详情页评分抓取与首屏并行化（4 处串行 preloadRatings 调用点改异步 loadRatings + 既有 payload 局部刷新，首屏不再被第二个整页 HTML 请求阻塞）；新增 util/EmojiAssetsIndex（表情 assets 进程内索引，消灭每表情每次绑定的主线程 assets.list，配 5 单测锚点）；长文行数测量 LruCache(512)；PageAdapter 跨页共享 RecycledViewPool(12)；crash_trace.txt 512KB 滚动上限。
- 2026-10-16 21:55 验证：testDebugUnitTest 115 用例（110+新增5）0 失败；assembleDebug + assembleRelease 全绿；合并清单实测 versionCode=91 / versionName=1.1.0；release dex 实测 Log.d/v 消息体全部剥离、Log.w/e 保留、debug 探针被 R8 整体消除（BuildConfig.DEBUG 恒假）。lintDebug 基线：22 Errors / 311 Warnings（均为既有问题——RecyclerView position 捕获与 API 级别警告，非本轮引入，登记下轮处理）。
- 2026-10-16 22:00 v104 发布包：outputs/apk/S1Client-v104-versionCode自动化+详情页性能优化.apk（6.16MB，已签名，cert CN=S1Client）+ mapping-v104-91.txt 归档。改动全部为非视觉/非行为项；评分首屏「–」占位不变、到货后 payload 原地刷新。待用户真机验收。
- 2026-10-16 22:05 待办：远程推送需新 PAT——旧凭据在本次误推（截断令牌）时被 credential reject 清除，GitHub 端仍为瘦身前旧历史；用户提供 PAT 后执行 git push -f origin main --tags 即可同步（本地 363db20 为最新）。Phase2（ThreadDetailViewModel / SessionRepository / 解析器契约测试）按报告排期待续。

- 2026-10-16 22:15 v1.1.0(91) 真机验收（OnePlus CPH2581 / 742d179c，root+su 自动化，截图存 outputs/shots/v104_*）：①装机确认 versionCode=91 / versionName=1.1.0 ②冷启动 ok、全程 logcat 零 FATAL ③首页瀑布流/五键底栏/已读态置灰/Levi''s 票据厂标/页码浮标正常 ④详情页 PS1 皮肤 + STAGE1 品牌行 + 归墟版块标签（按需加载链路实测命中）+ 评分 chip「+0/–」占位 ⑤阅读进度恢复实测：直接落到 41 楼（第 2 页），floorPid 口径工作正常，翻页往返后进度回写 ⑥第 2 页战斗力 pill（✕ 11/-23/-7/319）异步到货渲染正常，内联表情正常 ⑦独立视觉复核（审阅者）11/11 项 PASS「可验收」。记录两个非阻塞观察：进度直落第 2 页时顶栏作者名不显示（bindTopAuthor 只在首楼响应取值，v103 同，列入 Phase2 顺手修）；本机 root + su -c input 可自动化点击（推翻旧文档「无法自动化」限制，后续回归测试可用）。验收截图：outputs/shots/v104_home_01.png / v104_d_settled.png / v104_d_page2.png / v104_d_page1_back.png / v104_now.png。


- 2026-10-16 22:40 远程同步完成：用户提供新 PAT，force push main（a462f0c → d2f13f9）+ v35-baseline 标签；GitHub 现为瘦身后的干净历史；令牌已恢复进 Windows 凭据管理器（git:https://github.com）。
- 2026-10-16 23:00 阶段二第一轮（报告 7.2 的 ①③④ 三项，全量 ThreadDetailViewModel 转换留待下轮专属回归计划）：
  - 36af678 SessionRepository（data/session/）：会话字段写点全量收编——adoptLogin/refreshFormhash/refreshProfile/refreshForumTree 四方法，守卫与原调用点逐字一致；验收达成：`ForumDataCache.formhash =` 全项目仅 SessionRepository 内部命中。唯一收紧：refreshFormhash 拒写空串（原 ShareNav 无条件写会清有效缓存）。
  - 36af678 ThreadEntry（util/）：详情页起始页判定抽纯函数（guessFromFloor/correctionTarget）+ 12 边界单测，loadFirstPage 两处调用点改纯函数，口径与 s1er resolveThreadInitialPage 一致。
  - cdf7d8e 解析器契约测试：游客态真实 fixture 6 份（研究员实抓：viewthread 桌面模板 194KB 含 7 带评分楼层/13 评分行、viewratings 明细 ±1、showdarkroom JSON 47 条含裸数字键坏 JSON）+ RatingParserContractTest 5 用例 + DarkRoomParserContractTest 6 用例；上游改版时失败测试名直接指认断裂契约。⚠ 契约发现固化：桌面分支只取 td.xi1 正分单元格，负分被跳过——登记 PROBLEMS P7 待拍板。
  - 调查协作：两路研究员并行（fixture 采集 + 会话写点全图 notes_session_map.md/notes_fixtures.md）。
  - 验证：testDebugUnitTest 138 用例（115+12+11）0 失败 + assembleDebug 绿；真机冒烟：装机 Success、冷启动 ok、探针广播实测 probe done posts=40 totalPages=2 perPage=40、零 FATAL。
  - 遗留：P7 待拍板；全量 ThreadDetailViewModel 转换（下轮，含分 slices 回归计划）；142 用例基线上的 instrumentation 三路径（报告 5.5）。


- 2026-10-17 阶段二第二轮（报告 7.2-① 主项落地）：ThreadDetailViewModel 状态宿主——Activity 上 12 个跨线程/数据类字段（totalPages/perPage/loadedMaxPage/hasMore/authorId/isOnlyAuthor/threadFid/rateMap/missingPages/ratingFetchedPages/capturedPoll×2）改为同形委托属性直达 VM；@Volatile 全部消失（StateFlow + CAS 原子更新），四个 Actions 扩展文件零改动。主线程视图编排字段按职责保留在 Activity。验证：138 用例 0 失败 + assembleDebug 绿；真机装机后用户实机浏览中（详情页打开正常）。回帖/评分/投票提交链路为编译级验证，待用户日常复核。验收基线：开帖/翻页/进度恢复/探针在 VM 前一版已全绿（见上条验收记录），VM 版差异仅在状态存储位置。


- 2026-10-17 性能与遮挡修复（用户真机反馈两项）：①小米10 瀑布流左右滑掉帧——幻彩心跳每卡 60fps 改 15fps 帧控（XhsPostCard），同机同手势 gfxinfo A/B：janky 152→59（-61%）；②Levi''s 票夹常驻压卡+抢触摸——改「滚动超视口 ~8% 才现身」（HomeFeed derivedStateOf，参考绳子 ~8%），hitTest 向下外扩 180dp 收紧为 +10dp（TicketPullView）；验证截图 outputs/shots/v104_ticket_top|scrolled.png。138 用例 0 失败。用户已确认回帖/评分/投票三链路日常使用无异常。

- 2026-10-17 用户真机验收通过（本修复轮）：回帖/评分/投票三链路无异议；小米10 左右滑掉帧修复「完美闭环」；票夹「不占版面、点击触摸精准、卡片与票夹各司其职」。本验收轮关闭。
