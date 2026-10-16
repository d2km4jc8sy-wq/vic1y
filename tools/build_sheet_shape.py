# -*- coding: utf-8 -*-
"""跳转 Sheet 形态预览：圆角版（现状）vs 票据版（仿门票统一设计语言）——浅色/深色双主题"""
import os

CSS = """
:root{--pri:#5E6AD2;--bg:#08090A;--surf:#0F1011;--bd:rgba(255,255,255,.08);--tx:#EDEEF2;--sub:#8A8F98;
--pink:#FF6FB5;--purple:#B66BF7;--gold:#FFD76E;--paper:#241A33;--font:"PingFang SC","Microsoft YaHei",system-ui;--mono:ui-monospace,Consolas;}
*{box-sizing:border-box;margin:0;padding:0}
body{background:var(--bg);color:var(--tx);font-family:var(--font);padding:30px 20px 80px;-webkit-font-smoothing:antialiased}
.wrap{max-width:1150px;margin:0 auto}
header .k{font-family:var(--mono);font-size:11px;letter-spacing:.18em;color:var(--pri);text-transform:uppercase}
header h1{font-size:25px;font-weight:650;margin-top:8px}header h1 em{font-style:normal;color:var(--pri)}
header p{margin-top:10px;font-size:13px;color:var(--sub);line-height:1.8;max-width:820px}
h2{font-size:17px;border-left:3px solid var(--pri);padding-left:10px;margin:30px 0 14px}
.duo{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}
@media(max-width:1050px){.duo{grid-template-columns:repeat(2,1fr)}}
@media(max-width:620px){.duo{grid-template-columns:1fr}}
.case{background:var(--surf);border:1px solid var(--bd);border-radius:14px;padding:12px}
.case .ct{font-size:13px;font-weight:700;margin-bottom:2px}
.case .cs{font-size:10.5px;color:var(--sub);margin-bottom:10px;line-height:1.6;min-height:46px}
.stage{background:#1B1C1E;border-radius:20px;overflow:hidden;height:520px;position:relative;
background-image:linear-gradient(rgba(94,106,210,.05),transparent)}
.feed{padding:10px;opacity:.92}
.fcard{background:#17181B;border:1px solid var(--bd);border-radius:12px;padding:9px 11px;margin-bottom:8px}
.fcard .t{font-size:11px;font-weight:600;color:var(--tx)}
.fcard .m{font-size:9.5px;color:var(--dim);margin-top:3px}
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.5)}
.sheet{position:absolute;left:0;right:0;bottom:0}
/* ═══ 通用 sheet 内部 ═══ */
.grab{width:34px;height:4px;border-radius:99px;background:rgba(255,255,255,.14);margin:9px auto 8px}
.sh{display:flex;justify-content:space-between;align-items:center;padding:0 16px 8px}
.sh h4{font-size:12px;font-weight:600}
.chip{font-family:var(--mono);font-size:9.5px;border-radius:99px;padding:3px 10px;color:var(--pri);
border:1px solid rgba(94,106,210,.5);background:rgba(94,106,210,.16)}
.wheel{height:150px;overflow:hidden;position:relative;display:flex;align-items:center;justify-content:center}
.band{position:absolute;left:16px;right:16px;top:50%;height:40px;transform:translateY(-50%);
border-radius:10px;background:rgba(94,106,210,.16);border:1px solid rgba(94,106,210,.4)}
.wrow{font-family:var(--mono);font-size:14px;color:var(--dim);height:40px;display:flex;align-items:center;justify-content:center}
.wrow.c{color:var(--tx);font-size:17px;font-weight:700}
/* ═══ A：现状圆角版（深色）═══ */
.sq{background:#141517;border-top:1px solid var(--bds,rgba(255,255,255,.14))}
/* ═══ B：现状圆角版（浅色主题自适应）═══ */
.sq-l{background:#F2EEF6;border-top:1px solid #E2DAEC}
.sq-l .sh h4{color:#241A2E}.sq-l .wrow{color:#B9AECC}.sq-l .wrow.c{color:#241A2E}
.sq-l .band{background:rgba(94,106,210,.12);border-color:rgba(94,106,210,.45)}
.sq-l .chip{color:#5E6AD2;border-color:rgba(94,106,210,.45);background:rgba(94,106,210,.12)}
.sq-l .grab{background:rgba(0,0,0,.14)}
/* ═══ C：票据版（深色）═══ */
.tk{background:var(--paper);position:relative}
.tk::before{content:"";position:absolute;left:0;right:0;top:10px;height:10px;z-index:2;
background:radial-gradient(circle 5px at 8px 5px,#08090A 4.5px,transparent 5.5px);background-size:19px 10px;background-repeat:repeat-x}
.tk .perf-line{margin:14px 16px 6px;border-top:1.6px dashed rgba(255,214,240,.4);position:relative}
.tk .perf-line i{position:absolute;top:-6px;width:12px;height:12px;border-radius:50%;background:#08090A}
.tk .perf-line i.l{left:-22px}.tk .perf-line i.r{right:-22px}
.tk .grab{background:rgba(255,214,240,.3)}
.tk .sh h4{color:#E6D9F2;letter-spacing:1px}
.tk .sh h4 b{color:var(--gold);font-weight:900;margin-right:4px}
.tk .chip{color:var(--gold);border-color:rgba(255,215,110,.5);background:rgba(255,215,110,.12)}
.tk .band{background:rgba(255,111,181,.18);border-color:rgba(255,111,181,.5)}
.tk .wrow{color:#7A6E8C}.tk .wrow.c{color:#FFF;text-shadow:0 0 10px rgba(255,111,181,.5)}
/* ═══ D：票据版（浅色主题自适应）═══ */
.tk-l{background:#F7EFF9}
.tk-l::before{background:radial-gradient(circle 5px at 8px 5px,#F4F0F8 4.5px,transparent 5.5px)}
.tk-l .perf-line{border-top-color:rgba(214,51,156,.35)}
.tk-l .perf-line i{background:#F4F0F8}
.tk-l .grab{background:rgba(0,0,0,.14)}
.tk-l .sh h4{color:#241A2E}.tk-l .sh h4 b{color:#D6339C}
.tk-l .chip{color:#D6339C;border-color:rgba(214,51,156,.4);background:rgba(214,51,156,.08)}
.tk-l .band{background:rgba(214,51,156,.1);border-color:rgba(214,51,156,.4)}
.tk-l .wrow{color:#C0B4CE}.tk-l .wrow.c{color:#241A2E}
.badge{display:inline-block;font-family:var(--mono);font-size:9px;padding:2px 8px;border-radius:99px;margin-left:6px;vertical-align:1px}
.b-ok{background:rgba(14,159,110,.15);color:#0E9F6E}
.b-warn{background:rgba(229,72,77,.12);color:#E5484D}
.b-info{background:rgba(94,106,210,.15);color:var(--pri)}
table{width:100%;border-collapse:collapse;font-size:12.5px;margin-top:10px}
th,td{border:1px solid var(--bd);padding:7px 11px;text-align:left}
th{background:rgba(255,255,255,.03)}
footer{margin-top:30px;padding-top:14px;border-top:1px solid var(--bd);font-size:12px;color:var(--dim)}
"""

FEED = '''<div class="feed">
<div class="fcard"><div class="t">SCS2026 决赛观赛楼</div><div class="m">风夏 · 10 回复</div></div>
<div class="fcard"><div class="t">Fami 专栏玩家测试，看你对各种游戏系列的熟悉程度</div><div class="m">Dragon_Q · 36 回复</div></div>
<div class="fcard"><div class="t">无冬之夜：增强版 89.8193.37-17 简体中文汉化包</div><div class="m">没头脑和不高兴 · 15 回复</div></div>
</div>'''

def sheet_a(dark=True):
    cls = 'sq' if dark else 'sq sq-l'
    tag = '现状 · 圆角版（深色）' if dark else '现状 · 圆角版（浅色主题自适应后）'
    return f'''<div class="sheet {cls}">
  <div class="grab"></div>
  <div class="sh"><h4>跳转到页</h4><div class="chip">可长按跳转</div></div>
  <div class="wheel"><div class="band"></div>
    <div class="wrow">1</div><div class="wrow">2</div><div class="wrow c">3</div><div class="wrow">4</div><div class="wrow">5</div>
  </div>
</div>'''

def sheet_ticket(dark=True):
    cls = 'tk' if dark else 'tk tk-l'
    tag = '提案 · 票据版（深色）' if dark else '提案 · 票据版（浅色主题自适应）'
    return f'''<div class="sheet {cls}">
  <div class="grab"></div>
  <div class="perf-line"><i class="l"></i><i class="r"></i></div>
  <div class="sh"><h4><b>STAGE1</b>跳转到页</h4><div class="chip">可长按跳转</div></div>
  <div class="wheel"><div class="band"></div>
    <div class="wrow">1</div><div class="wrow">2</div><div class="wrow c">3</div><div class="wrow">4</div><div class="wrow">5</div>
  </div>
</div>'''

def case(cls, ct, cs, sheet_html):
    return f'''<div class="case">
  <div class="ct">{ct}</div><div class="cs">{cs}</div>
  <div class="stage"><div class="feed">{FEED}</div><div class="scrim"></div>{sheet_html}</div>
</div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>跳转 Sheet 形态预览 · 圆角 vs 票据</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <div class="k">Sheet Shape Proposal · 统一设计语言</div>
  <h1>跳转面板形态：<em>圆角版（现状）</em> vs <em>票据版</em></h1>
  <p>你提的两件事都画进去了：①上部直角改圆角（A/B 即修后效果）②「仿票据形状」提案（C/D）：顶部一排打孔 + 撕裂虚线 + STAGE1 金字票头，和门票同一套语言。四张都是静态效果图，双主题并排。</p>
</header>

<h2>① 四形态对比（深色 / 浅色 × 圆角 / 票据）</h2>
<div class="duo">
  {case('', 'A · 圆角版 · 深色 <span class="badge b-ok">现状已修</span>', '顶角 22dp 圆角 + 主题玻璃底。目前装机效果。', sheet_a(True))}
  {case('', 'B · 圆角版 · 浅色自适应 <span class="badge b-ok">本轮修复</span>', 's1GlassSheetBg 派生：浅色主题下自动浅底深字，不再近黑突兀。', sheet_a(False))}
  {case('', 'C · 票据版 · 深色 <span class="badge b-info">提案</span>', '顶部打孔 + 撕裂虚线 + 「STAGE1 跳转到页」金字票头——与长按门票同一套语言。', sheet_ticket(True))}
  {case('', 'D · 票据版 · 浅色自适应 <span class="badge b-info">提案</span>', '同 C，浅色主题派生（粉紫描边+纸白底）。', sheet_ticket(False))}
</div>

<h2>② 我的建议</h2>
<div class="card" style="background:var(--surf);border:1px solid var(--bd);border-radius:14px;padding:14px 18px">
  <p><b style="color:var(--pri)">推荐 C/D（票据版）</b>——理由：跳转 sheet 和门票是同一个「票据体系」的两件套（长按帖 = 拿票，跳页 = 检票口），打孔+撕裂线+STAGE1 票头让它一眼就是 S1 的东西；工程上只是 GradientDrawable 换 cornerRadii+两个小 View，30 分钟量级，白边风险为零（打孔用 scrim 色圆点，不吃 popup 背景）。</p>
  <p style="margin-top:8px"><b>若嫌打孔太花</b>：退一档用「只有撕裂虚线+票头字」的无孔版（去掉 ::before 打孔条），语言感保留 80%，视觉更安静。</p>
  <table>
    <tr><th>项</th><th>圆角版（A/B）</th><th>票据版（C/D）</th></tr>
    <tr><td>统一设计语言</td><td>中性（和任何 App 一样）</td><td class="ok">强（票据体系延伸）</td></tr>
    <tr><td>工程量</td><td>已修完（0）</td><td><span class="ok">小</span>（30 分钟：背景 drawable+两行 View）</td></tr>
    <tr><td>白边/亮角风险</td><td>无</td><td class="ok">无（打孔圆点用页面 scrim 色，宪法 18 合规）</td></tr>
    <tr><td>适配主题</td><td class="ok">✓ 双主题</td><td class="ok">✓ 双主题</td></tr>
  </table>
</div>

<footer>跳转 Sheet 形态提案 · 现状（A/B）与提案（C/D）——你选一个，我直接改 WheelJumpSheetView 的背景绘制。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/跳转Sheet形态预览.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html)//1024, "KB")
