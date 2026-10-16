# -*- coding: utf-8 -*-
"""Peek 弹窗 v2 · 两个质感方案（全息门票 / 黑玻璃座舱）—— uiverse dexter-st 门票 & dorian_8749 音乐卡移植"""
import base64, os

A = "preview_iridescent/assets"

def b64(name):
    mime = "jpeg" if name.endswith(".jpg") else "png"
    with open(os.path.join(A, name), "rb") as f:
        return "data:image/%s;base64," % mime + base64.b64encode(f.read()).decode()

AVA = b64("avatar.jpg")

# 生成邮票锯齿 clip-path（上下各 12 齿，齿深 7px；高度自适应内容）
def _zig(w=296, n=12, d=7):
    pts = []
    step = w / n
    for i in range(n + 1):          # 上边：齿谷 d / 齿尖 0 交替
        pts.append((i * step, d))
        if i < n:
            pts.append(((i + .5) * step, 0))
    pts.append((w, f"calc(100% - {d}px)"))
    for i in range(n, -1, -1):      # 下边反向
        if i < n:
            pts.append(((i + .5) * step, "100%"))
        pts.append((i * step, f"calc(100% - {d}px)"))
    return "clip-path: polygon(" + ", ".join(f"{x:.1f}px {y}" for x, y in pts) + ");"

ZIG = _zig()

CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F6F2F9; --card:#FFF; --pink:#D6339C; --purple:#8B3FD9;}
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
@media (max-width:860px){.duo{grid-template-columns:1fr;}}
.pcase{background:#FFF; border:1px solid var(--line); border-radius:16px; padding:14px; display:flex; flex-direction:column;}
.pcase .pt{font-size:15px; font-weight:800; margin-bottom:2px;}
.pcase .ps{font-size:12px; color:var(--ink2); margin-bottom:10px;}
.stage{background:radial-gradient(120% 100% at 50% 0%, #3B2C4E, #191223); border-radius:14px; padding:30px 16px; display:flex; justify-content:center; align-items:flex-start;}
.pnote{margin-top:10px;}
.pnote li{margin:5px 0; list-style:none; padding-left:18px; position:relative; font-size:12.5px; color:#4A4152;}
.pnote li::before{content:"·"; position:absolute; left:4px; color:var(--pink); font-weight:900;}
.pnote li b{color:#241A2E;}
.ref{display:inline-block; font-size:10.5px; color:#8B7FA0; background:#F1EAF7; border-radius:8px; padding:1px 8px; margin-bottom:6px;}
/* ═══════════ 方案④ 全息门票（uiverse dexter-st 移植）═══════════ */
.ticket{
  width:296px; position:relative; margin:9px 0;
  filter: drop-shadow(0 2px 1px rgba(0,0,0,.18)) drop-shadow(0 10px 9px rgba(0,0,0,.22));
}
/* 打孔：上下边缘两排背景色圆点（贴片模拟，兼容性稳）*/
.perf{position:absolute; left:0; right:0; height:14px; z-index:5;
  background:radial-gradient(circle 6.5px at 8px 50%, #2E2340 5.5px, transparent 6.5px);
  background-size:17px 14px; background-repeat:repeat-x;}
.perf.ptop{top:-7px;} .perf.pbot{bottom:-7px;}
/* 票面：夜光门票 = 深紫纸底 + 紫粉金全息光（克制版，去彩虹）*/
.ticket .paper{position:relative; background:#241A33; overflow:hidden;}
.ticket .holo{position:absolute; inset:0; opacity:.5;
  background:
    radial-gradient(90% 60% at 12% 6%, rgba(255,111,181,.55), transparent 55%),
    radial-gradient(80% 55% at 88% 18%, rgba(182,107,247,.5), transparent 58%),
    radial-gradient(90% 60% at 20% 88%, rgba(255,201,107,.28), transparent 55%),
    radial-gradient(70% 50% at 85% 92%, rgba(255,111,181,.32), transparent 60%),
    conic-gradient(from 210deg at 55% 45%,
      rgba(182,107,247,.16), rgba(255,111,181,.20), rgba(255,215,150,.14),
      rgba(182,107,247,.16), rgba(255,143,199,.20), rgba(182,107,247,.16));
}
/* 纸纹（细噪点模拟：repeating 渐变）*/
.ticket .grain{position:absolute; inset:0; opacity:.16;
  background:repeating-linear-gradient(115deg, rgba(255,255,255,.06) 0 1px, transparent 1px 3px),
             repeating-radial-gradient(circle at 30% 70%, rgba(255,255,255,.05) 0 2px, transparent 2px 9px);}
.ticket .tin{position:relative; z-index:2;}
/* 票头：版区大字 */
.ticket .thead{padding:15px 16px 9px; display:flex; align-items:baseline; gap:8px;}
.ticket .thead .big{font-size:20px; font-weight:900; letter-spacing:2px; color:rgba(255,255,255,.82);
  text-shadow:0 0 14px rgba(255,111,181,.45);}
.ticket .thead .zone{font-size:10px; color:#E9C8EE; opacity:.85;}
.ticket .thead .no{margin-left:auto; font-size:9px; color:#D8CCE6; opacity:.6; font-variant-numeric:tabular-nums;}
/* 撕裂线：虚线 + 两侧半圆缺口 */
.ticket .tear{position:relative; height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;}
.ticket .tear i{position:absolute; top:-7px; width:14px; height:14px; border-radius:50%;
  background:#2E2340;}
.ticket .tear i.l{left:-8px;} .ticket .tear i.r{right:-8px;}
/* 票身 */
.ticket .who{display:flex; align-items:center; gap:9px; padding:11px 16px 0;}
.ticket .who img{width:40px; height:40px; border-radius:10px; object-fit:cover; border:1.5px solid rgba(255,214,240,.55);}
.ticket .who .n{font-size:12.5px; font-weight:800; color:#FFF;}
.ticket .who .n span{font-size:9px; font-weight:400; color:#D8CCE6; margin-left:4px; opacity:.8;}
.ticket .who .z{font-size:9px; color:#D8CCE6; opacity:.75; margin-top:1px;}
.ticket .tt{font-size:13px; font-weight:800; color:#FFF; line-height:1.45; padding:9px 16px 0;}
.ticket .pp{font-size:10.5px; color:rgba(240,230,250,.82); line-height:1.62; padding:6px 16px 0; max-height:96px; overflow:hidden;}
/* 票根：数据 + 条码 + 操作 */
.ticket .stub{display:flex; align-items:center; gap:10px; padding:11px 16px 4px;}
.ticket .stats{font-size:9.5px; color:#E9C8EE; line-height:1.7;}
.ticket .stats b{color:#FFF;}
.ticket .barcode{margin-left:auto; width:40px; height:26px; opacity:.85;
  background:repeating-linear-gradient(90deg,
    #E9C8EE 0 1px, transparent 1px 3px,
    #E9C8EE 3px 4px, transparent 4px 6px,
    #E9C8EE 6px 9px, transparent 9px 11px,
    #E9C8EE 11px 12px, transparent 12px 16px,
    #E9C8EE 16px 17px, transparent 17px 20px,
    #E9C8EE 20px 23px, transparent 23px 26px,
    #E9C8EE 26px 27px, transparent 27px 31px,
    #E9C8EE 31px 34px, transparent 34px 40px);}
.ticket .tacts{display:flex; gap:7px; padding:8px 16px 0;}
.ticket .tact{flex:1; text-align:center; font-size:11px; font-weight:700; color:#FFF;
  background:rgba(255,255,255,.10); border:1px solid rgba(255,214,240,.28); border-radius:9px; padding:7px 0;}
.ticket .tact.hide{color:#FFC7DE;}
.ticket .topen{margin:9px 16px 14px; text-align:center; font-size:11.5px; font-weight:800; color:#2A1630;
  background:linear-gradient(100deg,#FFD6EC,#E9B8FF); border-radius:10px; padding:8px 0;}
/* ═══════════ 方案⑤ 黑玻璃座舱（uiverse dorian_8749 移植）═══════════ */
.glass{
  width:296px; border-radius:19.5px; position:relative;
  padding:1.5px;
  /* 双层背景法彩丝边（已验证可靠；不用 mask-composite）*/
  background-image:linear-gradient(#0B0810,#0B0810), linear-gradient(135deg, rgba(255,111,181,.9), rgba(182,107,247,.45) 45%, rgba(255,143,199,.8));
  background-origin:border-box; background-clip:padding-box, border-box;
  box-shadow:
    0 0 6px rgba(0,0,0,.03), 0 2px 6px rgba(0,0,0,.08),
    inset 0 0 2px rgba(255,255,255,.04),
    0 0 22px rgba(255,111,181,.14);
}
.gin{border-radius:18px; overflow:hidden; padding:15px 15px 13px;
  box-shadow:
    inset 3px 3px .5px -3px rgba(255,255,255,.09),
    inset -3px -3px .5px -3px rgba(255,255,255,.5),
    inset 1px 1px 1px -.5px rgba(255,255,255,.28),
    inset 0 0 8px 4px rgba(255,255,255,.03);}
.ghead{display:flex; gap:11px; align-items:flex-start;}
.gava{position:relative; width:58px; height:58px; flex-shrink:0; border-radius:16px; overflow:hidden;}
.gava img{width:100%; height:100%; object-fit:cover;}
.gava::after{content:""; position:absolute; inset:0; border-radius:16px;
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.14);}
.gid .n{font-size:13px; font-weight:800; color:#FFF; letter-spacing:.2px;}
.gid .s{font-size:10px; color:#9C8CB2; margin-top:3px;}
.gid .s b{color:#C9B8DE; font-weight:600;}
.gtt{font-size:13.5px; font-weight:800; color:#FFF; line-height:1.45; padding:12px 0 0;}
.gpp{font-size:11px; color:#B9A9CC; line-height:1.62; padding:6px 0 0; max-height:100px; overflow:hidden;}
/* 彩丝渐变分隔（幻彩身份）*/
.gsep{height:1.5px; margin:11px 0 3px; border-radius:99px;
  background:linear-gradient(90deg, rgba(255,111,181,.0), rgba(255,111,181,.7), rgba(182,107,247,.7), rgba(255,143,199,.0));}
.gstats{display:flex; gap:12px; padding:7px 2px 2px; font-size:9.5px; color:#9C8CB2;}
.gstats b{color:#E8DDF4;}
/* 三圆钮（参考核心气质）*/
.gacts{display:flex; justify-content:center; gap:18px; padding:12px 0 4px;}
.gbtn{position:relative; width:40px; height:40px; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  font-size:15px; color:#D8CCE6;
  background:#12101A;
  box-shadow:
    inset 3px 3px .5px -3px rgba(255,255,255,.35),
    inset -3px -3px .5px -3px rgba(255,255,255,.5),
    inset 1px 1px 1px -.5px rgba(255,255,255,.22),
    0 4px 10px rgba(0,0,0,.5);}
.gbtn.hide{color:#FF9CCB;}
.gbtn .lbl{position:absolute; top:44px; font-size:8.5px; color:#8F80A3; white-space:nowrap;}
.gopen{margin:26px 2px 2px; text-align:center; font-size:11.5px; font-weight:800; color:#0B0810;
  background:linear-gradient(100deg,#FF6FB5,#C97BF0); border-radius:12px; padding:9px 0;
  box-shadow:inset 1px 1px 1px -.5px rgba(255,255,255,.55), 0 6px 16px rgba(255,111,181,.25);}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

TITLE = "【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论"
BODY = ("先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，"
        "53 楼有当事方的回应截图，112 楼是版务处理结果。欢迎补充证据链，注意人身攻击直接删……")

TICKET = f'''<div class="ticket">
  <div class="perf ptop"></div><div class="perf pbot"></div>
  <div class="paper">
  <div class="holo"></div><div class="grain"></div>
  <div class="tin">
    <div class="thead"><span class="big">STAGE1</span><span class="zone">自由搏击区</span><span class="no">No.2287665</span></div>
    <div class="tear"><i class="l"></i><i class="r"></i></div>
    <div class="who">
      <img src="{AVA}">
      <div><div class="n">abfk <span>229182</span></div><div class="z">发帖于 2024-09-22</div></div>
    </div>
    <div class="tt">{TITLE}</div>
    <div class="pp">{BODY}</div>
    <div class="stub">
      <div class="stats">💬 <b>16789</b> 回复 · 👁 <b>2.1w</b> · ⭐ <b>456</b><br>座席 <b>E7</b> · 已验票</div>
      <div class="barcode"></div>
    </div>
    <div class="tacts"><div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div></div>
    <div class="topen">查看完整帖子 ›</div>
  </div>
</div></div>'''

GLASS = f'''<div class="glass"><div class="gin">
  <div class="ghead">
    <div class="gava"><img src="{AVA}"></div>
    <div class="gid">
      <div class="n">abfk</div>
      <div class="s">uid 229182 · <b>2024-09-22</b> · 自由搏击区</div>
    </div>
  </div>
  <div class="gtt">{TITLE}</div>
  <div class="gpp">{BODY}</div>
  <div class="gsep"></div>
  <div class="gstats"><span>💬 <b>16789</b> 回复</span><span>👁 <b>2.1w</b> 查看</span><span>⭐ <b>456</b> 评分</span></div>
  <div class="gacts">
    <div class="gbtn">☆<span class="lbl">收藏</span></div>
    <div class="gbtn">↗<span class="lbl">分享</span></div>
    <div class="gbtn hide">🙈<span class="lbl">隐藏</span></div>
  </div>
  <div class="gopen">查看完整帖子 ›</div>
  </div>
</div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Peek 改版 v2 · 全息门票 / 黑玻璃座舱</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">Peek v2</span>两个质感方向：全息门票 / 黑玻璃座舱</h1>
  <div class="sub">2026-10-15 · uiverse dexter-st「TICKET」& dorian_8749「Now Playing」移植 · 功能零改动 · 彩虹已收进紫粉色板（审美宪法）</div>
</header>

<section>
  <h2>① 两个方案</h2>
  <div class="duo">

    <div class="pcase">
      <div class="pt">方案④ · 全息门票 <span class="ref">ref: dexter-st TICKET</span></div>
      <div class="ps">长按 = 撕下一张票。上下打孔锯齿边 + 撕裂线（两侧缺口）+ 夜光全息纸纹 + 条形码票根。</div>
      <div class="stage">{TICKET}</div>
      <ul class="pnote">
        <li><b>票头</b> = 版区名大字（TICKET 位）+ 帖子编号（tid 装饰）</li>
        <li><b>撕裂线</b>上 = 作者名片头；<b>票根</b> = 数据 + 条形码（tid 生成的装饰条纹）+ 三操作</li>
        <li>彩虹全息<b>收进紫粉金</b>：参考的 conic 全彩虹与色板冲突（审美宪法），取其「纸面流光」气质</li>
        <li>锯齿边用 mask 实现，Compose 端用 Canvas 画锯齿 Path + 阴影</li>
        <li>可选动效：弹出时票面全息光晕缓慢漂移（8s 循环，静态也成立）</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">方案⑤ · 黑玻璃座舱 <span class="ref">ref: dorian_8749 Now Playing</span></div>
      <div class="ps">纯黑玻璃 + 多层 inset 高光（玻璃厚度感）+ 三枚圆形玻璃按钮 + 彩丝渐变分隔线。</div>
      <div class="stage">{GLASS}</div>
      <ul class="pnote">
        <li><b>inset 高光</b>直接搬参考数值（暗色版）——玻璃「厚」的来源</li>
        <li>头像 58dp 圆角方带内嵌 ring（= 参考的唱片图标位）</li>
        <li><b>三操作改圆形玻璃按钮</b>（参考核心气质），带小字标签</li>
        <li>分隔线用彩丝渐变（幻彩身份），比方案①的细线更亮眼</li>
        <li>正文字号/行距与方案①一致，可读性不变</li>
      </ul>
    </div>

  </div>
</section>

<section>
  <h2>② 和 v1 三案的关系</h2>
  <div class="card note">
    <b>质变 vs 排版：</b>v1 三案（经典名片/居中徽章/竖杠引文）动的是<b>信息架构</b>（加头像、分主次）；v2 这两案动的是<b>材质与隐喻</b>——门票把「长按」变成「撕票」的仪式，黑玻璃把弹窗变成「精密座舱」。两者信息架构都沿用 v1①的结论（头像+名+uid+时间 / 数据行 / 三操作 / 主按钮进帖）。<br>
    <span class="small">可混搭：例如「⑤黑玻璃座舱 + ④的锯齿票边」「⑤圆钮 + ①的横版头」。风险标注：④锯齿边+撕裂线的 Compose 实现量中等（Canvas Path），⑤几乎纯样式改动最省；门票的全息光晕若做动画需遵守流动减法（静态光晕已足够成立）。</span>
  </div>
</section>

<footer>Peek v2 · 仅预览未改码 —— 定方向：④门票 / ⑤黑玻璃 / 混搭 / 回到 v1①，一句话我动码。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/长按Peek改版v2.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
