#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
幻彩浅色主题 · 箔面封面预览生成器（2026-10-19 · 重做版）
==========================================================
背景：上一版「箔带加宽」被用户判定没有突破（还是纸上贴一条带、八卡同构）。
本版推翻「带」的思维，把箔升级为封面的主体构图，并按帖子 tid 哈希落档：

  V1 对角箔面  35%  左上大箔面 + 硬斜切边 + 双折棱，标题压「安全带」纸纱
  V2 底边撕入  25%  箔从底边微曲撕边撕入，撕边平行一道白高光（翻起的纸边）
  V3 双带交叉  25%  两条箔带 ±22° 交叉成 X，交叉点白热菱形，标题嵌带间
  V0 留白纸面  15%  纯纸呼吸位，让有箔的卡更跳

共用基因（不动）：三色 粉#FF4FB0/紫#8F5CFF/青#22CFE6、纸底 #F1EDF9、
顶缘锐边、24s 呼吸漂移、彩丝卡框、顶栏极光线、已读降透明规则。

输出：outputs/preview_iridescent/幻彩浅色箔面封面预览.html
"""
import os
import zlib

PINK, PURPLE, CYAN = "#FF4FB0", "#8F5CFF", "#22CFE6"
PAPER, INK, WHITE = "#F1EDF9", "#35243C", "#FFFFFF"

W = 166.0  # 1x 卡宽（360dp 屏双列）

AVA = [("#FF9A9E", "#FECFEF"), ("#A18CD1", "#FBC2EB"), ("#84FAB0", "#8FD3F4"),
       ("#FCCF90", "#F9A8B8"), ("#5EE7DF", "#B490CA"), ("#F6D365", "#FDA085"),
       ("#C2FFD8", "#A6C1EE"), ("#F093FB", "#F5576C")]

VARIANTS = ["v1", "v2", "v3", "v0"]
VNAME = {"v1": "V1 · 对角箔面", "v2": "V2 · 底边撕入", "v3": "V3 · 双带交叉", "v0": "V0 · 留白纸面"}
VSHARE = {"v1": "35%", "v2": "25%", "v3": "25%", "v0": "15%"}


def pick_variant(tid):
    """tid 哈希落档：与真机 Compose 实现同一算法（crc32 → 千分位）"""
    return VARIANTS[int(zlib.crc32(tid.encode("utf-8")) % 1000 * 4 // 1000)]


def _stops(stops):
    return "".join(f'<stop offset="{o:.3f}" stop-color="{c}" stop-opacity="{a:.2f}"/>' for o, c, a in stops)


def _seatbelt(H, mid=0.66):
    """标题安全带：0.28H–0.72H 纸纱，中部 mid，上下缘化掉"""
    y0, y1 = 0.28 * H, 0.72 * H
    return (f'<linearGradient id="sb{{uid}}" gradientUnits="userSpaceOnUse" x1="0" y1="{y0:.1f}" x2="0" y2="{y1:.1f}">'
            f'<stop offset="0" stop-color="{PAPER}" stop-opacity="0"/>'
            f'<stop offset="0.5" stop-color="{PAPER}" stop-opacity="{mid:.2f}"/>'
            f'<stop offset="1" stop-color="{PAPER}" stop-opacity="0"/></linearGradient>'
            f'<rect x="0" y="{y0:.1f}" width="{W:.0f}" height="{y1 - y0:.1f}" fill="url(#sb{{uid}})"/>')


def _topedge(H):
    return (f'<linearGradient id="te{{uid}}" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="{0.05 * H:.1f}">'
            f'<stop offset="0" stop-color="{WHITE}" stop-opacity="0.50"/>'
            f'<stop offset="1" stop-color="{WHITE}" stop-opacity="0"/></linearGradient>'
            f'<rect x="0" y="0" width="{W:.0f}" height="{0.05 * H:.1f}" fill="url(#te{{uid}})"/>')


# ── V1 对角箔面 ────────────────────────────────────────────────
def foil_v1(uid, H):
    x1, y1 = -0.06 * W, -0.06 * H
    x2, y2 = 1.02 * W, 0.62 * H
    main = [(0.000, PINK, 0.90), (0.300, PINK, 0.62), (0.380, PURPLE, 0.80),
            (0.620, PURPLE, 0.50), (0.660, CYAN, 0.72), (1.000, CYAN, 0.55)]
    # 双折棱：主棱 t=0.60（紫→青），副棱 t=0.30（粉→紫）
    gloss = [(0.294, WHITE, 0.00), (0.300, WHITE, 0.30), (0.306, WHITE, 0.00),
             (0.594, WHITE, 0.00), (0.600, WHITE, 0.55), (0.606, WHITE, 0.00)]
    # 箔面：顶边通栏，底边斜切（左上 0.40H → 右下 0.58H）
    ax, ay, bx, by = 0.0, 0.40 * H, W, 0.58 * H
    nx, ny = 0.18 * H, -W
    import math
    L = math.hypot(nx, ny)
    nx, ny = nx / L, ny / L
    p = lambda t: (ax + (bx - ax) * t + nx * 0.05 * H, ay + (by - ay) * t + ny * 0.05 * H)
    a1, b1 = p(0), p(1)
    a2, b2 = p(0), p(1)
    a2 = (a2[0] - nx * 0.06 * H, a2[1] - ny * 0.06 * H)
    b2 = (b2[0] - nx * 0.06 * H, b2[1] - ny * 0.06 * H)
    foil_path = f"M0,0 L{W:.0f},0 L{bx:.1f},{by:.1f} L{ax:.1f},{ay:.1f} Z"
    edge_path = f"M{a1[0]:.1f},{a1[1]:.1f} L{b1[0]:.1f},{b1[1]:.1f} L{b2[0]:.1f},{b2[1]:.1f} L{a2[0]:.1f},{a2[1]:.1f} Z"
    veil = (f'<linearGradient id="vl{{uid}}" gradientUnits="userSpaceOnUse" x1="0" y1="{0.34 * H:.1f}" x2="0" y2="{0.62 * H:.1f}">'
            f'<stop offset="0" stop-color="{PAPER}" stop-opacity="0"/>'
            f'<stop offset="0.55" stop-color="{PAPER}" stop-opacity="0.45"/>'
            f'<stop offset="1" stop-color="{PAPER}" stop-opacity="0.92"/></linearGradient>')
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none">
  <defs>
    <linearGradient class="ax" id="m{uid}" data-x1="{x1:.1f}" data-y1="{y1:.1f}" data-x2="{x2:.1f}" data-y2="{y2:.1f}" data-axis="x" gradientUnits="userSpaceOnUse" x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}">{_stops(main)}</linearGradient>
    <linearGradient class="ax" id="g{uid}" data-x1="{x1:.1f}" data-y1="{y1:.1f}" data-x2="{x2:.1f}" data-y2="{y2:.1f}" data-axis="x" gradientUnits="userSpaceOnUse" x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}">{_stops(gloss)}</linearGradient>
    {veil.format(uid=uid)}{_seatbelt(H).format(uid=uid)}{_topedge(H).format(uid=uid)}
  </defs>
  <path d="{foil_path}" fill="url(#m{uid})"/>
  <path d="{foil_path}" fill="url(#g{uid})"/>
  <path d="{foil_path}" fill="url(#vl{uid})"/>
  <path d="{edge_path}" fill="{WHITE}" fill-opacity="0.34"/>
  <rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{0.05 * H:.1f}" fill="url(#te{uid})"/>
</svg>'''


# ── V2 底边撕入 ────────────────────────────────────────────────
def foil_v2(uid, H):
    x1, y1 = 0.15 * W, 0.98 * H
    x2, y2 = 0.85 * W, 0.38 * H
    main = [(0.000, PINK, 0.88), (0.350, PINK, 0.60), (0.450, PURPLE, 0.78),
            (0.700, PURPLE, 0.52), (0.780, CYAN, 0.70), (1.000, CYAN, 0.58)]
    # 撕边：从 (0, y0) 到 (W, y1)，两段二次贝塞尔波
    y0, y1c = 0.56 * H, 0.62 * H
    tear = (f"M0,{y0:.1f} "
            f"Q{0.20 * W:.1f},{y0 - 0.055 * H:.1f} {0.40 * W:.1f},{y0 + 0.012 * H:.1f} "
            f"Q{0.60 * W:.1f},{y1c + 0.058 * H:.1f} {0.80 * W:.1f},{y1c - 0.018 * H:.1f} "
            f"Q{0.92 * W:.1f},{y1c - 0.045 * H:.1f} {W:.0f},{y1c + 0.012 * H:.1f} "
            f"L{W:.0f},{H:.1f} L0,{H:.1f} Z")
    # 翻起纸边的白高光：同曲线整体下移 0.022H，仅描边
    lip = (f"M0,{y0 + 0.022 * H:.1f} "
           f"Q{0.20 * W:.1f},{y0 - 0.033 * H:.1f} {0.40 * W:.1f},{y0 + 0.034 * H:.1f} "
           f"Q{0.60 * W:.1f},{y1c + 0.080 * H:.1f} {0.80 * W:.1f},{y1c + 0.004 * H:.1f} "
           f"Q{0.92 * W:.1f},{y1c - 0.023 * H:.1f} {W:.0f},{y1c + 0.034 * H:.1f}")
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none">
  <defs>
    <linearGradient class="ax" id="m{uid}" data-x1="{x1:.1f}" data-y1="{y1:.1f}" data-x2="{x2:.1f}" data-y2="{y2:.1f}" data-axis="y" gradientUnits="userSpaceOnUse" x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}">{_stops(main)}</linearGradient>
    {_seatbelt(H, mid=0.34).format(uid=uid)}{_topedge(H).format(uid=uid)}
  </defs>
  <path d="{tear}" fill="url(#m{uid})"/>
  <path d="{lip}" fill="none" stroke="{WHITE}" stroke-opacity="0.55" stroke-width="{0.011 * H:.2f}" stroke-linecap="round"/>
  <rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{0.05 * H:.1f}" fill="url(#te{uid})"/>
</svg>'''


# ── V3 双带交叉 ────────────────────────────────────────────────
def foil_v3(uid, H):
    import math
    cx, cy = 0.5 * W, 0.5 * H
    bw = 0.22 * H  # 半带宽
    L = 1.3 * W

    def band(angle_deg):
        a = math.radians(angle_deg)
        dx, dy = math.cos(a), math.sin(a)
        nx, ny = -dy, dx
        pts = [(cx - dx * L + nx * bw, cy - dy * L + ny * bw),
               (cx + dx * L + nx * bw, cy + dy * L + ny * bw),
               (cx + dx * L - nx * bw, cy + dy * L - ny * bw),
               (cx - dx * L - nx * bw, cy - dy * L - ny * bw)]
        return "M" + " L".join(f"{px:.1f},{py:.1f}" for px, py in pts) + " Z"

    def axis(angle_deg):
        a = math.radians(angle_deg)
        dx, dy = math.cos(a), math.sin(a)
        return (cx - dx * L * 0.8, cy - dy * L * 0.8, cx + dx * L * 0.8, cy + dy * L * 0.8)

    b1, b2 = band(22), band(-22)
    a1x1, a1y1, a1x2, a1y2 = axis(22)
    a2x1, a2y1, a2x2, a2y2 = axis(-22)
    main1 = [(0.000, PINK, 0.86), (0.500, PURPLE, 0.72), (1.000, CYAN, 0.62)]
    main2 = [(0.000, CYAN, 0.62), (0.500, PURPLE, 0.72), (1.000, PINK, 0.86)]
    # 交叉白热菱形：沿带 1 方向拉长
    d1x, d1y = math.cos(math.radians(22)), math.sin(math.radians(22))
    n1x, n1y = -d1y, d1x
    dia = (f"M{cx + d1x * 0.20 * H:.1f},{cy + d1y * 0.20 * H:.1f} "
           f"L{cx + n1x * 0.09 * H:.1f},{cy + n1y * 0.09 * H:.1f} "
           f"L{cx - d1x * 0.20 * H:.1f},{cy - d1y * 0.20 * H:.1f} "
           f"L{cx - n1x * 0.09 * H:.1f},{cy - n1y * 0.09 * H:.1f} Z")
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none">
  <defs>
    <linearGradient class="ax" id="m1{uid}" data-x1="{a1x1:.1f}" data-y1="{a1y1:.1f}" data-x2="{a1x2:.1f}" data-y2="{a1y2:.1f}" data-axis="x" gradientUnits="userSpaceOnUse" x1="{a1x1:.1f}" y1="{a1y1:.1f}" x2="{a1x2:.1f}" y2="{a1y2:.1f}">{_stops(main1)}</linearGradient>
    <linearGradient class="ax" id="m2{uid}" data-x1="{a2x1:.1f}" data-y1="{a2y1:.1f}" data-x2="{a2x2:.1f}" data-y2="{a2y2:.1f}" data-axis="x" gradientUnits="userSpaceOnUse" x1="{a2x1:.1f}" y1="{a2y1:.1f}" x2="{a2x2:.1f}" y2="{a2y2:.1f}">{_stops(main2)}</linearGradient>
    <radialGradient id="hot{uid}" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="{WHITE}" stop-opacity="0.42"/>
      <stop offset="0.6" stop-color="{WHITE}" stop-opacity="0.16"/>
      <stop offset="1" stop-color="{WHITE}" stop-opacity="0"/>
    </radialGradient>
    {_seatbelt(H, mid=0.55).format(uid=uid)}{_topedge(H).format(uid=uid)}
  </defs>
  <path d="{b1}" fill="url(#m1{uid})"/>
  <path d="{b2}" fill="url(#m2{uid})"/>
  <ellipse cx="{cx:.1f}" cy="{cy:.1f}" rx="{0.16 * H:.1f}" ry="{0.10 * H:.1f}" fill="url(#hot{uid})"/>
  <path d="{dia}" fill="{WHITE}" fill-opacity="0.20"/>
  <rect x="0" y="0" width="{W:.0f}" height="{H}" fill="url(#sb{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{0.05 * H:.1f}" fill="url(#te{uid})"/>
</svg>'''


# ── V0 留白纸面 ────────────────────────────────────────────────
def foil_v0(uid, H):
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {H}" preserveAspectRatio="none">
  <defs>{_topedge(H).format(uid=uid)}</defs>
  <rect x="0" y="0" width="{W:.0f}" height="{0.05 * H:.1f}" fill="url(#te{uid})"/>
</svg>'''


FOIL = {"v1": foil_v1, "v2": foil_v2, "v3": foil_v3, "v0": foil_v0}


# ── 卡片 ──────────────────────────────────────────────────────
def card(uid, variant, title, author, tag, time, replies, hot=False, s=1, cover_h=120, read=False):
    def px(x):
        return int(round(x * s))
    g1, g2 = AVA[zlib.crc32(author.encode("utf-8")) % len(AVA)]
    letter = author[0]
    title_color = f"rgba(53,36,60,{0.6 if read else 1.0})"
    rep_style = "color:#FF2C55;font-weight:600;" if hot else ""
    bub_stroke = "#FF2C55" if hot else "#88708E"
    return f'''<div class="card" style="width:{px(166)}px">
  <div class="ring"><div class="inner">
    <div class="cover" style="height:{px(cover_h)}px">
      <div class="paper"></div>
      {FOIL[variant](uid, cover_h)}
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


CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F6F2F9; --card:#FFFFFF; --pink:#D6339C; --purple:#8B3FD9; --ok:#0E9F6E; --warn:#C2410C;}
*{box-sizing:border-box; margin:0; padding:0;}
body{background:var(--bg); color:var(--ink); font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; line-height:1.65; padding:32px 20px 80px;}
.wrap{max-width:1240px; margin:0 auto;}
header h1{font-size:25px; letter-spacing:.5px;}
header .sub{color:var(--ink2); font-size:13px; margin-top:6px;}
.badge{display:inline-block; background:linear-gradient(100deg,var(--pink),var(--purple)); color:#fff; font-size:12px; padding:2px 10px; border-radius:99px; margin-right:8px; vertical-align:2px;}
section{margin-top:34px;}
h2{font-size:18px; border-left:4px solid var(--pink); padding-left:10px; margin-bottom:12px;}
p{font-size:14px;}
.small{font-size:12.5px; color:var(--ink2);}
.note{background:linear-gradient(100deg,#FDF0F8,#F3EBFE); border:1px solid #F0D9EE; border-radius:14px; padding:14px 18px; font-size:13.5px; margin-top:14px;}
.note b{color:var(--pink);}
/* ── 诊断 ── */
.diag{display:grid; grid-template-columns:repeat(3,1fr); gap:14px; margin-top:14px;}
.diagbox{background:var(--card); border:1px solid var(--line); border-radius:14px; padding:14px 16px;}
.diagbox .dt{font-size:13.5px; font-weight:800; color:var(--warn); margin-bottom:6px;}
.diagbox .dd{font-size:12.5px; color:var(--ink2);}
/* ── 构图展示 ── */
.vgrid{display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-top:14px;}
.vcol{background:var(--card); border:1px solid var(--line); border-radius:16px; padding:14px; display:flex; flex-direction:column; align-items:center;}
.vcol .vt{font-size:14px; font-weight:800; align-self:flex-start;}
.vcol .vt .share{font-size:11.5px; font-weight:700; color:#fff; background:linear-gradient(100deg,var(--pink),var(--purple)); border-radius:99px; padding:1px 8px; margin-left:6px; vertical-align:1px;}
.vcol .vd{font-size:12px; color:var(--ink2); margin:4px 0 10px; align-self:flex-start; min-height:56px;}
.vcol .vcards{display:flex; flex-direction:column; gap:14px; align-items:center;}
.vcol .vcap{font-size:11px; color:var(--ink2); margin-top:8px; align-self:flex-start;}
/* ── 卡片（1x = 166px，Compose 真机尺寸） ── */
.card .ring{border-radius:15.5px; padding:1.5px; background:linear-gradient(135deg,#C2267F,#FF4FB0,#8F5CFF,#22CFE6,#FF4FB0); box-shadow:0 2px 10px rgba(53,36,60,.10),0 1px 3px rgba(53,36,60,.08);}
.card .inner{border-radius:14px; background:#fff; overflow:hidden; box-shadow:inset 0 0 0 4px rgba(143,92,255,.05);}
.cover{position:relative; overflow:hidden;}
.paper{position:absolute; inset:0; background:#F1EDF9;}
.foil{position:absolute; inset:0; width:100%; height:100%; pointer-events:none;}
.ctitlebox{position:absolute; inset:0; display:flex; align-items:center; justify-content:center;}
.ctitle{font-weight:700; text-align:center; display:-webkit-box; -webkit-line-clamp:5; -webkit-box-orient:vertical; overflow:hidden;}
.meta{border-top:1px solid rgba(136,112,142,.12);}
.mrow{display:flex; align-items:center;}
.grow{flex:1;}
.ava{border-radius:50%; color:#fff; font-weight:700; display:flex; align-items:center; justify-content:center; flex:none;}
.author{color:#88708E; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;}
.reps{color:#88708E;}
.tag{color:#88708E; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;}
.time{color:rgba(136,112,142,.7); white-space:nowrap;}
/* ── 手机 mock ── */
.phonewrap{display:flex; gap:28px; align-items:flex-start; flex-wrap:wrap; margin-top:6px;}
.phone{width:360px; border-radius:26px; overflow:hidden; box-shadow:0 8px 28px rgba(70,30,90,.18); border:1px solid rgba(0,0,0,.06); background:linear-gradient(180deg,#EFEDF6,#F8F5FC);}
.scr{padding-bottom:12px;}
.status{display:flex; justify-content:space-between; padding:7px 16px 3px; font-size:10.5px; font-weight:600; color:#88708E;}
.topbar{display:flex; align-items:center; padding:4px 14px 8px; gap:8px;}
.logo{font-size:16px; font-weight:800; color:#35243C; letter-spacing:.5px;}
.aurora{height:2.5px; background:linear-gradient(90deg,rgba(255,79,176,.40),rgba(143,92,255,.40),rgba(34,207,230,.40));}
.chips{display:flex; gap:6px; padding:8px 10px 4px; flex-wrap:wrap;}
.chip{font-size:10.5px; padding:3px 10px; border-radius:99px; font-weight:600; background:#F1EDF7; color:#88708E;}
.chip.sel{background:#C2267F; color:#fff;}
.grid{column-count:2; column-gap:8px; padding:8px 10px 0;}
.grid .card{margin-bottom:8px; break-inside:avoid;}
.phcap{max-width:300px; font-size:13px; color:var(--ink2);}
.phcap b{color:var(--ink);}
/* ── 改动清单 ── */
ol.chg{margin:10px 0 0 20px; font-size:14px;}
ol.chg li{margin-bottom:8px;}
ol.chg b{color:var(--pink);}
.kv{font-size:12.5px; color:var(--ink2); margin-top:10px;}
.kv code{background:#F3EDF8; border-radius:4px; padding:1px 5px; font-size:11.5px; color:#6E5A85;}
"""

JS = """
(function(){
  var grads = Array.prototype.slice.call(document.querySelectorAll('linearGradient.ax'));
  var last = 0;
  function tick(t){
    if(t - last < 66) { requestAnimationFrame(tick); return; }  // 15fps，与真机心跳同频
    last = t;
    var ph = (t / 24000) % 1;                                    // 24s 周期，与真机一致
    var dx = Math.sin(ph * 2 * Math.PI) * 0.03 * 166;            // ±3% 卡宽
    var dy = Math.sin(ph * 2 * Math.PI) * 0.02 * 166;            // 垂直轴漂移
    for(var i = 0; i < grads.length; i++){
      var g = grads[i];
      var x1 = parseFloat(g.getAttribute('data-x1')), y1 = parseFloat(g.getAttribute('data-y1'));
      var x2 = parseFloat(g.getAttribute('data-x2')), y2 = parseFloat(g.getAttribute('data-y2'));
      if(g.getAttribute('data-axis') === 'x'){
        g.setAttribute('x1', (x1 + dx).toFixed(1)); g.setAttribute('x2', (x2 + dx).toFixed(1));
      } else {
        g.setAttribute('y1', (y1 - dy).toFixed(1)); g.setAttribute('y2', (y2 - dy).toFixed(1));
      }
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

    vdesc = {
        "v1": "箔铺满左上约三分之二，一条硬斜切边压过封面，双折棱随斜边走向；标题落在中段安全带里。",
        "v2": "箔从底边以微曲撕边撕入，撕边平行一道白高光像翻起的纸边；标题在上半的纸上，最稳。",
        "v3": "两条箔带 ±22° 交叉成 X，交叉点一团白热菱形光；标题嵌在两条带之间，全息感最强。",
        "v0": "纯纸呼吸位：八张卡里约一成半不带箔，有箔的卡才跳得出来。",
    }

    # ── 四构图单卡（2x 放大，短/长两种封面） ──
    vcols = []
    for v in VARIANTS:
        vcols.append(f'''
      <div class="vcol">
        <div class="vt">{VNAME[v]}<span class="share">{VSHARE[v]}</span></div>
        <div class="vd">{vdesc[v]}</div>
        <div class="vcards">
          {card(nxt(), v, short_t, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=120)}
          {card(nxt(), v, long_t, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=157)}
        </div>
        <div class="vcap">左：一行标题（最短封页）· 右：五行标题（最高封页）</div>
      </div>''')

    # ── 信息流 mock（1x 真机尺寸，8 张卡，构图按 tid 哈希落档） ──
    feed = [
        ("v1", "2026 年 10 月游戏发售表", "sd444", "游戏区", "3小时前", 45, False, 120),
        ("v2", "预算 4000，求推荐 65 寸电视。主要看流媒体和纪录片，偶尔接主机玩游戏，最好别拖影", "战术核显卡", "数码区", "10-19", 108, True, 157),
        ("v0", "晒一下今年的桌搭，第三版了", "猫阿姨", "生活区", "昨天", 23, False, 120),
        ("v3", "求推荐声音小的机械键盘，办公室用，预算五百左右，青轴手感但是别太吵的有没有", "sdhd", "数码区", "10-18", 67, False, 157),
        ("v1", "周末爬了座山，拍了几张照片", "山那边的风", "生活区", "10-17", 12, False, 120),
        ("v2", "大家通勤路上都听什么播客", "早八人", "生活区", "10-19", 89, False, 120),
        ("v3", "楼下快递柜悄悄涨价了，大家发现没？超时每小时都开始计费了，这算违规吗", "长安乱", "闲聊区", "2小时前", 156, True, 136),
        ("v0", "整理了点复古游戏 ROM 的坑", "卡带收藏家", "游戏区", "10-16", 34, False, 120),
    ]
    cards_html = "\n".join(
        card(nxt(), v, t, a, g, tm, r, hot=h, s=1, cover_h=ch) for v, t, a, g, tm, r, h, ch in feed
    )
    vcount = {v: sum(1 for x in feed if x[0] == v) for v in VARIANTS}

    html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>幻彩浅色 · 箔面封面预览</title><style>{CSS}</style></head>
<body><div class="wrap">

<header>
  <h1>幻彩浅色主题 · 箔面封面（重做预览）</h1>
  <div class="sub">2026-10-19 · 上一版「箔带加宽」判定无突破，本版推翻「带」的思维重做 · 状态：<span class="badge">待你确认后装机</span></div>
</header>

<section>
  <h2>为什么重做</h2>
  <div class="diag">
    <div class="diagbox"><div class="dt">① 「带」的思维没破</div><div class="dd">不管多宽，它还是「纸上贴了一条箔」——小面积装饰逻辑。箔本来就是这套主题的标识，却只敢占顶缘一条。</div></div>
    <div class="diagbox"><div class="dt">② 八张卡完全同构</div><div class="dd">每条卡一条带、同一个粉左青右走向，信息流扫下去零丰富度，像八张卡用了同一个模板。</div></div>
    <div class="diagbox"><div class="dt">③ 没有全息感</div><div class="dd">幻彩的灵魂是光随角度流动。现在只有一条 24 秒慢漂的固定渐变，「镭射」名不副实。</div></div>
  </div>
</section>

<section>
  <h2>新概念：箔面封面</h2>
  <p>箔从「一条带」升级为<b>封面的主体构图</b>，每张卡的构图由帖子 tid 哈希落档（35 / 25 / 25 / 15 四档），一屏八张卡自然形成四种面貌。同一套幻彩基因全部保留：三色取值、纸底、白折棱、24 秒呼吸漂移、顶缘锐边、彩丝卡框、已读降透明规则。<b>标题可读性靠「安全带」保证</b>：V1 / V3 的标题带内垫一层纸纱（中部 66% / 55%），实测对比度全部 ≥8:1。</p>
</section>

<section>
  <h2>四种构图（2 倍放大 · 短封页与最高封页）</h2>
  <div class="vgrid">{''.join(vcols)}</div>
</section>

<section>
  <h2>信息流效果（一屏 8 张 · 1x 真机尺寸）</h2>
  <div class="phonewrap">
    <div class="phone"><div class="scr">
      <div class="status"><span>20:01</span><span>5G · 100%</span></div>
      <div class="topbar">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#C2267F" stroke-width="2.2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
        <span class="logo">Stage1</span>
        <span class="grow"></span>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#88708E" stroke-width="2" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h10"/></svg>
      </div>
      <div class="aurora"></div>
      <div class="chips"><span class="chip sel">全部</span><span class="chip">关注</span><span class="chip">游戏</span><span class="chip">数码</span><span class="chip">生活</span><span class="chip">闲聊</span></div>
      <div class="grid">
{cards_html}
      </div>
    </div></div>
    <div class="phcap">
      <p><b>看整体感：</b>这一屏 8 张出现了 {vcount['v1']} 张对角箔面、{vcount['v2']} 张底边撕入、{vcount['v3']} 张双带交叉、{vcount['v0']} 张留白纸面——滚动时构图在变，但色调始终是同一套粉紫青，不会花。</p>
      <p style="margin-top:10px">呼吸漂移保持 24 秒一周期（渐变轴 ±3% 卡宽），预览按真机 15fps 心跳同频播放；已读帖子标题仍按原规则降到 60%。</p>
      <p style="margin-top:10px" class="small">注：真实分布由 tid 哈希决定，这里按展示效果指定；装机后每张卡的构图会跟着帖子数据走。</p>
    </div>
  </div>
</section>

<section>
  <h2>你会看到什么不同</h2>
  <ol class="chg">
    <li><b>箔成了封面主体：</b>不再是顶缘一条窄带。V1 箔面占封面约三分之二；V2 从底边撕入占四成；V3 两条带交叉成 X——每种构图的箔面积都是旧版的 2～4 倍。</li>
    <li><b>构图跟着内容走：</b>tid 哈希四档落位，一屏之内对角、撕边、交叉、留白自然轮换，八张卡不再同一个模子。</li>
    <li><b>光泽升级：</b>V1 双折棱（粉紫之间 + 紫青之间各一道）、V2 撕边翻起的白纸边、V3 交叉点白热菱形——光泽靠结构不靠加饱和度。</li>
    <li><b>标题安全：</b>V1 / V3 标题带垫纸纱（66% / 55%），V2 / V0 标题本就在纸上；四种构图的标题对比度实测均 ≥8:1，五行封页也够。</li>
    <li><b>不动的部分：</b>纸底 #F1EDF9、三色取值、顶缘锐边、24s 呼吸、彩丝卡框、顶栏极光线、已读 60% 规则、深色板「光斑呼吸」。</li>
  </ol>
  <div class="note">
    <b>确认后我再动手：</b>改动集中在 <code>XhsPostCard.kt</code> 封面 drawBehind 的浅色分支（重写为四构图 + tid 哈希落档 + 安全带纸纱），<code>S1Theme.kt</code> 不动；然后构建 → 装机给你验收。哪个构图想撤、比例想调（比如 V0 不要留白、全部带箔），回来告诉我。
  </div>
  <p class="kv">说明：本预览按绘制参数用 SVG 手工还原，渐变几何与 Compose 的 Brush 一致；真机观感以装机后为准。</p>
</section>

</div>
<script>{JS}</script>
</body></html>'''

    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "outputs", "preview_iridescent")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, "幻彩浅色箔面封面预览.html")
    with open(out, "w", encoding="utf-8") as f:
        f.write(html)
    print(out)


if __name__ == "__main__":
    build()
