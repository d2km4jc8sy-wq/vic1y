# PROBLEMS · 行为决策待办（重构代理无权擅自改行为的事项）

> 依照协议：不确定的地方记录，不擅自改功能行为。以下每项都需要用户拍板后才能动。

## P1 · ✅ 已解决（2026-10-13 用户拍板：StateFlow 化不改对外行为）
> 已落地：内部 MutableStateFlow + 同名属性语法 + 响应式只读出口；持久化语义不变。commit 8d1cdd0。
> 原状：
- 现状：11 个可变 var 被 11 个功能文件直接读写（AUDIT.md §3a/§4），是"拆东补西"的结构根源。
- 目标形态：登录态（memberUid/Username/Credits/groupTitle/RegDate/Signature）收敛为 `SessionState`（StateFlow），版块列表/formhash 归 HomeRepository；功能只订阅不直改。
- 阻塞点：StateFlow 化会改变 Compose 重组时机（当前是"手动改 var + 手动刷 UI"），属于**行为变化**，超出"只重构不改行为"边界。
- 需要用户确认：A) 接受重组时机变化换取解耦；B) 保持现状仅做封装（私有字段+同名 var 访问器，零收益不建议）。

## P2 · ✅ 已解决（2026-10-13 用户拍板：统一到 S1ColorTokens，保持视觉不变）
> 已落地：三主题 heart 收敛 FF2C55 + HeartRed getter 走 palette.heart。commit 8d1cdd0。
> 原状：
- 现状：`HeartRed = Color(0xFFFF2C55)` 全主题写死；而热帖计数已用 `s1.heart`（palette 派生）。两套红并存。
- 统一为 palette.heart = 每主题红色略有差异（视觉行为变化）。
- 需要用户确认统一方向。

## P3 · ✅ 已解决（2026-10-13 用户拍板：MockK 伪造登录态）
> 已落地：test/fake/FakeAuthRepository.kt + AuthFlowTest 4 用例（登录态预置/端点 stub/私信解析管线）。生产代码零改动。
> 原状：
- 这三个页面需登录 Cookie 才能抓样本；Cookie 由 Keystore 加密无法出 app 进程。
- 可行路线：①BODY 日志抓包（改 debug 日志级别 → 装机 → 用户手点页面 → logcat 提取，需要一次装机+手点）；②用户浏览器另存 HTML。
- 阻塞等待用户选择路线。

## P4 · ✅ 已解决（2026-10-13 用户拍板：保留设置页入口含主题名显示；我的页换浏览历史）
> 已落地：设置页主题行保留；我的页主题卡→浏览历史快捷卡；双份死代码清除。commit 待查 git log。
> 原状：
- 两处都能改主题（审计确认功能重复但用户未提出收敛），记录备查，未动。

## P5 · ✅ 已解决（2026-10-17：CI Linux 上 Roborazzi NATIVE 冒烟截图跑通）
> 阻塞（本机 NATIVE 起不来）已由 GitHub Actions 解除：run 38933168612 在 ubuntu-latest 上以 Robolectric 4.11.1 NATIVE 模式真实渲染并落盘 ci_smoke.png（Roborazzi 1.8.0 + Compose BOM 2024.02 兼容确认；跳过闸门做成 RuleChain 外层规则，Windows 本机按 skipped 干净处理）。
> 后续增强（非阻塞）：从单点冒烟扩展为"7 主题 × 3 关键页"截图对比矩阵（依赖冒烟基线稳定后逐步加）。原状：

## P6 · GitHub 备份需要认证凭据
- 远程已配置（origin → github.com/d2km4jc8sy-wq/vic1y），但推送需要 PAT。
- 解除条件：用户提供 Personal Access Token（repo 权限）→ 配置 credential 后推送 main。


## P7 · ✅ 已解决（2026-10-16 用户拍板：方案②修复解析器，正负分全收）
> 修复：RatingParser 桌面分支改为按位置取列（[用户名, 分值, 理由]），不再依赖单元格类名——实测发现理由单元格与负分单元格同为 xg1 类，按类名区分不可行。契约测试同步更新（13 条 = 7 正 + 6 负，理由/姓名/分值全断言）。138 用例 0 失败。原状：
- 现状（2026-10-16 契约测试实测固化）：viewthread 桌面模板的评分行里，正分用 `<td class="xi1">`、负分用 `<td class="xg1">`；RatingParser.parse 的桌面分支只取 xi1，**负分条目在楼层内联评分明细里不显示**（每块 count/total 取自块头，汇总数字不受影响；「加载完整评分历史」走 viewratings 接口，负分齐全）。
- 影响：内联明细少负分行；汇总与完整历史均正确。
- 可选：①保持现状（内联只看好消息，完整历史看全量）②修 RatingParser 桌面分支同时收 xi1+xg1（契约测试同步改 13 条，PROBLEMS 闭环）。
- fixture 与断言见 RatingParserContractTest（fixture: viewthread_p1_guest.html，13 行 = 7 正 + 6 负）。
