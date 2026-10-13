# 新功能模板

## 目录骨架（新功能一律按此建目录）
```
app/src/main/java/com/example/s1client/feature_<名字>/
├── <功能>State.kt        # 状态定义 +（如需）StateFlow
├── <功能>Store.kt        # 持久化（SharedPreferences 封装，命名对齐 *Store）
├── <功能>Screen.kt       # Compose UI（只读 token 与状态，不直接改别人的状态）
└── <功能>导航/入口        # 由宿主 Activity 持有，不在功能内 startActivity 别人的页面
app/src/test/java/com/example/s1client/feature_<名字>/
└── <功能>Test.kt         # 至少：状态流转 1 条 + 解析/计算 1 条
```

## 规则（来自 AUDIT/今天所有教训）
1. **颜色一律走 token**：`ui/design/S1ColorTokens`（TextMain/TextSub/AccentPink/Glass* 系），
   禁止 `Color(0x…)` 散落（品牌色例外需具名常量并登记 AUDIT）。
2. **间距/圆角走标尺**：`ui/design/S1DesignTokens`（S1Spacing/S1Radius/S1Elevation）。
   非标值（如 14dp）允许，但要在 PR 说明。
3. **不直接改别的功能的变量**：跨功能数据走对方的 Store API 或统一 StateFlow。
4. **退出路径**：每个可交互元素要有明确的取消/关闭方式（面板配遮罩、弹层配返回）。
5. **单一职责**：一个控件只干一件事（案例：绳子=刷新+回顶+进度环三合一而非新增第四个浮层）。

## 测试要求
- 纯逻辑直接 JUnit；Compose UI 用 ComponentTest 的模式（createComposeRule + LEGACY 图形模式）；
- 触及解析的：先抓真实响应存 `app/src/test/resources/fixtures/`（游客可读页直接 curl；
  登录页走 BODY 日志路线，见 PROBLEMS P3）。
