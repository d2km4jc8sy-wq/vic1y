# -*- coding: utf-8 -*-
"""全息门票 · 最终版（版A 做减法后）实际效果预览"""
import base64, os

CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F6F2F9; --card:#FFF; --pink:#D6339C; --purple:#8B3FD9; --ok:#0E9F6E;}
*{box-sizing:border-box; margin:0; padding:0;}
body{background:var(--bg); color:var(--ink); font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; line-height:1.65; padding:32px 20px 80px;}
.wrap{max-width:1080px; margin:0 auto;}
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
.duo{display:grid; grid-template-columns:1fr 1fr; gap:20px; margin-top:14px; align-items:start;}
@media (max-width:900px){.duo{grid-template-columns:1fr;}}
.stage{background:radial-gradient(120% 100% at 50% 0%, #3B2C4E, #191223); border-radius:14px; padding:34px 16px; display:flex; justify-content:center; align-items:flex-start;}
.stage.light{background:linear-gradient(160deg,#F6EBF5,#F3DDEE); align-items:center; min-height:520px;}
.stagelabel{font-size:10.5px; color:var(--ink2); text-align:center; margin-top:8px;}
table{border-collapse:collapse; width:100%; font-size:13px; margin-top:10px;}
th,td{border:1px solid var(--line); padding:6px 10px; text-align:left; vertical-align:top;}
th{background:#FAF6FC; font-weight:600; white-space:nowrap;}
.cut{color:var(--ok); font-weight:700;}
.keep{color:#8B3FD9; font-weight:700;}
/* ═══════ 门票最终版 ═══════ */
.ticket{width:296px; position:relative; margin:9px 0;
  filter: drop-shadow(0 2px 1px rgba(0,0,0,.18)) drop-shadow(0 10px 9px rgba(0,0,0,.22));}
.perf{position:absolute; left:0; right:0; height:14px; z-index:5;
  background:radial-gradient(circle 6.5px at 8px 50%, #2E2340 5.5px, transparent 6.5px);
  background-size:17px 14px; background-repeat:repeat-x;}
.perf.ptop{top:-7px;} .perf.pbot{bottom:-7px;}
.paper{position:relative; background:#241A33; overflow:hidden;}
.holo{position:absolute; inset:0; opacity:.5;
  background:
    radial-gradient(90% 60% at 12% 6%, rgba(255,111,181,.55), transparent 55%),
    radial-gradient(80% 55% at 88% 18%, rgba(182,107,247,.5), transparent 58%),
    radial-gradient(90% 60% at 20% 88%, rgba(255,201,107,.28), transparent 55%),
    radial-gradient(70% 50% at 85% 92%, rgba(255,111,181,.32), transparent 60%),
    conic-gradient(from 210deg at 55% 45%,
      rgba(182,107,247,.16), rgba(255,111,181,.20), rgba(255,215,150,.14),
      rgba(182,107,247,.16), rgba(255,143,199,.20), rgba(182,107,247,.16));}
.grain{position:absolute; inset:0; opacity:.16;
  background:repeating-linear-gradient(115deg, rgba(255,255,255,.06) 0 1px, transparent 1px 3px),
             repeating-radial-gradient(circle at 30% 70%, rgba(255,255,255,.05) 0 2px, transparent 2px 9px);}
.tin{position:relative; z-index:2;}
.thead{display:flex; align-items:center; gap:6px; padding:11px 14px 8px;}
.thead .zn{font-size:10px; font-weight:700; letter-spacing:1px; color:#E9C8EE;}
.thead .zn b{color:rgba(255,255,255,.9); font-weight:900; margin-right:5px; letter-spacing:1.5px;}
.stamp{position:absolute; top:26px; right:10px; width:50px; height:50px; z-index:6;
  border:2px dashed rgba(255,143,199,.65); border-radius:50%; transform:rotate(-12deg);
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  color:#FFC7DE; background:rgba(36,26,51,.35);}
.stamp .z1{font-size:11.5px; font-weight:900; letter-spacing:2px; line-height:1.1;}
.stamp .z2{font-size:7px; opacity:.8; letter-spacing:1px;}
.tear{position:relative; height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;}
.tear i{position:absolute; top:-7px; width:14px; height:14px; border-radius:50%; background:#2E2340;}
.tear i.l{left:-8px;} .tear i.r{right:-8px;}
.tt{font-size:13.5px; font-weight:800; color:#FFF; line-height:1.45; padding:10px 16px 0;}
.pp{font-size:11px; color:rgba(240,230,250,.84); line-height:1.64; padding:6px 16px 0; max-height:118px; overflow:hidden;}
.stubstats{display:flex; align-items:center; gap:8px; padding:10px 16px 2px;}
.stats{font-size:9.5px; color:#E9C8EE;}
.stats b{color:#FFF;}
.barcode{margin-left:auto; width:34px; height:22px; opacity:.85;
  background:repeating-linear-gradient(90deg,
    #E9C8EE 0 1px, transparent 1px 3px, #E9C8EE 3px 4px, transparent 4px 6px,
    #E9C8EE 6px 9px, transparent 9px 11px, #E9C8EE 11px 12px, transparent 12px 16px,
    #E9C8EE 16px 17px, transparent 17px 20px, #E9C8EE 20px 23px, transparent 23px 26px,
    #E9C8EE 26px 27px, transparent 27px 31px, #E9C8EE 31px 34px, transparent 34px 40px);}
.acts{display:flex; gap:7px; padding:9px 16px 0;}
.tact{flex:1; text-align:center; font-size:11px; font-weight:700; color:#FFF;
  background:rgba(255,255,255,.10); border:1px solid rgba(255,214,240,.28); border-radius:9px; padding:7px 0;}
.tact.hide{color:#FFC7DE;}
.signrow{display:flex; align-items:center; gap:8px; padding:9px 16px 12px;}
.ava-g{width:30px; height:30px; border-radius:9px; flex-shrink:0;
  background:linear-gradient(135deg,#FF6FB5,#B66BF7); color:#FFF; font-weight:900; font-size:13px;
  display:flex; align-items:center; justify-content:center; border:1.5px solid rgba(255,214,240,.55);}
.signrow .sn{font-size:10.5px; font-weight:700; color:#FFF;}
.signrow .st{font-size:8.5px; color:#D8CCE6; opacity:.72;}
.topen{margin-left:auto; font-size:10px; font-weight:700; color:#E9C8EE;
  border:1px solid rgba(233,200,238,.4); border-radius:99px; padding:4px 12px; white-space:nowrap;}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

TITLE = "【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论"
BODY = ("先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，"
        "53 楼有当事方的回应截图，112 楼是版务处理结果。欢迎补充证据链，注意人身攻击直接删……")

TICKET = '''<div class="ticket">
  <div class="perf ptop"></div><div class="perf pbot"></div>
  <div class="paper"><div class="holo"></div><div class="grain"></div>
  <div class="tin">
    <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
    <div class="stamp"><span class="z1">热帖</span><span class="z2">HOT 16789</span></div>
    <div class="tear"><i class="l"></i><i class="r"></i></div>
    <div class="tt">''' + TITLE + '''</div>
    <div class="pp">''' + BODY + '''</div>
    <div class="stubstats"><div class="stats">💬 <b>16789</b> · 👁 <b>2.1w</b> · ⭐ <b>456</b></div><div class="barcode"></div></div>
    <div class="acts"><div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div></div>
    <div class="signrow">
      <div class="ava-g">a</div>
      <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
      <span class="topen">查看完整帖 ›</span>
    </div>
  </div></div>
</div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>全息门票 · 最终版</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">门票 · 最终版</span>版A 做完减法的实际效果</h1>
  <div class="sub">2026-10-15 · 四处减法已执行 · 深浅两种 App 环境各验一遍（票面恒深色，浅色主题下如一张实体票浮起）</div>
</header>

<section>
  <h2>① 减法清单</h2>
  <div class="card">
    <table>
      <tr><th>元素</th><th>处置</th><th>理由</th></tr>
      <tr><td>「座席 E7 · 已验票」</td><td class="cut">✗ 删</td><td>编的装饰文本，零信息量</td></tr>
      <tr><td>STAGE1 大字票头</td><td class="cut">✗ 压成一行小字头</td><td>占一整行只为仪式感；App 名用户天天见</td></tr>
      <tr><td>签名里的 uid</td><td class="cut">✗ 删</td><td>作者名已够定位，uid 是杂讯</td></tr>
      <tr><td>数据行「回复/查看/评分」长标签</td><td class="cut">✗ 纯图标+数字</td><td>图标已表意，文字重复</td></tr>
      <tr><td>票头渐变光距离/章缩小</td><td class="cut">✗ 微调</td><td>给标题让出第一屏焦点</td></tr>
      <tr><td>打孔边 / 撕裂线 / 全息纸纹 / 条形码 / 热帖章</td><td class="keep">✓ 保留</td><td>「这是一张票」的身份要素，一个不少</td></tr>
    </table>
  </div>
</section>

<section>
  <h2>② 实际效果（减法后 · 深色 App / 浅色 App）</h2>
  <div class="duo">
    <div>
      <div class="stage">{TICKET}</div>
      <div class="stagelabel">深色主题 · 门票融入夜色</div>
    </div>
    <div>
      <div class="stage light">{TICKET}</div>
      <div class="stagelabel">浅色主题 · 深色票面如实体票浮起（恒深色票面，两种主题一张票）</div>
    </div>
  </div>
</section>

<section>
  <h2>③ 动效/震动备注（开工时一并实现）</h2>
  <div class="card">
    <span class="small">
      弹出 260ms 缩放+淡入（弹簧轻超调）· 长按触发震动 TICK · 关闭缩回 + 原卡彩丝闪一下 ·
      收藏=金星印章 0.4s + 震动 success · 隐藏=卡片折叠 240ms + 震动 LIGHT + Snackbar 撤销 ·
      可选：票面全息光晕 8s 缓漂（默认关，静态已成立）
    </span>
  </div>
</section>

<footer>全息门票 · 最终版 · 仅预览未改码 —— 无异议即开工改 HomeDialogs.kt 装机。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/全息门票最终版.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
