#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
幻彩渐变 · 浅色主题四候选预览生成器（2026-10-19 · 打破箔概念）
==============================================================
用户反馈：箔面封面仍在「箔」的隐喻里打转。要求抛开一切现有约束
（纸底、折棱、彩丝环、封面版式），回归「幻彩渐变」四个字，
用最简单的科技美学重做浅色主题。V2（底边撕入）点名保留进候选。

四个候选：
  A 弥散光晕  三团雾状彩光（粉/紫/青）极慢漂移，无边界无棱，光即全部
  B 流动光幕  淡雾底 + 一道宽光幕 24s 扫过封面，渐变在流动（最直白的「渐变」）
  C 全息圆盘  偏心幻彩圆盘（位置按 tid 哈希五锚点），圆外近白
  V2 底边撕入 用户点名保留

共用：页面底近白冷调（卡片浮得起）、标题安全带、24s 时间相位、
tid 驱动的卡间差异（A 色相偏移 / B 光幕相位天然错开 / C 圆锚点）、
卡框统一弱化为 0.75dp 渐变细边。

输出：outputs/preview_iridescent/幻彩渐变浅色主题四候选预览.html
"""
import os
import zlib

PINK, PURPLE, CYAN = "#FF4FB0", "#8F5CFF", "#22CFE6"
PAPER, INK, WHITE = "#FBF9FE", "#35243C", "#FFFFFF"

W = 166.0

AVA = [("#FF9A9E", "#FECFEF"), ("#A18CD1", "#FBC2EB"), ("#84FAB0", "#8FD3F4"),
       ("#FCCF90", "#F9A8B8"), ("#5EE7DF", "#B490CA"), ("#F6D365", "#FDA085"),
       ("#C2FFD8", "#A6C1EE"), ("#F093FB", "#F5576C")]


def _stops(stops):
    return "".join(f'<stop offset="{o:.3f}" stop-color="{c}" stop-opacity="{a:.2f}"/>' for o, c, a in stops)


def _seatbelt(H, mid=0.62):
    y0, y1 = 0.30 * H, 0.72 * H
    return (f'<linearGradient id="sb{{uid}}" gradientUnits="userSpaceOnUse" x1="0" y1="{y0:.1f}" x2="0" y2="{y1:.1f}">'
            f'<stop offset="0" stop-color="{WHITE}" stop-opacity="0"/>'
            f'<stop offset="0.5" stop-color="{WHITE}" stop-opacity="{mid:.2f}"/>'
            f'<stop offset="1" stop-color="{WHITE}" stop-opacity="0"/></linearGradient>'
            f'<rect x="0" y="{y0:.1f}" width="{W:.0f}" height="{y1 - y0:.1f}" fill="url(#sb{{uid}})"/>')


# ── A · 弥散光晕 ──────────────────────────────────────────────
# 三团 radialGradient，cx/cy 带 data 属性由 JS 24s 漂移（三团相位错开）
A_BLOBS = [
    (PINK, 0.55, 0.80, 0.12, 0.22, 0.05, 0.04, 0.00),   # 色, alpha, r(×W), cx, cy, dx, dy, 相位
    (PURPLE, 0.50, 0.85, 0.88, 0.12, 0.04, 0.05, 0.33),
    (CYAN, 0.42, 0.75, 0.45, 0.80, 0.05, 0.04, 0.67),
]


def foil_a(uid, H, hue=0):
    defs, bodies = [], []
    for i, (c, a, r, cx, cy, dx, dy, ph) in enumerate(A_BLOBS):
        gid = f"ab{i}_{uid}"
        cx_px, cy_px, r_px = cx * W, cy * H, r * W
        defs.append(f'<radialGradient class="drift" id="{gid}" data-cx="{cx_px:.1f}" data-cy="{cy_px:.1f}" '
                    f'data-dx="{dx * W:.1f}" data-dy="{dy * H:.1f}" data-ph="{ph:.2f}" '
                    f'cx="{cx_px:.1f}" cy="{cy_px:.1f}" r="{r_px:.1f}" gradientUnits="userSpaceOnUse">'
                    f'<stop offset="0" stop-color="{c}" stop-opacity="{a:.2f}"/>'
                    f'<stop offset="0.55" stop-color="{c}" stop-opacity="{a * 0.55:.2f}"/>'
                    f'<stop offset="1" stop-color="{c}" stop-opacity="0"/></radialGradient>')
        bodies.append(f'<rect x="-20" y="-20" width="{W + 40:.0f}" height="{H + 40:.0f}" fill="url(#{gid})"/>')
    defs.append(_seatbelt(H, 0.66).format(uid=uid))
    bodies.append(f'<rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>')
    hue_css = f' style="filter:hue-rotate({hue}deg)"' if hue else ""
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none"{hue_css}>
  <defs>{"".join(defs)}</defs>
  {"".join(bodies)}
</svg>'''


# ── B · 流动光幕 ──────────────────────────────────────────────
B_BLOBS = [(PINK, 0.30, 0.80, 0.15, 0.25), (PURPLE, 0.28, 0.85, 0.85, 0.15), (CYAN, 0.24, 0.75, 0.45, 0.78)]


def foil_b(uid, H, hue=0):
    defs, bodies = [], []
    for i, (c, a, r, cx, cy) in enumerate(B_BLOBS):
        gid = f"bb{i}_{uid}"
        defs.append(f'<radialGradient id="{gid}" cx="{cx * W:.1f}" cy="{cy * H:.1f}" r="{r * W:.1f}" gradientUnits="userSpaceOnUse">'
                    f'<stop offset="0" stop-color="{c}" stop-opacity="{a:.2f}"/>'
                    f'<stop offset="0.55" stop-color="{c}" stop-opacity="{a * 0.5:.2f}"/>'
                    f'<stop offset="1" stop-color="{c}" stop-opacity="0"/></radialGradient>')
        bodies.append(f'<rect x="-20" y="-20" width="{W + 40:.0f}" height="{H + 40:.0f}" fill="url(#{gid})"/>')
    defs.append(_seatbelt(H, 0.55).format(uid=uid))
    bodies.append(f'<rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>')
    hue_css = f' style="filter:hue-rotate({hue}deg)"' if hue else ""
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none"{hue_css}>
  <defs>{"".join(defs)}</defs>
  {"".join(bodies)}
</svg>
<div class="curtain"></div>'''


# ── C · 全息圆盘 ──────────────────────────────────────────────
C_ANCHORS = [(0.30, 0.40), (0.70, 0.40), (0.50, 0.52), (0.34, 0.62), (0.66, 0.62)]


def foil_c(uid, H, anchor=0):
    cx, cy = C_ANCHORS[anchor % len(C_ANCHORS)]
    cx_px, cy_px = cx * W, cy * H
    r = 0.46 * min(W, H) + 0.06 * W  # 圆径 ≈ 封面短边的 92%+
    mask = (f'<radialGradient id="mk{uid}" cx="0.5" cy="0.5" r="0.5">'
            f'<stop offset="0" stop-color="#FFFFFF" stop-opacity="1"/>'
            f'<stop offset="0.80" stop-color="#FFFFFF" stop-opacity="1"/>'
            f'<stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient>'
            f'<mask id="msk{uid}" maskUnits="userSpaceOnUse" x="{cx_px - r:.1f}" y="{cy_px - r:.1f}" width="{2 * r:.1f}" height="{2 * r:.1f}">'
            f'<rect x="{cx_px - r:.1f}" y="{cy_px - r:.1f}" width="{2 * r:.1f}" height="{2 * r:.1f}" fill="url(#mk{uid})"/></mask>')
    lg = (f'<linearGradient id="cg{uid}" gradientUnits="userSpaceOnUse" x1="{cx_px - r * 0.8:.1f}" y1="{cy_px - r * 0.8:.1f}" x2="{cx_px + r * 0.8:.1f}" y2="{cy_px + r * 0.8:.1f}">'
          + _stops([(0.000, PINK, 0.88), (0.480, PURPLE, 0.80), (1.000, CYAN, 0.72)]) + "</linearGradient>")
    halo = (f'<radialGradient id="hl{uid}" cx="{cx_px:.1f}" cy="{cy_px:.1f}" r="{r * 1.15:.1f}" gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="{PURPLE}" stop-opacity="0.14"/>'
            f'<stop offset="1" stop-color="{PURPLE}" stop-opacity="0"/></radialGradient>')
    return f'''<svg class="foil breathe" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none">
  <defs>{mask}{lg}{halo}{_seatbelt(H, 0.55).format(uid=uid)}</defs>
  <rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#hl{uid})"/>
  <circle cx="{cx_px:.1f}" cy="{cy_px:.1f}" r="{r:.1f}" fill="url(#cg{uid})" mask="url(#msk{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>
</svg>'''


# ── V2 · 底边撕入（保留候选，复用上一版） ──────────────────────
def foil_v2(uid, H):
    x1, y1 = 0.15 * W, 0.98 * H
    x2, y2 = 0.85 * W, 0.38 * H
    main = [(0.000, PINK, 0.88), (0.350, PINK, 0.60), (0.450, PURPLE, 0.78),
            (0.700, PURPLE, 0.52), (0.780, CYAN, 0.70), (1.000, CYAN, 0.58)]
    y0, y1c = 0.56 * H, 0.62 * H
    tear = (f"M0,{y0:.1f} "
            f"Q{0.20 * W:.1f},{y0 - 0.055 * H:.1f} {0.40 * W:.1f},{y0 + 0.012 * H:.1f} "
            f"Q{0.60 * W:.1f},{y1c + 0.058 * H:.1f} {0.80 * W:.1f},{y1c - 0.018 * H:.1f} "
            f"Q{0.92 * W:.1f},{y1c - 0.045 * H:.1f} {W:.0f},{y1c + 0.012 * H:.1f} "
            f"L{W:.0f},{H:.1f} L0,{H:.1f} Z")
    lip = (f"M0,{y0 + 0.022 * H:.1f} "
           f"Q{0.20 * W:.1f},{y0 - 0.033 * H:.1f} {0.40 * W:.1f},{y0 + 0.034 * H:.1f} "
           f"Q{0.60 * W:.1f},{y1c + 0.080 * H:.1f} {0.80 * W:.1f},{y1c + 0.004 * H:.1f} "
           f"Q{0.92 * W:.1f},{y1c - 0.023 * H:.1f} {W:.0f},{y1c + 0.034 * H:.1f}")
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none">
  <defs>
    <linearGradient class="ax" id="m{uid}" data-x1="{x1:.1f}" data-y1="{y1:.1f}" data-x2="{x2:.1f}" data-y2="{y2:.1f}" data-axis="y" gradientUnits="userSpaceOnUse" x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}">{_stops(main)}</linearGradient>
    {_seatbelt(H, 0.34).format(uid=uid)}
  </defs>
  <path d="{tear}" fill="url(#m{uid})"/>
  <path d="{lip}" fill="none" stroke="{WHITE}" stroke-opacity="0.55" stroke-width="{0.011 * H:.2f}" stroke-linecap="round"/>
  <rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>
</svg>'''


FOIL = {"a": foil_a, "b": foil_b, "c": foil_c, "v2": foil_v2}
VNAME = {"a": "A · 弥散光晕", "b": "B · 流动光幕", "c": "C · 全息圆盘", "v2": "V2 · 底边撕入"}
VTAG = {"a": "雾与时间", "b": "渐变在流动", "c": "一枚全息盘", "v2": "保留候选"}


def card(uid, variant, title, author, tag, time, replies, hot=False, s=1, cover_h=120, read=False,
         hue=0, anchor=0):
    def px(x):
        return int(round(x * s))
    g1, g2 = AVA[zlib.crc32(author.encode("utf-8")) % len(AVA)]
    letter = author[0]
    title_color = f"rgba(53,36,60,{0.6 if read else 1.0})"
    rep_style = "color:#FF2C55;font-weight:600;" if hot else ""
    bub_stroke = "#FF2C55" if hot else "#88708E"
    if variant == "a":
        foil = foil_a(uid, cover_h, hue)
    elif variant == "c":
        foil = foil_c(uid, cover_h, anchor)
    elif variant == "b":
        foil = foil_b(uid, cover_h, hue)
    else:
        foil = foil_v2(uid, cover_h)
    return f'''<div class="card" style="width:{px(166)}px">
  <div class="ring"><div class="inner">
    <div class="cover" style="height:{px(cover_h)}px">
      <div class="paper"></div>
      {foil}
      <div class="ctitlebox">
        <div class="ctitle" style="font-size:{px(15)}px;line-height:{px(21)}px;padding:0 {px(18)}px;color:{title_color}">{title}</div>
      </div>
    </div>
    <div class="meta">
      <div class="mrow" style="padding:{px(6)}px {px(12)}px {px(4)}px">
        <span class="ava" style="width:{px(24)}px;height:{px(24)}px;font-size:{px(12)}px;background:linear-gradient(135deg,{g1},{g2})">{letter}</span>
        <span style="width:{px(7)}px"></span>
        <span class="author" style="font-size:{px(12)}px">{author}</span>
        <span class="grow"></span>
        <svg width="{px(13)}" height="{px(13)}" viewBox="0 0 24 24" fill="none" stroke="{bub_stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.2L4 17.2V4h16v12z"/></svg>
        <span style="width:{px(3)}px"></span>
        <span class="reps" style="font-size:{px(11)}px;{rep_style}">{replies}</span>
      </div>
      <div class="mrow" style="padding:0 {px(12)}px {px(10)}px">
        <span class="tag" style="font-size:{px(11)}px">{tag}</span>
        <span class="grow"></span>
        <span class="time" style="font-size:{px(10)}px">{time}</span>
      </div>
    </div>
  </div></div>
</div>'''


def strip(variant):
    """主题元素示意：页面底 / 主按钮 / chips / 细边卡"""
    return f'''<div class="strip">
  <div class="stbg"><span class="stlab">页面底</span></div>
  <div class="stbtn">签到</div>
  <div class="stchips"><span class="sc sel">全部</span><span class="sc">关注</span><span class="sc">游戏</span></div>
  <div class="stcard"><span class="stlab2">卡框 0.75dp 细边</span></div>
</div>'''


CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F4F1F8; --card:#FFFFFF; --pink:#D6339C; --purple:#8B3FD9; --warn:#C2410C;}
*{box-sizing:border-box; margin:0; padding:0;}
body{background:var(--bg); color:var(--ink); font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; line-height:1.65; padding:32px 20px 80px;}
.wrap{max-width:1240px; margin:0 auto;}
header h1{font-size:25px; letter-spacing:.5px;}
header .sub{color:var(--ink2); font-size:13px; margin-top:6px;}
.badge{display:inline-block; background:linear-gradient(100deg,var(--pink),var(--purple)); color:#fff; font-size:12px; padding:2px 10px; border-radius:99px; margin-right:8px; vertical-align:2px;}
section{margin-top:36px;}
h2{font-size:18px; border-left:4px solid var(--pink); padding-left:10px; margin-bottom:6px;}
h2 .vt{font-size:12px; font-weight:700; color:#fff; background:linear-gradient(100deg,var(--pink),var(--purple)); border-radius:99px; padding:1px 9px; margin-left:8px; vertical-align:2px;}
p{font-size:14px;}
.small{font-size:12.5px; color:var(--ink2);}
.note{background:linear-gradient(100deg,#FDF0F8,#F3EBFE); border:1px solid #F0D9EE; border-radius:14px; padding:14px 18px; font-size:13.5px; margin-top:14px;}
.note b{color:var(--pink);}
.why{background:var(--card); border:1px solid var(--line); border-radius:14px; padding:16px 20px; font-size:14px; margin-top:14px;}
.why b{color:var(--pink);}
/* ── 候选区 ── */
.cand{background:var(--card); border:1px solid var(--line); border-radius:18px; padding:20px; margin-top:18px;}
.cand.reco{border:1.5px solid #F0C9E4; background:linear-gradient(180deg,#FFF9FD,#FFFFFF);}
.candhead{display:flex; align-items:baseline; gap:10px;}
.candhead .cn{font-size:17px; font-weight:800;}
.candhead .cs{font-size:13px; color:var(--ink2);}
.reco-flag{font-size:11.5px; font-weight:700; color:#fff; background:linear-gradient(100deg,#E0449B,#8F5CFF); border-radius:99px; padding:2px 10px;}
.candbody{display:flex; gap:26px; margin-top:14px; flex-wrap:wrap;}
.candcards{display:flex; gap:16px;}
.cap{font-size:11.5px; color:var(--ink2); margin-top:6px;}
.candright{flex:1; min-width:280px;}
.canddesc{font-size:13.5px;}
.canddesc b{color:var(--pink);}
.strip{display:flex; align-items:center; gap:12px; margin-top:14px; flex-wrap:wrap;}
.stbg{width:150px; height:52px; border-radius:10px; background:linear-gradient(180deg,#EFEDF6,#F8F5FC); border:1px solid var(--line); position:relative;}
.stlab{position:absolute; left:8px; bottom:4px; font-size:10px; color:#9A8FA8;}
.stbtn{height:34px; padding:0 22px; border-radius:99px; background:linear-gradient(100deg,#FF4FB0,#8F5CFF,#22CFE6); color:#fff; font-size:13px; font-weight:700; display:flex; align-items:center; box-shadow:0 2px 8px rgba(143,92,255,.25);}
.stchips{display:flex; gap:6px;}
.sc{font-size:11px; padding:3px 11px; border-radius:99px; font-weight:600; background:#F1EDF7; color:#88708E;}
.sc.sel{background:linear-gradient(100deg,#FF4FB0,#8F5CFF); color:#fff;}
.stcard{width:130px; height:52px; border-radius:10px; background:#fff; border:0.75px solid transparent; background-image:linear-gradient(#fff,#fff),linear-gradient(135deg,rgba(255,79,176,.55),rgba(143,92,255,.55),rgba(34,207,230,.55)); background-origin:border-box; background-clip:padding-box,border-box; position:relative; box-shadow:0 1px 4px rgba(53,36,60,.06);}
.stlab2{position:absolute; left:8px; bottom:4px; font-size:10px; color:#9A8FA8;}
/* ── 卡片 ── */
.card .ring{border-radius:15.5px; padding:0.75px; background:linear-gradient(135deg,rgba(255,79,176,.55),rgba(143,92,255,.55),rgba(34,207,230,.55)); box-shadow:0 2px 10px rgba(53,36,60,.08),0 1px 3px rgba(53,36,60,.06);}
.card .inner{border-radius:14.75px; background:#fff; overflow:hidden;}
.cover{position:relative; overflow:hidden;}
.paper{position:absolute; inset:0; background:#FBF9FE;}
.foil{position:absolute; inset:0; width:100%; height:100%; pointer-events:none;}
.curtain{position:absolute; top:-30%; left:-70%; width:55%; height:170%; pointer-events:none;
  background:linear-gradient(100deg,rgba(255,255,255,0) 0%,rgba(255,255,255,.60) 32%,rgba(255,79,176,.34) 58%,rgba(143,92,255,.28) 74%,rgba(34,207,230,.24) 88%,rgba(255,255,255,0) 100%);
  transform:rotate(16deg); animation:sweep 24s linear infinite;}
@keyframes sweep{from{transform:translateX(0) rotate(16deg);} to{transform:translateX(340%) rotate(16deg);}}
.breathe{animation:breathe 24s ease-in-out infinite; transform-origin:center;}
@keyframes breathe{0%,100%{transform:scale(1);}50%{transform:scale(1.035);}}
.ctitlebox{position:absolute; inset:0; display:flex; align-items:center; justify-content:center;}
.ctitle{font-weight:700; text-align:center; display:-webkit-box; -webkit-line-clamp:5; -webkit-box-orient:vertical; overflow:hidden;}
.meta{border-top:1px solid rgba(136,112,142,.10);}
.mrow{display:flex; align-items:center;}
.grow{flex:1;}
.ava{border-radius:50%; color:#fff; font-weight:700; display:flex; align-items:center; justify-content:center; flex:none;}
.author{color:#88708E; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;}
.reps{color:#88708E;}
.tag{color:#88708E; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;}
.time{color:rgba(136,112,142,.7); white-space:nowrap;}
/* ── 信息流片段 ── */
.feed4{display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-top:14px; width:352px;}
/* ── 对比表 ── */
table.cmp{width:100%; border-collapse:collapse; margin-top:14px; font-size:13px;}
table.cmp th,table.cmp td{border:1px solid var(--line); padding:8px 12px; text-align:left; vertical-align:top;}
table.cmp th{background:#FAF7FC; font-size:12.5px;}
table.cmp td:first-child{font-weight:700; white-space:nowrap;}
.dot{display:inline-block; width:9px; height:9px; border-radius:50%; margin-right:6px; vertical-align:0;}
"""

JS = """
(function(){
  var drifts = Array.prototype.slice.call(document.querySelectorAll('radialGradient.drift'));
  var axes = Array.prototype.slice.call(document.querySelectorAll('linearGradient.ax'));
  var last = 0;
  function tick(t){
    if(t - last < 66) { requestAnimationFrame(tick); return; }
    last = t;
    var ph = (t / 24000) % 1;
    var ang = ph * 2 * Math.PI;
    for(var i = 0; i < drifts.length; i++){
      var g = drifts[i];
      var cx = parseFloat(g.getAttribute('data-cx')), cy = parseFloat(g.getAttribute('data-cy'));
      var dx = parseFloat(g.getAttribute('data-dx')), dy = parseFloat(g.getAttribute('data-dy'));
      var off = parseFloat(g.getAttribute('data-ph')) * 2 * Math.PI;
      g.setAttribute('cx', (cx + Math.sin(ang + off) * dx).toFixed(1));
      g.setAttribute('cy', (cy + Math.cos(ang + off) * dy).toFixed(1));
    }
    var dyv = Math.sin(ang) * 0.02 * 166;
    for(var j = 0; j < axes.length; j++){
      var a = axes[j];
      a.setAttribute('y1', (parseFloat(a.getAttribute('data-y1')) - dyv).toFixed(1));
      a.setAttribute('y2', (parseFloat(a.getAttribute('data-y2')) - dyv).toFixed(1));
    }
    requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
})();
"""


def build():
    uid = [0]

    def nxt():
        uid[0] += 1
        return uid[0]

    short_t = "通勤听什么播客"
    long_t = "有人用 mini LED 当显示器办公吗？想入个 27 寸的，主要写代码看文档，偶尔修图，预算三千左右，求推荐型号"

    cand = {
        "a": dict(
            one="封面就是三团雾状彩光，没有任何边界和折痕——光本身就是封面。",
            desc="三团大半径径向渐变色团（粉 / 紫 / 青）叠在近白底上互相渗透，以 24 秒为周期极慢漂移（三团相位错开，像呼吸）。"
                 "卡间差异靠 <b>tid 哈希驱动的色相偏移</b>（±20°）：一屏之内每张卡的色调都不同，但都是幻彩家族。"
                 "没有任何箔元素：没有硬边、没有棱、没有撕边。<b>这是四个候选里最「安静」的一个</b>——封面不抢标题，阅读最稳。",
        ),
        "b": dict(
            one="淡雾底上一道宽光幕 24 秒扫过封面——「渐变」在流动，最直白的幻彩渐变。",
            desc="底层是一层很淡的弥散雾（透明度只有 A 的三分之二），上面压一道 <b>45° 宽光幕</b>（白 → 粉 → 青），"
                 "24 秒一周期从封面左侧扫到右侧，扫过之处整体提亮。标题带垫固定安全带，光幕从标题上下流过不咬字。"
                 "八张卡的光幕同相位扫描，靠封面底色雾团的 tid 色相偏移错开。<b>实现最简单、最贴「渐变」二字。</b>",
        ),
        "c": dict(
            one="一枚偏心全息圆盘落在近白纸上，圆盘位置按帖子哈希五锚点轮换。",
            desc="封面近白，一个直径约封面短边 92% 的圆盘，圆内是粉 → 紫 → 青的斜向渐变，圆边完全雾化（无硬边）。"
                 "圆盘位置按 tid 哈希落在五个锚点（左上 / 右上 / 居中 / 左下 / 右下），"
                 "整盘以 24 秒周期做 3.5% 幅度的呼吸。<b>记忆点最强</b>，几何形态也最接近「科技产品」的气质。",
        ),
        "v2": dict(
            one="你点名保留的候选：箔从底边撕入。放在这里做对照基准。",
            desc="上一版的底边撕入原样保留：微曲撕边 + 翻起纸边的白高光，标题在上半的纸上。"
                 "它和其他三个候选的本质区别是<b>仍然带着「纸 / 箔」的物质隐喻</b>，而 A / B / C 只有光。",
        ),
    }

    # 信息流片段（每候选 4 卡）
    feeds = {
        "a": [("h", "2026 年 10 月游戏发售表", "sd444", "游戏区", "3小时前", 45, False, 120),
              ("h", "预算 4000，求推荐 65 寸电视。主要看流媒体和纪录片，偶尔接主机玩游戏，最好别拖影", "战术核显卡", "数码区", "10-19", 108, True, 157),
              ("h", "晒一下今年的桌搭，第三版了", "猫阿姨", "生活区", "昨天", 23, False, 120),
              ("h", "大家通勤路上都听什么播客", "早八人", "生活区", "10-19", 89, False, 120)],
        "b": [("h", "周末爬了座山，拍了几张照片", "山那边的风", "生活区", "10-17", 12, False, 120),
              ("h", "求推荐声音小的机械键盘，办公室用，预算五百左右，青轴手感但是别太吵的有没有", "sdhd", "数码区", "10-18", 67, False, 157),
              ("h", "楼下快递柜悄悄涨价了，大家发现没？超时每小时都开始计费了，这算违规吗", "长安乱", "闲聊区", "2小时前", 156, True, 136),
              ("h", "整理了点复古游戏 ROM 的坑", "卡带收藏家", "游戏区", "10-16", 34, False, 120)],
        "c": [("h", "通勤听什么播客", "早八人", "生活区", "10-19", 89, False, 120),
              ("h", "有人用 mini LED 当显示器办公吗？想入个 27 寸的，主要写代码看文档，偶尔修图", "战术核显卡", "数码区", "10-19", 108, True, 157),
              ("h", "周末爬了座山，拍了几张照片", "山那边的风", "生活区", "10-17", 12, False, 120),
              ("h", "晒一下今年的桌搭，第三版了", "猫阿姨", "生活区", "昨天", 23, False, 120)],
        "v2": [("h", "2026 年 10 月游戏发售表", "sd444", "游戏区", "3小时前", 45, False, 120),
               ("h", "大家通勤路上都听什么播客", "早八人", "生活区", "10-19", 89, False, 120),
               ("h", "预算 4000，求推荐 65 寸电视。主要看流媒体和纪录片，偶尔接主机玩游戏", "战术核显卡", "数码区", "10-19", 108, True, 157),
               ("h", "整理了点复古游戏 ROM 的坑", "卡带收藏家", "游戏区", "10-16", 34, False, 120)],
    }
    hues = [0, -14, 18, -8, 10, -20, 6, 14]
    anchors = [0, 3, 1, 4, 2, 0, 3, 1]

    secs = []
    for v in ["a", "b", "c", "v2"]:
        reco = ' reco' if v == "b" else ""
        flag = '<span class="reco-flag">我的推荐</span>' if v == "b" else ""
        single = []
        for ch, t in [(120, short_t), (157, long_t)]:
            if v == "a":
                single.append(card(nxt(), v, t, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=ch))
            elif v == "c":
                single.append(card(nxt(), v, t, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=ch, anchor=2))
            else:
                single.append(card(nxt(), v, t, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=ch))
        fcards = []
        for i, (_, t, a, g, tm, r, h, ch) in enumerate(feeds[v]):
            if v == "a":
                fcards.append(card(nxt(), v, t, a, g, tm, r, hot=h, s=1, cover_h=ch, hue=hues[i]))
            elif v == "c":
                fcards.append(card(nxt(), v, t, a, g, tm, r, hot=h, s=1, cover_h=ch, anchor=anchors[i]))
            else:
                fcards.append(card(nxt(), v, t, a, g, tm, r, hot=h, s=1, cover_h=ch))
        secs.append(f'''
<section>
  <h2>{VNAME[v]}<span class="vt">{VTAG[v]}</span></h2>
  <div class="cand{reco}">
    <div class="candhead"><span class="cn">{VNAME[v]}</span><span class="cs">{cand[v]['one']}</span>{flag}</div>
    <div class="candbody">
      <div>
        <div class="candcards">{''.join(single)}</div>
        <div class="cap">左：一行标题（最短封页）· 右：五行标题（最高封页）· 2 倍放大</div>
      </div>
      <div class="candright">
        <div class="canddesc">{cand[v]['desc']}</div>
        {strip(v)}
      </div>
    </div>
    <div class="feed4">{''.join(fcards)}</div>
    <div class="cap" style="margin-top:6px">信息流片段（1x 真机尺寸 · 4 张）：{'色相按 tid 偏移，四张卡色调不同' if v == 'a' else ('圆盘位置按 tid 哈希落五个锚点' if v == 'c' else ('光幕同相位扫描，八卡错开靠底色雾团色相' if v == 'b' else '撕边波形固定，卡间差异仅标题长度'))}</div>
  </div>
</section>''')

    html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>幻彩渐变 · 浅色主题四候选预览</title><style>{CSS}</style></head>
<body><div class="wrap">

<header>
  <h1>幻彩渐变 · 浅色主题 · 四候选</h1>
  <div class="sub">2026-10-19 · 抛开「箔」的隐喻，回归「幻彩渐变」四个字 · 最简单的科技美学 · 状态：<span class="badge">待你拍板方向</span></div>
</header>

<section>
  <div class="why">
    <b>这版改了什么思路：</b>上一版「箔面封面」再被否，核心问题是还困在「纸 / 箔」的物质隐喻里——有硬边、有棱、有撕边，本质上还是「在纸上贴东西」。这版把现有约束全部放下：纸底、折棱、彩丝环、封面版式都不参考，从「幻彩渐变」四个字本身出发，只留三样东西——<b>渐变、时间、随机</b>。四个候选里三个全新方向 + 你点名的 V2 保留做对照。卡框统一弱化为 0.75dp 渐变细边，主按钮 / 选中 chip 走粉紫青渐变，页面底仍是近白冷调（白卡才浮得起来）。
  </div>
</section>

{''.join(secs)}

<section>
  <h2>四个候选怎么挑</h2>
  <table class="cmp">
    <tr><th style="width:110px">候选</th><th>封面气质</th><th>卡间差异手段</th><th>标题可读性</th><th>实现成本</th></tr>
    <tr><td><span class="dot" style="background:#FF4FB0"></span>A 弥散光晕</td><td>最安静，封面不抢标题，久读不累</td><td>tid 色相偏移 ±20°</td><td>安全带 66% 白纱，≥8:1</td><td>三个 radialGradient + 相位漂移</td></tr>
    <tr><td><span class="dot" style="background:#8F5CFF"></span>B 流动光幕</td><td>「渐变」在流动，最直白、最科技</td><td>光幕同相位 + 底色雾团色相偏移</td><td>安全带 55%，光幕不咬字</td><td>最低：一个渐变矩形 + 相位平移</td></tr>
    <tr><td><span class="dot" style="background:#22CFE6"></span>C 全息圆盘</td><td>记忆点最强，几何形态最「产品」</td><td>tid 哈希五锚点 + 呼吸</td><td>安全带 55%，圆外本就在纸上</td><td>中：圆 + 羽化 mask + 锚点</td></tr>
    <tr><td><span class="dot" style="background:#B084B8"></span>V2 底边撕入</td><td>仍带纸 / 箔的物质隐喻</td><td>仅标题长度</td><td>标题本就在纸上，最稳</td><td>已有实现</td></tr>
  </table>
  <div class="note">
    <b>我为什么推荐 B：</b>「幻彩渐变」四个字里最核心的是「渐变」——A 把渐变雾化到底（偏向「弥散」），C 把渐变装进圆里（偏向「图形」），只有 B 让渐变保持渐变的长条形态并且<b>真的在流动</b>；同时它的实现成本最低（一个渐变矩形沿轴平移），八卡同相位扫描反而是辨识度（整屏像一道光周期扫过），卡间差异交给底色雾团的色相偏移。如果你的口味更偏「安静阅读」选 A，更偏「一眼记住」选 C。
  </div>
  <p class="kv" style="font-size:12.5px; color:var(--ink2); margin-top:10px;">说明：预览按绘制参数用 SVG / CSS 还原（渐变几何、24s 周期、15fps 心跳与真机一致）；圆盘羽化用 SVG mask，真机 Compose 可用 Brush.radialGradient 叠色实现。选定方向后我再动代码。</p>
</section>

</div>
<script>{JS}</script>
</body></html>'''

    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "outputs", "preview_iridescent")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, "幻彩渐变浅色主题四候选预览.html")
    with open(out, "w", encoding="utf-8") as f:
        f.write(html)
    print(out)


if __name__ == "__main__":
    build()
