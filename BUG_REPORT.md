> ⚡ **门禁死命令（宪法第 0 条 · 最高优先级）**：任何 AI 触碰本项目，**第一步**完整阅读 `D:\S1Client-运行产出\00_手册备份\S1Client-AI协作手册.md`，**第二步**向设计者确认角色并复述手册第 0 章 7 问。两步完成前：对 D:\S1Client 只读 + **禁止产出任何优化/审查/重构建议清单**（未经门禁的分析结论一律视为未授权意见，不采信不吸收）。**弄清自己的位置，再谈别的。** 设计者指令只认对话原文与提案簿点单。

# S1Client 代码缺陷审查报告

审查时间：2026-10-12
项目路径：`C:/Users/Administrator/Desktop/S1Client`
代码规模：73 个 Kotlin 文件 / 约 17,000 行
编译状态：✅ `:app:compileDebugKotlin` 通过（无语法/类型错误）

> 结论：项目**防御性整体做得不错**（BbCode 的 `coerceIn`、各 Parser 的 try-catch、
> `PostBookmarkManager` 的 Mutex、`S1App` 的崩溃追踪、`safeApiCall` 正确重抛
> `CancellationException`）。下面的问题多为**个别文件的遗漏**，其中 3 处与项目
> 整体风格明显不一致（`ZoomableImageView`、`PostAdapter.saveImage`、`ImageDownloader`）。

---

## 🔴 严重（会崩溃 / 必现风险）

### BUG-1　`ui/ZoomableImageView.kt:169-170` —— 拖动时 `!!` 空指针
> ✅ 已修复（2026-10-12）
```kotlin
val dw = drawable?.intrinsicWidth!! * v[Matrix.MSCALE_X]   // 169
val dh = drawable!!.intrinsicHeight * v[Matrix.MSCALE_Y]   // 170
```
第 169 行刚用 `?.` 判空，下一行立刻 `!!` 解包。走的是 `ACTION_MOVE` 分支，
**只要图片未加载完/加载失败时用户拖动，每帧都执行，必崩 NPE**。
修复：
```kotlin
val d = drawable ?: return true
val dw = d.intrinsicWidth * v[Matrix.MSCALE_X]
val dh = d.intrinsicHeight * v[Matrix.MSCALE_Y]
```

### BUG-2　`ui/PostAdapter.kt:736-745` —— 裸协程 + Activity 泄漏
> ✅ 已修复（2026-10-12）
```kotlin
private fun saveImage(ctx: android.content.Context, url: String) {
    S1Notify.show(ctx, "正在保存...")
    CoroutineScope(Dispatchers.IO).launch {          // 736：无父作用域
        val filename = ImageDownloader.downloadToGallery(ctx, url)
        android.os.Handler(Looper.getMainLooper()).post { ... S1Notify.show(ctx, ...) }
    }
}
```
`CoroutineScope(Dispatchers.IO)` 没有生命周期约束：Activity 销毁后协程继续跑，
持有 `ctx`（Activity）导致泄漏；完成后还会向已销毁 Activity 弹窗，
触发 `WindowManager$BadTokenException`。全项目唯一一处这种写法。
修复：改用 `lifecycleScope`（`ctx as? LifecycleOwner`），并在回调前判断
`ctx` 对应 Activity 是否 `!isFinishing && !isDestroyed`。

### BUG-3　`util/ImageDownloader.kt:37-42` —— Response 未关闭 + 死代码
> ✅ 已修复（2026-10-12）
```kotlin
val resp = RetrofitClient.httpClient.newCall(req).execute()
if (!resp.isSuccessful) return@withContext null
val body = resp.body ?: return@withContext null
val total = body.contentLength()      // 41：算完从未使用
val bytes = body.byteStream().readBytes()
onProgress(100)                       // 43：假的进度
```
`resp` 从不 `close()`，OkHttp 连接不归还连接池 → 反复保存图片会耗尽连接、
内存持续增长。`total` 是无用变量（复制粘贴残留）。
修复：
```kotlin
RetrofitClient.httpClient.newCall(req).execute().use { resp ->
    if (!resp.isSuccessful) return@withContext null
    val bytes = resp.body?.byteStream()?.readBytes() ?: return@withContext null
    ...
}
```

### BUG-4　`util/SearchManager.kt:48-49` —— Response 未关闭（同 BUG-3）
> ✅ 已修复（2026-10-12）
```kotlin
val resp = RetrofitClient.httpClient.newCall(req).execute()
val html = resp.body?.string() ?: ""
```
每次搜索泄漏一个连接。修复：`.execute().use { it.body?.string().orEmpty() }`

### BUG-5　`ThreadDetailActivity.kt:150 / 703` —— 非线程安全集合跨协程写
> ✅ 已修复（2026-10-12）
```kotlin
private val rateMap = java.util.concurrent.ConcurrentHashMap<String, RateInfo>()  // 148 ✅ 已线程安全
private val ratingFetchedPages = mutableSetOf<Int>()                             // 150 ❌ 普通 Set
...
if (page < 1 || ratingFetchedPages.contains(page)) return   // 702
ratingFetchedPages.add(page)                                // 703
```
`loadFirstPage` 与多个 `preloadPage` 各自 `lifecycleScope.launch` 并发调用，
普通 `mutableSetOf` 并发读写 → `ConcurrentModificationException` 或状态丢失
（`rateMap` 已经用了 ConcurrentHashMap，此处明显是遗漏）。
修复：`ConcurrentHashMap.newKeySet<Int>()`。

### BUG-6　`ui/UserCardDialog.kt:142` —— 弹窗关闭后仍写视图
> ✅ 已修复（2026-10-12）
```kotlin
activity.lifecycleScope.launch {          // 142：绑 Activity 而非 Dialog
    val resp = withContext(Dispatchers.IO) { RetrofitClient.api.getProfile(uid = uid) }
    cardBinding.tvCardGroup.text = ...    // 弹窗已 dismiss → 幽灵更新
    ...
    S1Notify.show(activity, ...)          // Activity 已 finish → BadTokenException
}
dialog.show()
```
`lifecycleScope` 不随 Dialog 关闭取消。用户快速关弹窗 → 网络回来后向已销毁视图写数据。
修复：`val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)`，
`dialog.setOnDismissListener { scope.cancel() }`，回调内判断 `dialog.isShowing`。

---

## 🟠 中等（逻辑错误 / 竞态 / 资源）

### BUG-7　`PostEditorActivity.kt:124-148` —— 上传状态与生命周期脱钩
> ✅ 已修复（2026-10-12）
```kotlin
val scope = rememberCoroutineScope()   // 93：随 Composable 离开组合即取消
...
uploading = true                       // 124
scope.launch {
    runCatching { ImageUploader.upload(context, uri, tokenPages) }
        .onSuccess { ... }
        .onFailure { toast(...) }
    uploading = false                  // 148：中断则永不复位
}
```
上传中用户返回 → 协程取消，`uploading` 卡在 `true`（按钮永久禁用）。
且 `toast(context)` / `S1Notify.show(context)` 在挂起恢复后用的是可能已销毁的
Activity Context。`ThreadDetailActivity.kt:122-141` 的 `replyImagePicker` 同样问题。
修复：用 `lifecycleScope`，`uploading = false` 放 `finally`，回调前判 `isDestroyed`。

### BUG-8　`ui/PageAdapter.kt:81-85` —— LRU 淘汰可能误删最大页
```kotlin
while (pageAccess.size > MAX_PAGES) {
    val victim = pageAccess.first()
    pageAccess.remove(victim)
    pages.remove(victim)            // 若 victim == maxPage → 状态与 maxPage 不一致
}
if (page > maxPage) {
    val oldMax = maxPage
    maxPage = page
    if (oldMax == 0) notifyItemInserted(page - 1)
    else notifyItemRangeInserted(oldMax, page - oldMax)   // 分界会错
}
```
`maxPage` 决定 ViewPager2 的 item 数量，被淘汰页保留为"空 item"。若 LRU 恰好
淘汰掉 `maxPage` 本身，`notifyItemRangeInserted` 的分界会错位，导致页号错乱。
当前 `MAX_PAGES=30` 不易触发，属边界隐患。修复：淘汰时跳过 `page >= maxPage`。

### BUG-9　`ThreadDetailActivity.kt:1647-1682` —— `isLoading` 共享标志竞态
```kotlin
lifecycleScope.launch {
    isLoading = true
    try {
        var guard = 0
        while (guard++ < 60) { ... }   // 最多 60 次串行网络请求
    } finally { isLoading = false }
}
```
`isLoading` 是 Activity 级共享标志，`preloadPage` / `loadFirstPage` 也在读写它。
回帖后 `jumpToNewReply` 循环期间，用户在 `OnPageChangeCallback`(189) 触发的预加载
会被 `isLoading` 门禁挡掉。弱网下 60 次串行请求会造成长时间"假死"感。
修复：用独立的 `jumpLoading` 标志，或把 guard 上限改为"根据实际总页数"。

### BUG-10　`MainActivity.kt:1967` + 3 处 —— `abs(hashCode())` 可能为负
> ✅ 已修复（2026-10-12）
```kotlin
AvatarGradients[(seed.hashCode().absoluteValue) % AvatarGradients.size]  // 1967
```
`Int.MIN_VALUE.absoluteValue` 仍是 `Int.MIN_VALUE`（负数），`% size` 得负索引 →
`IndexOutOfBoundsException`。概率极低但真实存在。同样代码见：
`PmDetailActivity.kt:142`、`PmListActivity.kt:218`、`PostAdapter.kt:800`
（`Math.abs(seed) % palettes.size`）。
修复：统一改用 `Math.floorMod(seed, size)`。

### BUG-11　`ThreadDetailActivity.kt:1397` —— `subList` 返回视图而非副本
> ✅ 已修复（2026-10-12）
```kotlin
if (posts.size > PAGE_SIZE) posts.subList(0, PAGE_SIZE) else posts
```
`subList` 返回**原列表的视图**，持有对原始 `postList` 的引用；上游若复用/修改
原列表，视图会跟着变，结构修改还会抛 `ConcurrentModificationException`。
修复：改成 `posts.take(PAGE_SIZE)`。

### BUG-12　`PostEditorActivity.kt:265` —— 用 URL 字符串判成败，脆弱
```kotlin
val jumpedToThread = resp.request.url.toString().contains("thread-")
```
Discuz 成功会跳 `thread-123-1-1.html`（含 `thread-`），但跳到列表页
`forum-xx-1.html` 成功时不含，会**误判为失败**；反之正常 URL 含该串会误判成功。
修复：结合响应体中 `#messagetext` 文本判断。

---

## 🟡 轻微（健壮性 / 规范 / 死代码）

### BUG-13　`ui/PageFooterAdapter.kt` —— 空文件死代码
整文件只剩 `// 已废弃：改用底部固定页码条` 一行注释，无 package 无声明。应删除。

### BUG-14　`ui/RopeRefreshView.kt:186-191` —— `postDelayed` 未清理
```kotlin
postDelayed({ refreshing = false; stopSpin(); animate().alpha(IDLE_ALPHA)...; busy = false }, 2000)
```
`onDetachedFromWindow` 未 `removeCallbacks`，View 移除后 Runnable 仍执行 `animate()`。
`springBack` 的 `postDelayed` 同问题。

### BUG-15　`ui/EdgePageBar.kt:197` —— `hide()` 动画回调可能丢失
```kotlin
animate().alpha(0f).setDuration(200).withEndAction {
    visibility = GONE; onDismiss?.invoke()
}.start()
```
`onDetachedFromWindow` 只 `removeCallbacksAndMessages`，未 `animate().cancel()`。
淡出动画中 detach → `withEndAction` 不执行 → `onDismiss` 丢失，调用方状态不同步。

### BUG-16　`util/ThemeManager.kt:53` —— `appContext!!` 非线程安全初始化
```kotlin
if (appContext == null) {
    appContext = context.applicationContext
    val prefs = appContext!!.getSharedPreferences(PREF, Context.MODE_PRIVATE)
```
`S1App.onCreate` 与 `BaseActivity.applyS1Theme` 可能并发进入，理论上重复初始化。
赋值即非空，`!!` 多余。修复：`val ctx = context.applicationContext; appContext = ctx; ...`

### BUG-17　`LoginActivity.kt:984` —— `first {}` 无匹配抛异常
```kotlin
SecurityQuestions.first { it.first == questionId }.second
```
当前 `questionId` 只来自 `SecurityQuestions` 内部（1156 行），是安全的；
但一旦被持久化/外部注入即 `NoSuchElementException`。
修复：`firstOrNull()?.second ?: "未设置"`。

### BUG-18　`RichEditor.kt:258-281` —— 表情索引按顺序硬对齐，易错位
```kotlin
val re = Regex("""\[[a-g]:\d{1,4}]|\uFFFC""")
var emojiIndex = 0
for (m in re.findAll(displayBody)) {
    val url = if (token == "\uFFFC") parsed.emojiUrls.getOrNull(emojiIndex++)
```
假设 `displayBody` 中 `\uFFFC` 出现顺序与 `parsed.emojiUrls` 严格一致；但
`displayBody` 已用 `ATTACH_IMG_RE.replace(previewBody, "［图片］")` 处理过（245 行），
附件占位符若影响 `\uFFFC` 数量/顺序即索引错位 → 表情错图或消失。
修复：在 `PostContentParser.parse` 阶段直接产出"占位符 → url"的有序映射。

### BUG-19　重复代码（复制粘贴残留）
- **正则重复**：`EmojiCode.kt:48` 与 `RichEditor.kt:260` 各定义了一份 `\[[a-g]:\d{1,4}]`
- **头像路径拼接重复 4 处**：`FriendListActivity.kt:122`、`UserCardDialog.kt:55`、
  `MineActivity.kt:339`、`PostAdapter.kt:786`，而 `Extensions.avatarUrl` 已存在
- 建议抽公共常量/函数。

### BUG-20　`LoginActivity.kt:794-798` —— 预取 formhash 静默吞异常
```kotlin
LaunchedEffect(Unit) {
    runCatching { withContext(Dispatchers.IO) { ...getForumIndex().variables?.formhash } }
        .getOrNull()?.let { if (it.isNotBlank()) formhash = it }
}
```
失败无任何反馈。虽 `doLogin`(844) 有兜底重取，但建议至少加日志。
另 `LoginScreen.kt:208` 在 `remember` 中同步读 SharedPreferences，属主线程 I/O。

---

## 已在历史版本修复（供参考）

| 问题 | 原位置 | 现状 |
|---|---|---|
| `IllegalArgumentException: The key must be an application-specific resource id` | `PostAdapter.kt:592`（旧版行号） | ✅ 已加 `res/values/ids.xml` 定义 `tag_post_layout_listener` |

---

## 修复优先级

1. **BUG-1**（拖动必崩）→ 2. **BUG-2**（协程泄漏+BadToken）→ 3. **BUG-3/4**（连接泄漏）
→ 4. **BUG-5**（并发集合）→ 5. **BUG-6**（弹窗生命周期）→ 6. **BUG-7**（上传状态卡死）
→ 7. **BUG-10**（负索引）→ 其余。

---

## 建议：改 Linear 风格前先清障

Linear 风格改造会大规模重写 UI 层（主题、组件、动效），若先做 UI 再修 bug，
会因行号漂移导致返工。**建议顺序：先修 🔴 6 项 + 🟠 关键 3 项 → 再动 UI**。
