# S1Client Git 速查（写给非命令行用户）

> 日常你**不需要记任何命令**——Agent 每次改完代码会自动提交。
> 这份速查只在你想自己看历史/回滚时用，或者复制给任何工具看。

## 约定（最重要）
- **每次改动一个 commit**：Agent 改完代码会自动 `git commit`，并写清楚改了什么
- **并行工具改完代码**：跟我说一声"提交一下"，我把别人的改动也收进版本库
- **出问题想回滚**：直接告诉我"回到 XXX 之前的样子"，我用 tag/commit 帮你操作
- **看现在的改动**：跟我说"看看改了什么"，我拉 diff 给你讲

## 基线
- 当前基线：tag `v35-baseline`（2026-10-13，幻彩视觉语言定稿版）
- 仓库位置：`C:/Users/Administrator/Desktop/S1Client/.git`（本地仓库，无远程）
- 签名备份：`C:/Users/Administrator/Desktop/S1Client-keys-backup/`（jks + local.properties，git 不入库防泄漏）

## 常用命令（想自己动手时）
| 想做什么 | 命令 |
|---|---|
| 看改动历史 | `git log --oneline` |
| 看某次改了什么 | `git show <commit前几位>` |
| 看当前未提交的改动 | `git status` + `git diff` |
| 放弃某个文件未提交的改动 | `git checkout -- <文件路径>` |
| 整个工作区回到基线 | `git reset --hard v35-baseline`（慎用，未提交改动会丢） |
| 打新版本标签 | `git tag v36-xxx` |

## 红线
- `.gitignore` 已排除：构建产物 / APK / 签名文件 / local.properties / preview 截图
- **不要**把 `.git/` 文件夹删掉或移动
- 若接远程仓库（GitHub 等）：先确认 `local.properties` 和 `*.jks` 仍在忽略列表里再推
