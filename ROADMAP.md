> ⚡ **门禁死命令（宪法第 0 条 · 最高优先级）**：任何 AI 触碰本项目，**第一步**完整阅读 `D:\S1Client-运行产出\00_手册备份\S1Client-AI协作手册.md`，**第二步**向设计者确认角色并复述手册第 0 章 7 问。两步完成前：对 D:\S1Client 只读 + **禁止产出任何优化/审查/重构建议清单**（未经门禁的分析结论一律视为未授权意见，不采信不吸收）。**弄清自己的位置，再谈别的。** 设计者指令只认对话原文与提案簿点单。

# S1Client 对标 s1er：优劣势分析与改进规划

> 制定时间：2026-10-13 凌晨
> 基线：S1Client（Kotlin/Compose，约 **18324** 行 Kotlin，**3** 个测试文件 / 41 用例）
> 对照：s1er（Flutter/Dart，约 **58645** 行 Dart，**226** 个测试文件）

---

## 一、总体结论（一句话）

**我们的功能密度和交互打磨优于对标，但工程安全垫（测试、架构分层）远落后。**
s1er 的代码量是我们的 3 倍，但其中相当一部分是它在测试与防御性编程上的投入。
我们目前的风险不在"做不出来"，而在**改一次就可能炸一次**——2026-10-13 夜间这一轮
连续两个线上事故（进帖子全失败）都源于这类风险，已经实证过了。

---

## 二、我们的优势

| 项 | 说明 |
|---|---|
| **视觉与交互打磨** | 小红书式瀑布流、流光封面、四套主题、贴边页码滚轮（`EdgePageBar`）、底部玻璃栏、系统栏同色处理。s1er 是工具型 UI，我们的产品感更强。 |
| **帖子详情页交互** | 引用、评分、屏蔽/折叠（作者 / 关键词 / 单楼）、多选分享、黑名单、只看作者、楼层定位。这些 s1er 详情页要么没有，要么做得更简单。 |
| **本地快照能力** | 楼层收藏（`PostBookmarkManager`）保存作者/时间/标题/原文，**原帖删除仍可看**，这点比 s1er 的收藏更实用。 |
| **阅读连续性** | 读历史记录 + 未读跳转（`ReadHistoryActivity`、`resolveOpenPage`），跨设备/跨会话体验好。 |
| **可测试纯函数化** | 已抽出 `util/Paging.kt`（分页）与 `util/JoinDays.kt`（加入天数），纯逻辑无 Android 依赖，可直接单测——这是能继续扩展测试的前提。 |

## 三、我们的劣势（按严重程度）

### 🔴 P0 — 测试密度差一个数量级
- s1er：**226** 个测试文件；我们：**3** 个文件、41 个用例。
- 后果已经出现：**夜间盲改两次都引发线上问题**。
  - `reserveUpTo` 写在 IO 线程 → 每个帖子都"加载失败"（已修）；
  - `addPage()` 在预留槽位后写错页码 → 末页不可达（已修）。
  - 这两个 bug **都不是靠测试发现的，是靠加了日志后人工读 logcat 发现的**。
- 改进方向：把纯逻辑从 Activity 里抽出来补测试（见 P0-1）。

### 🔴 P0 — 单文件过大 / 线程边界无约束
- `MainActivity.kt` **约 2700 行**；`ThreadDetailActivity.kt` **约 2000 行**。
- `ThreadDetailActivity` 里 6 处 `fetchOnePage` 调用、协程切换手写 `withContext`，
  **没有任何机制阻止"在 IO 线程碰 View"**——这就是昨晚 bug 的结构性原因。
- s1er 用 Riverpod 管理状态，UI 与数据获取之间有明确边界。
- 改进方向：见 P0-2、P1-1。

### 🟡 P1 — 缺失功能（s1er 有、我们没有）
| 功能 | s1er 位置 | 我们的状态 |
|---|---|---|
| 小黑屋（公开处罚列表） | `dark_room_screen.dart` | 无 |
| 已屏蔽版块管理 | `hidden_forums_screen.dart` | 无（有全局屏蔽但无版块级） |
| 更新日志页 | `whats_new_screen.dart` | 无 |
| 设置二级页 | `settings_screen.dart` | 已扁平化（用户偏好，见下） |

> 注：设置页我们主动做了扁平化（取消二级页），这是**产品选择而非缺失**，
> 但需确认是否丢失了任何真实能力（待逐项核对）。

### 🟡 P1 — 崩溃可观测性不足
- 我们有 `crash_trace.txt` 落盘（`S1App`），但**本次事故里它是空的**，
  最后靠 `ActivityManager` 的 `Killing...SwipeUpClean` 才确认"不是崩溃"。
- 2026-10-13 新增 debug 广播探针（见下），是目前唯一能自动化验证的手段。

### 🟡 P1 — 无 instrumentation 测试 / 无 CI
- `app/src/` 原本只有 `main`，没有 `androidTest`；UI 回归全靠人工。
- 无 CI 配置，任何改动只能靠本地编译 + 人工点。

---

## 四、改进规划（按优先级）

### P0-1：把纯逻辑从 Activity 抽出来补测试
**目标**：分页 / 楼层定位 / 加入天数 / 签到解析 / HTML 解析 / 黑名单判定 —— 全部可单测。
**做法**：
- 已完成：`Paging.kt`、`JoinDays.kt`、`AttendanceManager.parseResponse()`。
- 待办：`HtmlParser.parseFavoriteList` / `parseThreadList`、`BlacklistManager`、
  `BlockedAuthorManager`、`PostBookmarkManager`、`TimeUtil.formatSmart`。
- 每个抽出的纯函数配 8–15 个边界用例（参照 `PagingTest` 的写法）。
**验收**：核心纯逻辑单测 ≥ 60 个；`gradle :app:testDebugUnitTest` 全绿可离线跑。

### P0-2：给线程边界加防护
**目标**：杜绝"在子线程碰 View"这类 bug 再次发生。
**做法**：
1. 抽 `ThreadRepository`：所有网络请求集中在一处，**只返回数据、不碰 UI**。
2. Activity 只做 `viewModelScope.launch { repo.load(page) }`，拿到数据再 `bind()`。
3. 现有 `fetchOnePage` 的 catch 里已对该异常单独打完整栈（保留，别删）。
**验收**：代码搜索 `withContext(Dispatchers.IO)` 块内不再出现 `binding.` / `adapter.` 调用。

### P1-1：拆分超大文件
- `MainActivity.kt` → 按职责拆：`HomeFeed`、`PostCard`、`CategoryTabs`、`BottomNav`、`FlowCover`。
- `ThreadDetailActivity.kt` → 按职责拆：`ThreadPager`、`ReplySheet`、`RatingDialog`、`ShareSelect`、`Mute/Block`。
- **约束**：流光封面绘制嵌在 `XhsPostCard` 的 `drawBehind` 里，抽离需重写绘制逻辑，
  属于 UI 改动，**必须先出预览让用户确认**（用户 2026-10-13 明确要求流光"先不动"）。

### P1-2：补上缺失功能（建议顺序）
1. **小黑屋** — 论坛特色功能，s1er 有先例，实现成本低（拉 HTML + 解析列表）。
2. **已屏蔽版块** — 与现有屏蔽体系同源，可复用黑名单存储。
3. 更新日志页 — 成本最低，可作为版本说明载体。

### P1-3：把 debug 探针制度化
2026-10-13 新增的 `com.example.s1client.PROBE_THREAD` 广播（仅 debug 构建），
绕开了 MIUI 禁止 `adb input` 的限制，是目前**唯一可靠的自动化验证手段**。
- 已验证可用：`probe OK tid=2267000 page=2 耗时=106ms 楼层数=40 总页数=4`。
- 建议：扩展一组探针（签到 / 收藏 / 发帖 / 翻页），每次改动后跑一遍。

### P2-1：补 instrumentation 测试（仅关键路径）
不建议全面铺开，只测会碎的地方：帖子详情页翻页、发帖流程、登录态恢复。

### P2-2：CI + release 签名
- 当前 release 签名口令在 `local.properties`，无 CI 时需手工分发。
- 建议：先跑通本地 release 构建，再考虑 GitHub Actions。

---

## 五、待用户确认的事项

1. **设置扁平化是否丢能力**：需要逐项核对 s1er `settings_screen` 与我们的齿轮菜单。
2. **是否补小黑屋**：论坛特色，用户取向决定。
3. **流光封面最终形态**：用户明确"12 颗不是想要的效果"、"先不动"，
   需要重新出一版方案再定（当前实现是方向 2 两团光斑）。
4. **个人资料修改**：头衔 / 签名 / 头像 / 密码安全，Discuz 走
   `home.php?mod=spacecp&ac=profile` 表单流程，改密需二次验证、头像需上传，
   s1er 无实现可参照，需真机抓表单字段后才能写对。