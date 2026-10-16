# -*- coding: utf-8 -*-
"""长按 Peek 弹窗版面重设计 · 三方案对比（现状 + 经典名片 + 居中徽章名片 + 竖杠引文）"""
import base64, os

A = "preview_iridescent/assets"

def b64(name):
    mime = "jpeg" if name.endswith(".jpg") else "png"
    with open(os.path.join(A, name), "rb") as f:
        return "data:image/%s;base64," % mime + base64.b64encode(f.read()).decode()

AVA = b64("avatar.jpg")

CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F6F2F9; --card:#FFFFFF; --pink:#D6339C; --purple:#8B3FD9; --ok:#0E9F6E;}
*{box-sizing:border-box; margin:0; padding:0;}
body{background:var(--bg); color:var(--ink); font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; line-height:1.65; padding:32px 20px 80px;}
.wrap{max-width:1180px; margin:0 auto;}
header h1{font-size:24px; letter-spacing:.5px;}
header .sub{color:var(--ink2); font-size:13px; margin-top:6px;}
.badge{display:inline-block; background:linear-gradient(100deg,var(--pink),var(--purple)); color:#fff; font-size:12px; padding:2px 10px; border-radius:99px; margin-right:8px; vertical-align:2px;}
section{margin-top:32px;}
h2{font-size:18px; border-left:4px solid var(--pink); padding-left:10px; margin-bottom:12px;}
p{font-size:14px;}
.small{font-size:12.5px; color:var(--ink2);}
.card{background:var(--card); border:1px solid var(--line); border-radius:14px; padding:16px 20px; margin-top:12px;}
.note{background:linear-gradient(100deg,#FDF0F8,#F3EBFE); border:1px solid #F0D9EE;}
.note b{color:var(--pink);}
.grid2{display:grid; grid-template-columns:1fr 1fr; gap:18px; margin-top:14px;}
@media (max-width:900px){.grid2{grid-template-columns:1fr;}}
.pcase{background:#FFF; border:1px solid var(--line); border-radius:16px; padding:14px; display:flex; flex-direction:column;}
.pcase.rec{border-color:#F0C7E2; background:linear-gradient(180deg,#FFF7FC,#FFF);}
.pcase .pt{font-size:14.5px; font-weight:800; margin-bottom:2px;}
.pcase.rec .pt{color:var(--pink);}
.pcase .ps{font-size:12px; color:var(--ink2); margin-bottom:10px;}
.stage{background:linear-gradient(160deg,#3B2C4E,#241A33); border-radius:14px; padding:22px 14px; display:flex; justify-content:center; align-items:flex-start;}
.pnote{margin-top:10px;}
.pnote li{margin:4px 0 4px 2px; list-style:none; padding-left:18px; position:relative; font-size:12.5px; color:#4A4152;}
.pnote li::before{content:"·"; position:absolute; left:4px; color:var(--pink); font-weight:900;}
.pnote li b{color:#241A2E;}
/* ---------- 弹窗通用（深色幻彩板）---------- */
.dlg{width:288px; border-radius:16px; overflow:hidden; position:relative;
  border:1.5px solid transparent; background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7,#C636E7);
  background-origin:border-box; background-clip:padding-box,border-box; box-shadow:0 16px 40px rgba(0,0,0,.55);}
.dlg .dim{position:absolute; left:0; right:0; top:-22px; height:22px; text-align:center; font-size:9px; color:#B9A9CC;}
/* 现状（还原 PostPeekDialog 玻璃版）*/
.dlg.now{background-image:none; background:#2B2238; border:1px solid rgba(255,255,255,.14);}
.now .nbody{padding:14px 16px 12px;}
.now .nt{font-size:13px; font-weight:800; color:#F4ECFA; line-height:1.4;}
.now .nm{font-size:10px; color:#A08FB6; margin-top:3px;}
.now .nline{height:1px; background:rgba(255,255,255,.10); margin:10px 0;}
.now .np{font-size:11px; color:#D8CCE6; line-height:1.6; max-height:132px; overflow:hidden;}
.now .nacts{display:flex; gap:6px; margin-top:12px;}
.now .nact{flex:1; text-align:center; font-size:11.5px; font-weight:600; color:#E8DDF4; background:rgba(255,255,255,.08); border-radius:9px; padding:8px 0;}
.now .nopen{margin-top:9px; font-size:12px; font-weight:700; color:#FF9CCB;}
/* 方案一 经典名片 */
.v1 .vhead{display:flex; align-items:center; gap:9px; padding:12px 14px 9px;
  background:radial-gradient(130% 150% at 8% 0%, rgba(182,107,247,.20), transparent 62%);}
.v1 .vhead img{width:42px; height:42px; border-radius:11px; object-fit:cover; border:1.5px solid rgba(255,111,181,.5);}
.v1 .vwho .n{font-size:13px; font-weight:800; color:#F7EFFC;}
.v1 .vwho .n span{font-size:9px; font-weight:400; color:#9C8CB2; margin-left:4px;}
.v1 .vwho .s{font-size:9.5px; color:#B9A9CC; margin-top:1px;}
.v1 .vtime{margin-left:auto; font-size:9px; color:#9C8CB2; background:rgba(255,111,181,.10); border:1px solid rgba(255,111,181,.28); padding:2px 8px; border-radius:99px; white-space:nowrap;}
.v1 .vsep{height:1px; background:linear-gradient(90deg, rgba(255,111,181,.45), rgba(182,107,247,.12), transparent);}
.v1 .vbody{padding:10px 14px 0;}
.v1 .vt{font-size:13px; font-weight:800; color:#F7EFFC; line-height:1.45;}
.v1 .vp{font-size:11px; color:#C4B6D8; line-height:1.62; margin-top:6px; max-height:118px; overflow:hidden;}
.v1 .vstats{display:flex; gap:11px; padding:8px 14px; font-size:9.5px; color:#9C8CB2;}
.v1 .vstats b{color:#E8DDF4;}
.v1 .vacts{display:flex; gap:7px; padding:0 14px;}
.v1 .vact{flex:1; text-align:center; font-size:11.5px; font-weight:700; color:#E8DDF4; background:rgba(255,255,255,.09); border-radius:10px; padding:8px 0;}
.v1 .vact.hide{color:#FF8FA8;}
.v1 .vopen{margin:10px 14px 12px; text-align:center; font-size:11.5px; font-weight:800; color:#221830; background:linear-gradient(100deg,#FF6FB5,#C97BF0); border-radius:11px; padding:8px 0;}
/* 方案二 居中徽章名片 */
.v2 .vchead{text-align:center; padding:16px 14px 11px;
  background:radial-gradient(100% 130% at 50% 0%, rgba(182,107,247,.24), transparent 70%);}
.v2 .vchead img{width:56px; height:56px; border-radius:50%; object-fit:cover; border:2px solid rgba(255,111,181,.55);}
.v2 .vcn{font-size:14px; font-weight:800; color:#F7EFFC; margin-top:6px;}
.v2 .vcn span{font-size:9px; font-weight:400; color:#9C8CB2; margin-left:4px;}
.v2 .vcpills{margin-top:6px; display:flex; gap:6px; justify-content:center;}
.v2 .vcp{font-size:8.5px; padding:2px 9px; border-radius:99px; background:rgba(255,111,181,.14); color:#FF9CCB; border:1px solid rgba(255,111,181,.35);}
.v2 .vcp.solid{background:#FF6FB5; color:#221830; font-weight:700; border:none;}
.v2 .vbody{padding:10px 14px 0;}
.v2 .vt{font-size:13px; font-weight:800; color:#F7EFFC; line-height:1.45;}
.v2 .vp{font-size:11px; color:#C4B6D8; line-height:1.62; margin-top:6px; max-height:104px; overflow:hidden;}
.v2 .vacts{display:flex; gap:7px; padding:9px 14px 0;}
.v2 .vact{flex:1; text-align:center; font-size:11.5px; font-weight:700; color:#E8DDF4; background:rgba(255,255,255,.09); border-radius:10px; padding:8px 0;}
.v2 .vact.hide{color:#FF8FA8;}
.v2 .vopen{margin:10px 14px 12px; text-align:center; font-size:11.5px; font-weight:800; color:#221830; background:linear-gradient(100deg,#FF6FB5,#C97BF0); border-radius:11px; padding:8px 0;}
/* 方案三 竖杠引文 */
.v3 .vhead{display:flex; align-items:center; gap:7px; padding:11px 14px 0;}
.v3 .vhead img{width:20px; height:20px; border-radius:50%; object-fit:cover;}
.v3 .vhead .n{font-size:11px; font-weight:700; color:#E8DDF4;}
.v3 .vhead .s{font-size:9.5px; color:#9C8CB2;}
.v3 .vt{font-size:14px; font-weight:800; color:#F7EFFC; line-height:1.45; padding:8px 14px 0;}
.v3 .quote{margin:9px 14px 0; padding:2px 0 2px 11px; border-left:3px solid #FF6FB5;
  font-size:11px; color:#C4B6D8; line-height:1.62; max-height:128px; overflow:hidden;}
.v3 .vmeta{display:flex; gap:10px; padding:8px 14px 0; font-size:9.5px; color:#9C8CB2;}
.v3 .vmeta b{color:#E8DDF4;}
.v3 .vacts{display:flex; gap:7px; padding:10px 14px 0;}
.v3 .vact{flex:1; text-align:center; font-size:11.5px; font-weight:700; color:#E8DDF4; background:rgba(255,255,255,.09); border-radius:10px; padding:8px 0;}
.v3 .vact.hide{color:#FF8FA8;}
.v3 .vopen{margin:10px 14px 12px; text-align:center; font-size:11.5px; font-weight:800; color:#221830; background:linear-gradient(100deg,#FF6FB5,#C97BF0); border-radius:11px; padding:8px 0;}
/* 要素覆盖表 */
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

TITLE = "【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论"
BODY = ("先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，"
        "53 楼有当事方的回应截图，112 楼是版务处理结果。欢迎补充证据链，注意人身攻击直接删……")
ACTS = '<div class="vact">☆ 收藏</div><div class="vact">↗ 分享</div><div class="vact hide">🙈 隐藏</div>'

NOW = f'''<div class="dlg now"><div class="nbody">
  <div class="nt">{TITLE}</div>
  <div class="nm">abfk · 2024-09-22 · 16789 回复</div>
  <div class="nline"></div>
  <div class="np">{BODY}</div>
  <div class="nacts"><div class="nact">收藏</div><div class="nact">分享</div><div class="nact">隐藏</div></div>
  <div class="nopen">查看完整帖子 →</div>
</div></div>'''

V1 = f'''<div class="dlg v1">
  <div class="vhead">
    <img src="{AVA}">
    <div class="vwho"><div class="n">abfk <span>229182</span></div><div class="s">自由搏击区 · 💬 16789</div></div>
    <div class="vtime">2024-09-22</div>
  </div>
  <div class="vsep"></div>
  <div class="vbody"><div class="vt">{TITLE}</div><div class="vp">{BODY}</div></div>
  <div class="vstats"><span>💬 <b>16789</b> 回复</span><span>👁 <b>2.1w</b> 查看</span><span>⭐ <b>456</b> 评分</span></div>
  <div class="vacts">{ACTS}</div>
  <div class="vopen">查看完整帖子 ›</div>
</div>'''

V2 = f'''<div class="dlg v2">
  <div class="vchead">
    <img src="{AVA}">
    <div class="vcn">abfk <span>229182</span></div>
    <div class="vcpills"><span class="vcp solid">宝可梦go!</span><span class="vcp">2024-09-22</span><span class="vcp">💬 16789</span></div>
  </div>
  <div class="vbody"><div class="vt">{TITLE}</div><div class="vp">{BODY}</div></div>
  <div class="vacts">{ACTS}</div>
  <div class="vopen">查看完整帖子 ›</div>
</div>'''

V3 = f'''<div class="dlg v3">
  <div class="vhead"><img src="{AVA}"><span class="n">abfk</span><span class="s">· 2024-09-22 · 💬16789 · 👁2.1w · ⭐456</span></div>
  <div class="vt">{TITLE}</div>
  <div class="quote">{BODY}</div>
  <div class="vmeta"></div>
  <div class="vacts">{ACTS}</div>
  <div class="vopen">查看完整帖子 ›</div>
</div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>长按 Peek 版面重设计 · 三方案</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">Peek 弹窗改版</span>长按预览版面重设计 · 三方案对比</h1>
  <div class="sub">2026-10-15 · 功能零改动（正文现取 / 收藏分享隐藏 / 进帖全保留），只重排版面 · 幻彩深板演示 · 要素一个不少</div>
</header>

<section>
  <h2>① 现状 → 三案</h2>
  <div class="grid2">

    <div class="pcase">
      <div class="pt">现状 · PostPeekDialog</div>
      <div class="ps">问题：作者缩在元信息行里没有脸；分割线+三按钮+进帖行全部平铺，重心不明。</div>
      <div class="stage">{NOW}</div>
      <ul class="pnote">
        <li>作者只有一个名字，<b>没有头像</b> —— 「看人再进帖」的判断链断了</li>
        <li>标题→元信息→线→正文→按钮→进帖，<b>六段平铺无主次</b></li>
      </ul>
    </div>

    <div class="pcase rec">
      <div class="pt">方案一 · 经典名片 <span style="font-size:11px;color:var(--ok);">★ 你的意向</span></div>
      <div class="ps">横版名片头（头像+名+uid+时间胶囊）+ 数据行 + 操作栏，Dialog 宽度下信息密度最高。</div>
      <div class="stage">{V1}</div>
      <ul class="pnote">
        <li><b>名片头：</b>42dp 圆角方头像（点进作者空间，现成 onAvatarClick）+ 名字/uid + 右侧时间胶囊</li>
        <li><b>头部渐变光</b>：幻彩身份点缀（静态，无动画）</li>
        <li><b>数据行：</b>回复/查看/评分 —— views/likes 字段现成，空的自动隐藏</li>
        <li><b>进帖改主按钮：</b>粉紫胶囊，动作明确（原来是纯文字行）</li>
        <li>幻彩主题弹窗带<b>彩丝描边</b>（与瀑布流同一语法）；其他主题自动退玻璃边</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">方案二 · 居中徽章名片</div>
      <div class="ps">传统名片的居中构图：圆头像+名字居中+徽章胶囊行，「递名片」仪式感最强。</div>
      <div class="stage">{V2}</div>
      <ul class="pnote">
        <li><b>圆头像 56dp 居中</b> + 名字居中 + 三枚胶囊（头衔/时间/回复数）</li>
        <li>仪式感最强，但居中头<b>占掉约 150px 高度</b>，正文区被压扁</li>
        <li>适合「作者即内容」的帖子（同人/创作型）；快节奏刷流时偏重</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">方案三 · 竖杠引文</div>
      <div class="ps">非名片方向：作者压成一行小字，正文用 App 的竖杠引用语言呈现，最贴论坛气质、正文最突出。</div>
      <div class="stage">{V3}</div>
      <ul class="pnote">
        <li><b>3dp 竖杠引文线</b>（论坛引用视觉），正文就是主角</li>
        <li>作者行轻到 20dp 小头像+一行元数据（含查看/评分）</li>
        <li>最轻最快，但「名片感」为零 —— 和你的意向相反方向，放进来对比用</li>
      </ul>
    </div>

  </div>
</section>

<section>
  <h2>② 要素覆盖与代价</h2>
  <div class="card">
    <table style="width:100%; font-size:13px;">
      <tr><th>要素（全保留）</th><th>现状</th><th>① 经典名片</th><th>② 居中徽章</th><th>③ 竖杠引文</th></tr>
      <tr><td>标题 / 正文预览 / 三操作 / 进帖</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td></tr>
      <tr><td>作者头像（零接口）</td><td class="spec">✗</td><td>✓ 42dp</td><td>✓ 56dp</td><td>✓ 20dp</td></tr>
      <tr><td>查看/评分数（字段现成）</td><td>✗</td><td>✓ 数据行</td><td>胶囊里省略</td><td>✓ 并入元数据行</td></tr>
      <tr><td>正文可视高度</td><td>约 180dp</td><td>约 150dp</td><td class="spec">约 120dp（头占太高）</td><td>约 170dp</td></tr>
      <tr><td>名片仪式感</td><td>—</td><td>中</td><td class="spec">强</td><td>无</td></tr>
    </table>
    <p class="small" style="margin-top:8px;">我的意见：<b>方案一</b>。Dialog 宽度就 300dp 出头，横版名片头是信息密度和名片感的平衡点；居中式（②）好看但正文可视区被压得最矮，与「预览=快速判断」的目的有点打架；③是给「其实想要更轻」留的退路。实现上三案都是同一 Dialog 换 Column 布局，头像走 <b>avatarUrl(authorId)</b> + 字母渐变兜底（瀑布流同款），改动只在 HomeDialogs.kt 一个文件。</p>
  </div>
</section>

<footer>Peek 版面三案 · 仅预览未改码 —— 定一案（或说混搭点，比如「①的头 + ③的竖杠正文」），我动码装机。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/长按Peek改版三方案.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
