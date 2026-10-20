#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
幻彩浅色主题 · 箔带加宽预览生成器（2026-10-19）
================================================
问题：现行箔带（XhsPostCard.kt style 5 浅色分支）颜色只铺到封面左上角——
      渐变 54% 后alpha已掉到 .34、76% 后 .16、98% 透明，右半张卡只剩纸色；
      底部收边又从 34% 高度就开始压，实色区只剩最顶一小条。

新版：三色通栏——粉(左)/紫(中)/青(右)一路铺到卡片右边缘，右端保持 ~20%
      透明度被卡边裁齐（像真箔纸贴上去）；折棱与棱上白高光保留；
      底部收边推迟到 55% 高度，整条带子是「实」的。

停靠点参数与 Compose 实现一一对应（offset 沿渐变轴归一化），
预览用 SVG userSpaceOnUse 渐变忠实还原 Brush.linearGradient 的几何。

输出：outputs/preview_iridescent/幻彩浅色箔带加宽预览.html
"""
import os
import zlib

PINK, PURPLE, CYAN, PAPER, WHITE = "#FF4FB0", "#8F5CFF", "#22CFE6", "#F1EDF9", "#FFFFFF"

# ── 箔带参数（与 XhsPostCard.kt 浅色分支一致的结构） ──────────────────────
CURRENT = dict(
    main=[(0.000, PINK, 0.82), (0.160, PINK, 0.52), (0.300, PURPLE, 0.68), (0.400, PURPLE, 0.40),
          (0.415, CYAN, 0.62), (0.540, CYAN, 0.34), (0.760, CYAN, 0.16), (0.980, CYAN, 0.00)],
    gloss=[(0.392, WHITE, 0.00), (0.402, WHITE, 0.58), (0.414, WHITE, 0.00),
           (0.600, WHITE, 0.24), (0.630, WHITE, 0.00)],
    veil=0.34, edge=0.50, label="现状",
)
NEW = dict(
    main=[(0.000, PINK, 0.88), (0.090, PINK, 0.78), (0.170, PINK, 0.55), (0.235, PURPLE, 0.74),
          (0.345, PURPLE, 0.44), (0.357, CYAN, 0.68), (0.540, CYAN, 0.60), (0.760, CYAN, 0.34),
          (0.900, CYAN, 0.24), (0.985, CYAN, 0.20)],
    gloss=[(0.346, WHITE, 0.00), (0.352, WHITE, 0.58), (0.358, WHITE, 0.00),
           (0.660, WHITE, 0.20), (0.690, WHITE, 0.00)],
    veil=0.55, edge=0.50, label="新版",
)

W = 166.0  # 卡片宽 1x（360dp 屏：双列 + 10dp 页边距 + 8dp 列缝）

AVA = [("#FF9A9E", "#FECFEF"), ("#A18CD1", "#FBC2EB"), ("#84FAB0", "#8FD3F4"),
       ("#FCCF90", "#F9A8B8"), ("#5EE7DF", "#B490CA"), ("#F6D365", "#FDA085"),
       ("#C2FFD8", "#A6C1EE"), ("#F093FB", "#F5576C")]


def band_h(cover_h):
    """与 Compose 一致：min(h*0.34, 64dp)"""
    return min(cover_h * 0.34, 64.0)


def t_to_pct(t):
    """渐变轴归一化坐标 t → 卡片宽度百分比（轴 -10%w → 98%w，即轴跨 108% 宽）"""
    return max(0.0, min(100.0, (t - 0.0884) / 0.885 * 100.0))


def foil_svg(v, uid, cover_h):
    """还原 Compose drawBehind：clipRect(bandH){ 主渐变 + 折棱高光 + 纸色收边 } + 顶缘锐边"""
    bh = band_h(cover_h)
    x1, x2, y2 = -0.10 * W, 0.98 * W, 0.30 * cover_h
    stops = "".join(f'<stop offset="{o:.3f}" stop-color="{c}" stop-opacity="{a:.2f}"/>' for o, c, a in v["main"])
    gstops = "".join(f'<stop offset="{o:.3f}" stop-color="{c}" stop-opacity="{a:.2f}"/>' for o, c, a in v["gloss"])
    vy1 = v["veil"] * bh
    return f'''<svg class="foil" viewBox="0 0 {W:.0f} {cover_h}" preserveAspectRatio="none">
  <defs>
    <linearGradient class="ax" id="m{uid}" gradientUnits="userSpaceOnUse" x1="{x1:.1f}" y1="0" x2="{x2:.1f}" y2="{y2:.1f}">{stops}</linearGradient>
    <linearGradient class="ax" id="g{uid}" gradientUnits="userSpaceOnUse" x1="{x1:.1f}" y1="0" x2="{x2:.1f}" y2="{y2:.1f}">{gstops}</linearGradient>
    <linearGradient id="v{uid}" gradientUnits="userSpaceOnUse" x1="0" y1="{vy1:.1f}" x2="0" y2="{bh:.1f}">
      <stop offset="0" stop-color="{PAPER}" stop-opacity="0"/><stop offset="0.5" stop-color="{PAPER}" stop-opacity="0.55"/><stop offset="1" stop-color="{PAPER}" stop-opacity="1"/>
    </linearGradient>
    <linearGradient id="e{uid}" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="{0.05 * cover_h:.1f}">
      <stop offset="0" stop-color="#FFFFFF" stop-opacity="{v['edge']:.2f}"/><stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
    </linearGradient>
  </defs>
  <rect x="0" y="0" width="{W:.0f}" height="{bh:.1f}" fill="url(#m{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{bh:.1f}" fill="url(#g{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{bh:.1f}" fill="url(#v{uid})"/>
  <rect x="0" y="0" width="{W:.0f}" height="{0.05 * cover_h:.1f}" fill="url(#e{uid})"/>
</svg>'''


def card(v, uid, title, author, tag, time, replies, hot=False, s=1, cover_h=120, read=False):
    def px(x):
        return int(round(x * s))
    g1, g2 = AVA[zlib.crc32(author.encode("utf-8")) % len(AVA)]
    letter = author[0]
    title_color = f"rgba(46,27,56,{0.6 if read else 1.0})"
    rep_style = "color:#FF2C55;font-weight:600;" if hot else ""
    bub_stroke = "#FF2C55" if hot else "#88708E"
    return f'''<div class="card" style="width:{px(166)}px">
  <div class="ring"><div class="inner">
    <div class="cover" style="height:{px(cover_h)}px">
      <div class="paper"></div>
      {foil_svg(v, uid, cover_h)}
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


def cov_bar(v):
    """沿卡片宽度的箔色覆盖示意条"""
    segs = []
    for i, (o, c, a) in enumerate(v["main"]):
        p = t_to_pct(o)
        rgba = f"rgba({int(c[1:3],16)},{int(c[3:5],16)},{int(c[5:7],16)},{a:.2f})"
        if i == 0:
            segs.append(f"{rgba} 0%")
        else:
            segs.append(f"{rgba} {p:.0f}%")
    return f'<div class="covbar"><div class="covfill" style="background:linear-gradient(90deg,{",".join(segs)})"></div></div>'


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
/* ── 覆盖示意 ── */
.cov{margin-top:14px; display:flex; flex-direction:column; gap:10px;}
.covrow{display:flex; align-items:center; gap:12px; flex-wrap:wrap;}
.covlab{font-size:13px; font-weight:800; width:38px;}
.covbar{width:420px; height:26px; border-radius:6px; background:repeating-linear-gradient(45deg,#F0EAF5 0 6px,#E9E1F0 6px 12px); border:1px solid var(--line); overflow:hidden;}
.covfill{height:100%;}
.covtxt{font-size:12.5px; color:var(--ink2);}
/* ── 单卡对比 ── */
.pairgrid{display:grid; grid-template-columns:1fr 1fr; gap:20px; margin-top:14px;}
.pair{background:var(--card); border:1px solid var(--line); border-radius:16px; padding:16px; display:flex; flex-direction:column; align-items:center;}
.pair .ptag{font-size:14px; font-weight:800; margin-bottom:2px; align-self:flex-start;}
.pair .ptag .no{display:inline-block; width:22px; height:22px; border-radius:50%; color:#fff; text-align:center; line-height:22px; font-size:12px; margin-right:7px;}
.pair .pdesc{font-size:12.5px; color:var(--ink2); margin-bottom:10px; align-self:flex-start; min-height:38px;}
.pair.old .ptag{color:var(--warn);} .pair.old .ptag .no{background:#B084B8;}
.pair.new .ptag{color:var(--pink);} .pair.new .ptag .no{background:var(--pink);}
.pair.new{background:linear-gradient(180deg,#FFF7FC,rgba(255,255,255,0)); border:1px solid #F4D9EC;}
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
  var W = 166;
  var grads = Array.prototype.slice.call(document.querySelectorAll('linearGradient.ax'));
  var last = 0;
  function tick(t){
    if(t - last < 66) { requestAnimationFrame(tick); return; }  // 15fps，与真机心跳同频
    last = t;
    var ph = (t / 24000) % 1;                                    // 24s 周期，与真机一致
    var drift = Math.sin(ph * 2 * Math.PI) * 0.03 * W;           // ±3% 宽度
    for(var i = 0; i < grads.length; i++){
      grads[i].setAttribute('x1', (-0.10 * W + drift).toFixed(1));
      grads[i].setAttribute('x2', ( 0.98 * W + drift).toFixed(1));
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

    # ── 单卡对比（2 倍放大，SVG 内部坐标保持 1x） ──
    short_t = "通勤听什么播客"
    long_t = "有人用 mini LED 当显示器办公吗？想入个 27 寸的，主要写代码看文档，偶尔修图，预算三千左右，求推荐型号"

    pairs = []
    for cover_h, title, cap_old, cap_new in [
        (120, short_t, "一行标题：箔带高约 41dp，颜色集中在左上一小块，右半张是纸色。",
         "一行标题：同高度下三色通栏，右端低透明度直接被卡边裁齐。"),
        (157, long_t, "五行标题（最高封页）：标题第二行往右就压在纸色上，箔带存在感最弱。",
         "五行标题：整条封面顶缘都是箔，标题深字压在箔上对比度仍 ≥5:1。"),
    ]:
        pairs.append(f'''
      <div class="pair old">
        <div class="ptag"><span class="no">旧</span>现状 · {'一行' if cover_h == 120 else '五行'}标题</div>
        <div class="pdesc">{cap_old}</div>
        {card(CURRENT, nxt(), title, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=cover_h)}
      </div>''')
        pairs.append(f'''
      <div class="pair new">
        <div class="ptag"><span class="no">新</span>新版 · {'一行' if cover_h == 120 else '五行'}标题</div>
        <div class="pdesc">{cap_new}</div>
        {card(NEW, nxt(), title, "猫阿姨", "生活区", "10-19", 108, hot=True, s=2, cover_h=cover_h)}
      </div>''')

    # ── 信息流 mock（1x 真机尺寸，8 张卡） ──
    feed = [
        ("2026 年 10 月游戏发售表", "sd444", "游戏区", "3小时前", 45, False, 120),
        ("预算 4000，求推荐 65 寸电视。主要看流媒体和纪录片，偶尔接主机玩游戏，最好别拖影", "战术核显卡", "数码区", "10-19", 108, True, 157),
        ("晒一下今年的桌搭，第三版了", "猫阿姨", "生活区", "昨天", 23, False, 120),
        ("求推荐声音小的机械键盘，办公室用，预算五百左右，青轴手感但是别太吵的有没有", "sdhd", "数码区", "10-18", 67, False, 157),
        ("周末爬了座山，拍了几张照片", "山那边的风", "生活区", "10-17", 12, False, 120),
        ("大家通勤路上都听什么播客", "早八人", "生活区", "10-19", 89, False, 120),
        ("楼下快递柜悄悄涨价了，大家发现没？超时每小时都开始计费了，这算违规吗", "长安乱", "闲聊区", "2小时前", 156, True, 136),
        ("整理了点复古游戏 ROM 的坑", "卡带收藏家", "游戏区", "10-16", 34, False, 120),
    ]
    cards_html = "\n".join(
        card(NEW, nxt(), t, a, g, tm, r, hot=h, s=1, cover_h=ch) for t, a, g, tm, r, h, ch in feed
    )

    html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>幻彩浅色 · 箔带加宽预览</title><style>{CSS}</style></head>
<body><div class="wrap">

<header>
  <h1>幻彩浅色主题 · 箔带加宽预览</h1>
  <div class="sub">2026-10-19 · 问题：箔带只贴在左上角 · 方案：三色通栏箔带 · 状态：<span class="badge">待你确认后装机</span></div>
</header>

<section>
  <h2>现状诊断</h2>
  <p>现行箔带（XhsPostCard.kt · style 5 浅色分支）的渐变在 <b>54%</b> 处青色 alpha 已掉到 0.34、<b>76%</b> 后只剩 0.16、<b>98%</b> 完全透明——右半张卡实际是纸色；同时底部从 <b>34%</b> 高度就被纸色盖住，实色区只剩最顶一小条。两相叠加，就是「只有左上角」。</p>
  <div class="cov">
    <div class="covrow"><span class="covlab" style="color:var(--warn)">现状</span>{cov_bar(CURRENT)}<span class="covtxt">箔色 0–45% 有 → 45% 起淡出 → 76% 后只剩纸色</span></div>
    <div class="covrow"><span class="covlab" style="color:var(--pink)">新版</span>{cov_bar(NEW)}<span class="covtxt">箔色 0–100% 通栏 → 右端保持 20% 透明度被卡边裁齐</span></div>
  </div>
</section>

<section>
  <h2>单卡对比（2 倍放大 · 封面高度取最短与最长两种）</h2>
  <div class="pairgrid">{''.join(pairs)}</div>
</section>

<section>
  <h2>信息流效果（新版 · 一屏 8 张，1x 真机尺寸）</h2>
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
      <p><b>看一屏的整体感：</b>8 张卡同屏时，每条箔带都是通栏的，颜色走向一致（粉左 / 紫中 / 青右），像一排贴齐的镭射纸；不会再出现「一半卡有箔、一半卡没有」的参差。</p>
      <p style="margin-top:10px">顶栏下缘那条 2.5dp 三色细线（极光线）与箔带同色系，是这套主题已有的元素，本次不动。</p>
      <p style="margin-top:10px" class="small">动画：箔带保留 24 秒一周期的呼吸漂移（±3% 卡宽），预览里已按真机 15fps 心跳同频播放。</p>
    </div>
  </div>
</section>

<section>
  <h2>你会看到什么不同</h2>
  <ol class="chg">
    <li><b>通栏：</b>粉（左）→ 紫（中）→ 青（右）一路铺到卡片右边缘；右端不再淡没，而是保持约 20% 透明度被卡边裁齐——看起来是箔纸从左贴到右，不是贴了一角。</li>
    <li><b>折棱保留：</b>紫转青之间那道硬折边（箔片压过的棱）随新布局落在中段，斜着压过箔带；棱上的白色高光线也保留，光泽仍然靠这条线不靠加饱和度。</li>
    <li><b>下半幅保色：</b>旧版从 34% 高度就被纸色盖住，新版扛到 55% 高度才软化收边——整条带子是「实」的，不再只有最顶一条细线。</li>
    <li><b>不动的部分：</b>纸底 #F1EDF9、顶缘裁切锐边、呼吸漂移、深色板的「光斑呼吸」、三色取值（粉 #FF4FB0 / 紫 #8F5CFF / 青 #22CFE6）、已读标题降透明度规则。</li>
    <li><b>标题安全：</b>深色标题压在箔带上的对比度全部 ≥5:1（粉色最重区约 6.2:1），长标题五行封页也够。</li>
  </ol>
  <div class="note">
    <b>确认后我再动手：</b>改动只在 <code>XhsPostCard.kt</code> 浅色分支（style 5）的箔带渐变停靠点与收边起始高度，<code>S1Theme.kt</code> 不动；然后构建 → 装机给你验收。想更「满」的话，箔带高度上限（现 64dp）和三色浓度都还能再加，这版先按通栏做。
  </div>
  <p class="kv">说明：本预览按绘制参数用 SVG 手工还原（渐变轴几何与 Compose 的 Brush.linearGradient 一致），真机观感以装机后为准。</p>
</section>

</div>
<script>{JS}</script>
</body></html>'''

    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "outputs", "preview_iridescent")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, "幻彩浅色箔带加宽预览.html")
    with open(out, "w", encoding="utf-8") as f:
        f.write(html)
    print(out)


if __name__ == "__main__":
    build()
