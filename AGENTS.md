# S1Client

Stage1st 论坛第三方 Android 客户端。Kotlin + Jetpack Compose + View 混合。

## 怎么跑

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

USB 常连，改完直接装。签名冲突时先 `adb uninstall com.example.s1client` 再装。

## 五条硬规则

**1. 改用户看得见的界面之前，先说一句「你会看到什么不同」，然后停下等答复。**

用非技术语言，不解释实现。沉默、「嗯」「哦」「随便」都不算同意，
只有「改」「行」「可以」「你决定」才算。

判断标准：用户打开 App 会不会看出来不一样？会 → 先问；不会 → 直接做。

**2. 界面小事不用逐步问，直接构建 + 安装，自己看一眼，再一句话交付。**

间距、配色、文案、动效打磨属于这一类。「要不要这么改」要问，
「要不要装」不用问。装完说清楚改了什么，让用户验收。

**3. 同一件事改三轮还没达到预期，停下来。**

先出 HTML 预览，或者说明这个效果在 Android 上的标准做法，让用户在两条路里选。
不许第四轮继续盲改。

**4. 构建不过不许装。涉及界面必须真机验证，不许凭代码推断「应该没问题」。**

**5. 不许修改 `s1client.jks`、`local.properties`；不许 `git push --force`。**

## 需要时再查

| 文件 | 内容 |
|---|---|
| `CONSTITUTION.md` | 设计规范：配色、间距、图标、票据体系、调试先验 |
| `UX_BACKLOG.md` | 交互待办 |
| `PROGRESS.md` | 架构与修复历史 |

以上三个都是查资料用的，不需要开工前读。
