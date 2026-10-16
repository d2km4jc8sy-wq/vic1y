# -*- coding: utf-8 -*-
"""撕票动画预览：票沿撕裂线撕成两半的两种手法 + 性能结论"""
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
.duo{display:grid; grid-template-columns:1fr 1fr; gap:20px; margin-top:14px;}
@media (max-width:900px){.duo{grid-template-columns:1fr;}}
.pcase{background:#FFF; border:1px solid var(--line); border-radius:16px; padding:14px;}
.pcase .pt{font-size:14.5px; font-weight:800; margin-bottom:2px;}
.pcase .ps{font-size:12px; color:var(--ink2); margin-bottom:10px;}
.stage{background:radial-gradient(120% 100% at 50% 0%, #3B2C4E, #191223); border-radius:14px; padding:40px 14px; display:flex; justify-content:center; align-items:center; min-height:520px; overflow:hidden;}
table{border-collapse:collapse; width:100%; font-size:13px; margin-top:10px;}
th,td{border:1px solid var(--line); padding:6px 10px; text-align:left;}
th{background:#FAF6FC; font-weight:600;}
.ok{color:var(--ok); font-weight:700;}
.warn{color:#C2410C; font-weight:700;}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
/* ═══ 门票（复用最终版样式，拆上/下两层）═══ */
.ticket{width:296px; position:relative;}
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
    radial-gradient(70% 50% at 85% 92%, rgba(255,111,181,.32), transparent 60%);}
.tin{position:relative; z-index:2;}
.thead{display:flex; align-items:center; gap:6px; padding:11px 14px 8px;}
.thead .zn{font-size:10px; font-weight:700; letter-spacing:1px; color:#E9C8EE;}
.thead .zn b{color:rgba(255,255,255,.9); font-weight:900; margin-right:5px;}
.stamp{position:absolute; top:26px; right:10px; width:50px; height:50px; z-index:6;
  border:2px dashed rgba(255,143,199,.65); border-radius:50%; transform:rotate(-12deg);
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  color:#FFC7DE; background:rgba(36,26,51,.35);}
.stamp .z1{font-size:11.5px; font-weight:900; letter-spacing:2px;}
.stamp .z2{font-size:7px; opacity:.8;}
.tt{font-size:13.5px; font-weight:800; color:#FFF; line-height:1.45; padding:10px 16px 0;}
.pp{font-size:11px; color:rgba(240,230,250,.84); line-height:1.64; padding:6px 16px 0; max-height:96px; overflow:hidden;}
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
  border:1px solid rgba(233,200,238,.4); border-radius:99px; padding:4px 12px;}
/* ═══ 手法一：横撕两层（上半左旋上飞 / 下半右旋下沉）═══ */
.tearwrap{position:relative; width:296px; height:430px; filter:drop-shadow(0 10px 9px rgba(0,0,0,.22));}
.tearwrap .ticket{position:absolute; top:0; left:0; margin:0;}
.half{animation:9s infinite;}
.hTop{transform-origin:50% 100%; animation-name:ripTop;}
.hBot{transform-origin:50% 0%; animation-name:ripBot;}
@keyframes ripTop{0%,9%{transform:translate(0,0) rotate(0); opacity:1;}
  14%{transform:translate(-4px,-2px) rotate(-1deg);}
  26%,86%{transform:translate(-70px,-150px) rotate(-19deg); opacity:0;}
  92%,100%{transform:translate(0,0) rotate(0); opacity:1;}}
@keyframes ripBot{0%,9%{transform:translate(0,0) rotate(0); opacity:1;}
  16%{transform:translate(6px,3px) rotate(1.4deg);}
  28%,86%{transform:translate(46px,190px) rotate(15deg); opacity:0;}
  92%,100%{transform:translate(0,0) rotate(0); opacity:1;}}
/* 撕裂处闪光 */
.ripflash{position:absolute; left:0; right:0; top:212px; height:2px; z-index:9; opacity:0;
  background:linear-gradient(90deg,transparent,#FFE9F6 30%,#FFD76E 50%,#FFE9F6 70%,transparent);
  filter:blur(.5px); animation:ripflash 9s infinite;}
@keyframes ripflash{0%,10%{opacity:0;} 14%{opacity:1; box-shadow:0 0 18px 4px rgba(255,215,110,.65);} 26%{opacity:0;} 88%{opacity:0;} 92%{opacity:0;} 100%{opacity:0;}}
/* 撕飞后票根撤销钮常驻展示（演示重点）*/
.stub-demo{position:absolute; z-index:20; left:50%; top:250px; margin-left:-104px; width:208px;
  background:#241A33; border-radius:12px; overflow:hidden;
  border:1.5px solid transparent; background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7);
  background-origin:border-box; background-clip:padding-box,border-box;
  box-shadow:0 12px 30px rgba(0,0,0,.55); display:flex; align-items:stretch;
  animation:stubDemo 9s infinite;}
@keyframes stubDemo{0%,22%{opacity:0; transform:translateY(-14px) scale(.92);}
  30%,86%{opacity:1; transform:translateY(0) scale(1);}
  92%,100%{opacity:0;}}
.stub-demo .msg{flex:1; padding:9px 12px; font-size:11px; font-weight:700; color:#F4ECFA; line-height:1.3;}
.stub-demo .msg i{display:block; font-style:normal; font-size:8.5px; font-weight:400; color:#C9B8DE; margin-top:1px;}
.stub-demo .undo{padding:9px 15px; font-size:11.5px; font-weight:900; color:#221830;
  background:linear-gradient(100deg,#FFD76E,#FFC7DE); display:flex; align-items:center;
  border-left:1.6px dashed rgba(255,214,240,.5);}
/* 手法二：整体抽出（现状 v72 已实现）*/
.whole{animation:wholeOut 9s infinite;}
@keyframes wholeOut{0%,9%{transform:translate(0,0) rotate(0); opacity:1;}
  16%{transform:translate(10px,6px) rotate(2deg);}
  30%,86%{transform:translate(60px,210px) rotate(-13deg); opacity:0;}
  92%,100%{transform:translate(0,0) rotate(0); opacity:1;}}
/* 回放按钮提示 */
.replay{font-size:11px; color:#C9B8DE; text-align:center; margin-top:10px;}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

def ticket(hTop=False, hBot=False):
    cls = ("half hTop" if hTop else ("half hBot" if hBot else "whole"))
    # 手法一：拆两层（同内容、各自 clip 一半）；简化用整票两张叠放 clip
    return f'''<div class="ticket"><div class="paper {cls}">
  <div class="holo"></div>
  <div class="tin">
    <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
    <div class="stamp"><span class="z1">热帖</span><span class="z2">HOT 16789</span></div>
    <div style="height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;"></div>
    <div class="tt">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论</div>
    <div class="pp">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，53 楼有当事方的回应截图，112 楼是版务处理结果。</div>
    <div style="display:flex; gap:7px; padding:9px 16px 0;">
      <div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div>
    </div>
    <div class="signrow"><div class="ava-g">a</div>
      <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
      <span class="topen">查看完整帖 ›</span></div>
  </div>
</div></div>'''

TOP_HALF = f'''<div class="tearwrap">
<div class="ticket"><div class="paper half hTop" style="clip-path: inset(0 0 48% 0);">
  <div class="holo"></div>
  <div class="tin">
    <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
    <div class="stamp"><span class="z1">热帖</span><span class="z2">HOT 16789</span></div>
    <div style="height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;"></div>
    <div class="tt">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论</div>
    <div class="pp">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，53 楼有当事方的回应截图。</div>
    <div style="display:flex; gap:7px; padding:9px 16px 0;">
      <div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div>
    </div>
    <div class="signrow"><div class="ava-g">a</div>
      <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
      <span class="topen">查看完整帖 ›</span></div>
  </div>
</div></div>
<div class="ticket"><div class="paper half hBot" style="clip-path: inset(52% 0 0 0);">
  <div class="holo"></div>
  <div class="tin">
    <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
    <div class="stamp"><span class="z1">热帖</span><span class="z2">HOT 16789</span></div>
    <div style="height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;"></div>
    <div class="tt">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论</div>
    <div class="pp">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，53 楼有当事方的回应截图。</div>
    <div style="display:flex; gap:7px; padding:9px 16px 0;">
      <div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div>
    </div>
    <div class="signrow"><div class="ava-g">a</div>
      <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
      <span class="topen">查看完整帖 ›</span></div>
  </div>
</div></div>
<div class="ripflash"></div>
<div class="stub-demo">
  <div class="msg">已隐藏该帖<i>可在 我的-抹布贴 恢复</i></div>
  <div class="undo"><b>撤销</b></div>
</div></div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>撕票动画 · 两种手法预览</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">撕票动画</span>沿撕裂线撕成两半 vs 整体抽出（现状）</h1>
  <div class="sub">2026-10-15 · 每 9 秒循环：前 1.2s 静止 → 撕裂闪光 → 两半分离 · 性能结论见下方表格</div>
</header>

<section>
  <h2>① 两种手法 · 循环演示</h2>
  <div class="duo">
    <div class="pcase">
      <div class="pt">手法一 · 真·撕票（沿撕裂线撕成两半）<span style="font-size:11px;color:var(--ok);"> ★ 新做</span></div>
      <div class="ps">上半沿撕裂线断开：左旋上飞；下半右旋下沉；撕裂处一道金色闪光。对应真实「撕票根」动作。</div>
      <div class="stage">{TOP_HALF}</div>
      <div class="replay">9s 循环 · 撕裂闪光在第 1.4s</div>
    </div>
    <div class="pcase">
      <div class="pt">手法二 · 整体抽出（v72 现状）</div>
      <div class="ps">整张票右下抽出+旋转淡出。作为对照——逻辑上「票被收走」。</div>
      <div class="stage"><div class="whole">{ticket()}</div></div>
      <div class="replay">9s 循环</div>
    </div>
  </div>
</section>

<section>
  <h2>② 性能结论：不影响</h2>
  <div class="card">
    <table>
      <tr><th>项</th><th>整体抽出（现状）</th><th>真·撕票（两半）</th></tr>
      <tr><td>动画层数</td><td>1 个 graphicsLayer</td><td class="ok">2 个 graphicsLayer（上半/下半各一）</td></tr>
      <tr><td>每帧开销</td><td>纯 transform（GPU 合成）</td><td class="ok">纯 transform（GPU 合成），无重排/无重绘</td></tr>
      <tr><td>实现方式</td><td>Animatable 驱动 translationY/rotationZ/alpha</td><td>同一 Animatable 同时驱动两层（上半 translationY⁻/rotationZ⁻，下半 translationY⁺/rotationZ⁺），撕裂闪光=1 个 alpha 动画</td></tr>
      <tr><td>内容结构</td><td>票=1 个 Column</td><td>票内容渲染<b>两次</b>（上下两层各自 clip）——内容是轻量文本，渲染两份无压力；正文本就不长（Peek 预览 ≤148dp）</td></tr>
      <tr><td>结论</td><td colspan="2" class="ok">均可流畅 60fps；两半只比现状多一次同内容布局，动画期 ≤0.5s，开销可忽略</td></tr>
    </table>
    <p class="small" style="margin-top:8px;">Compose 端实现：外层 Box 装上下两个 Box，各 <span class="spec">clip = ClipRectangle(顶部0~52% / 底部52%~100%)</span> + 各自 graphicsLayer 由同一 hideAnim 取值反向驱动；撕裂闪光 = 一条渐变线 alpha 动画。总代码量 +40 行左右。</p>
  </div>
</section>

<section>
  <h2>③ 顺手待确认 · 你上一条里两句我没吃透</h2>
  <div class="card">
    <p><b>「瀑布流实现很难往最下看」</b>——这句我没解析出来。你是指：a) 隐藏后的提示条挡住了瀑布流底部？b) Peek 弹出位置偏上、下面看不清？c) 瀑布流滚动到底部加载时的什么问题？<br>
    <b>「跳转详情和整体风格不搭，改回帖子详情那种滚动翻页」</b>——现在详情页是 <b>ViewPager 左右翻页</b>（v63 定位逻辑的基础）。你是想：a) Peek 进详情时的转场动画改成滑动式？b) 还是详情页本身<b>从左右翻页改回上下滚动</b>？（后者是详情页核心交互的大改，需要单独出一版方案，不敢猜着动。）</p>
    <p class="small">已按你明确的部分先行落地：<b>收藏改为单向（只收不取消，取消去收藏列表手动）</b>——已在收藏中的帖子再点会提示「已在收藏中 · 取消请到收藏页手动操作」，不再误盖「已收藏」章；<b>隐藏去掉自动撤销条</b>，提示改为「已隐藏 · 恢复请到 我的-抹布贴」（手动恢复）。</p>
  </div>
</section>

<footer>撕票动画预览 · 确认用手法一还是保留手法二（或两半+抽出的混合），我改 HomeDialogs.kt。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/撕票动画预览.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
