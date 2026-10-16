# -*- coding: utf-8 -*-
"""全息门票 v3 · 三版布局（票根签名 / 撕票入场 / 竖票根）"""
import base64, os

A = "preview_iridescent/assets"

def b64(name):
    mime = "jpeg" if name.endswith(".jpg") else "png"
    with open(os.path.join(A, name), "rb") as f:
        return "data:image/%s;base64," % mime + base64.b64encode(f.read()).decode()

# 不再用照片头像：字母渐变头像（瀑布流兜底同款）

CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F6F2F9; --card:#FFF; --pink:#D6339C; --purple:#8B3FD9;}
*{box-sizing:border-box; margin:0; padding:0;}
body{background:var(--bg); color:var(--ink); font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; line-height:1.65; padding:32px 20px 80px;}
.wrap{max-width:1180px; margin:0 auto;}
header h1{font-size:24px; letter-spacing:.5px;}
header .sub{color:var(--ink2); font-size:13px; margin-top:6px;}
.badge{display:inline-block; background:linear-gradient(100deg,var(--pink),var(--purple)); color:#fff; font-size:12px; padding:2px 10px; border-radius:99px; margin-right:8px; vertical-align:2px;}
section{margin-top:32px;}
h2{font-size:18px; border-left:4px solid var(--pink); padding-left:10px; margin-bottom:12px;}
h3{font-size:15px; margin:14px 0 6px;}
p{font-size:14px;}
.small{font-size:12.5px; color:var(--ink2);}
.card{background:var(--card); border:1px solid var(--line); border-radius:14px; padding:16px 20px; margin-top:12px;}
.note{background:linear-gradient(100deg,#FDF0F8,#F3EBFE); border:1px solid #F0D9EE;}
.note b{color:var(--pink);}
.tri3{display:grid; grid-template-columns:1fr 1fr 1fr; gap:18px; margin-top:14px;}
@media (max-width:1000px){.tri3{grid-template-columns:1fr;}}
.pcase{background:#FFF; border:1px solid var(--line); border-radius:16px; padding:14px; display:flex; flex-direction:column;}
.pcase .pt{font-size:14.5px; font-weight:800; margin-bottom:2px;}
.pcase .ps{font-size:12px; color:var(--ink2); margin-bottom:10px; min-height:36px;}
.stage{background:radial-gradient(120% 100% at 50% 0%, #3B2C4E, #191223); border-radius:14px; padding:26px 14px; display:flex; justify-content:center; align-items:flex-start;}
.pnote{margin-top:10px;}
.pnote li{margin:4px 0; list-style:none; padding-left:16px; position:relative; font-size:12px; color:#4A4152;}
.pnote li::before{content:"·"; position:absolute; left:2px; color:var(--pink); font-weight:900;}
.pnote li b{color:#241A2E;}
/* ═══════ 门票公共 ═══════ */
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
.thead{padding:14px 16px 8px; display:flex; align-items:baseline; gap:8px;}
.thead .big{font-size:19px; font-weight:900; letter-spacing:2px; color:rgba(255,255,255,.85); text-shadow:0 0 14px rgba(255,111,181,.45);}
.thead .zone{font-size:10px; color:#E9C8EE; opacity:.85;}
/* 右上角：验票章 */
.stamp{position:absolute; top:34px; right:11px; width:54px; height:54px; z-index:6;
  border:2px dashed rgba(255,143,199,.65); border-radius:50%; transform:rotate(-12deg);
  display:flex; flex-direction:column; align-items:center; justify-content:center; gap:0;
  color:#FFC7DE; background:rgba(36,26,51,.35);}
.stamp .z1{font-size:12px; font-weight:900; letter-spacing:2px; line-height:1.1;}
.stamp .z2{font-size:7px; opacity:.8; letter-spacing:1px;}
.tear{position:relative; height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;}
.tear i{position:absolute; top:-7px; width:14px; height:14px; border-radius:50%; background:#2E2340;}
.tear i.l{left:-8px;} .tear i.r{right:-8px;}
.tt{font-size:13px; font-weight:800; color:#FFF; line-height:1.45; padding:9px 16px 0;}
.pp{font-size:10.5px; color:rgba(240,230,250,.82); line-height:1.62; padding:6px 16px 0; max-height:110px; overflow:hidden;}
.stubstats{display:flex; align-items:center; gap:10px; padding:10px 16px 4px;}
.stats{font-size:9.5px; color:#E9C8EE; line-height:1.7;}
.stats b{color:#FFF;}
.barcode{margin-left:auto; width:40px; height:24px; opacity:.85;
  background:repeating-linear-gradient(90deg,
    #E9C8EE 0 1px, transparent 1px 3px, #E9C8EE 3px 4px, transparent 4px 6px,
    #E9C8EE 6px 9px, transparent 9px 11px, #E9C8EE 11px 12px, transparent 12px 16px,
    #E9C8EE 16px 17px, transparent 17px 20px, #E9C8EE 20px 23px, transparent 23px 26px,
    #E9C8EE 26px 27px, transparent 27px 31px, #E9C8EE 31px 34px, transparent 34px 40px);}
/* 字母渐变头像（瀑布流兜底同款）*/
.ava-g{background:linear-gradient(135deg,#FF6FB5,#B66BF7); color:#FFF; font-weight:900;
  display:flex; align-items:center; justify-content:center; flex-shrink:0;
  border:1.5px solid rgba(255,214,240,.55);}
.ava-g.ag32{width:32px; height:32px; border-radius:9px; font-size:14px;}
.ava-g.ag40{width:40px; height:40px; border-radius:11px; font-size:17px;}
.ava-g.ag44{width:44px; height:44px; border-radius:50%; font-size:18px;}
/* 门票风格按钮（不突兀）*/
.tact{flex:1; text-align:center; font-size:11px; font-weight:700; color:#FFF;
  background:rgba(255,255,255,.10); border:1px solid rgba(255,214,240,.28); border-radius:9px; padding:7px 0;}
.tact.hide{color:#FFC7DE;}
.topen-link{font-size:10.5px; font-weight:700; color:#E9C8EE; text-decoration:none;
  border:1px solid rgba(233,200,238,.35); border-radius:99px; padding:4px 13px; white-space:nowrap;}
.topen-fine{font-size:9.5px; color:rgba(233,200,238,.75); text-align:center; padding:8px 16px 13px;}
/* ═══════ 版A 票根签名 ═══════ */
.vA .acts{display:flex; gap:7px; padding:9px 16px 0;}
.vA .signrow{display:flex; align-items:center; gap:8px; padding:9px 16px 13px;}
.vA .signrow .sn{font-size:10.5px; font-weight:700; color:#FFF;}
.vA .signrow .sn span{font-size:8.5px; font-weight:400; color:#D8CCE6; opacity:.8; margin-left:3px;}
.vA .signrow .st{font-size:8.5px; color:#D8CCE6; opacity:.7;}
.vA .signrow .open{margin-left:auto;}
/* ═══════ 版B 撕票入场 ═══════ */
.vB .sign{display:flex; align-items:center; gap:9px; padding:10px 16px 0;}
.vB .sign .sn{font-size:12px; font-weight:800; color:#FFF;}
.vB .sign .sn span{font-size:9px; font-weight:400; color:#D8CCE6; opacity:.8; margin-left:4px;}
.vB .sign .st{font-size:8.5px; color:#D8CCE6; opacity:.7; margin-top:1px;}
.vB .miniacts{margin-left:auto; display:flex; gap:6px;}
.vB .mini{width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center;
  font-size:13px; color:#FFF; background:rgba(255,255,255,.10); border:1px solid rgba(255,214,240,.3);}
.vB .mini.hide{color:#FFC7DE;}
/* ═══════ 版C 竖票根 ═══════ */
.vC .cols{display:flex;}
.vC .main{flex:1; min-width:0; padding-right:2px;}
.vC .stubcol{width:86px; flex-shrink:0; border-left:1.6px dashed rgba(255,214,240,.45);
  position:relative; display:flex; flex-direction:column; align-items:center; padding:12px 6px 12px 10px;}
.vC .stubcol i{position:absolute; left:-8px; width:14px; height:14px; border-radius:50%; background:#2E2340;}
.vC .stubcol i.t{top:38px;} .vC .stubcol i.b{bottom:38px;}
.vC .stubcol .cn{font-size:9.5px; font-weight:800; color:#FFF; margin-top:5px; text-align:center;}
.vC .stubcol .ct{font-size:7.5px; color:#D8CCE6; opacity:.7; margin-top:1px;}
.vC .vacts{display:flex; flex-direction:column; gap:6px; margin-top:9px;}
.vC .vmini{width:30px; height:30px; border-radius:50%; display:flex; align-items:center; justify-content:center;
  font-size:12px; color:#FFF; background:rgba(255,255,255,.10); border:1px solid rgba(255,214,240,.3);}
.vC .vmini.hide{color:#FFC7DE;}
.vC .openlink{margin:8px 16px 12px; text-align:center;}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

TITLE = "【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论"
BODY = ("先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，"
        "53 楼有当事方的回应截图，112 楼是版务处理结果。欢迎补充证据链，注意人身攻击直接删……")
HEAD = '<div class="thead"><span class="big">STAGE1</span><span class="zone">自由搏击区</span></div><div class="stamp"><span class="z1">热帖</span><span class="z2">HOT 16789</span></div>'
TEAR = '<div class="tear"><i class="l"></i><i class="r"></i></div>'
STATS = '<div class="stubstats"><div class="stats">💬 <b>16789</b> 回复 · 👁 <b>2.1w</b> · ⭐ <b>456</b><br>座席 <b>E7</b> · 已验票</div><div class="barcode"></div></div>'
AG = lambda cls: f'<div class="ava-g {cls}">a</div>'

VA = f'''<div class="ticket vA">
  <div class="perf ptop"></div><div class="perf pbot"></div>
  <div class="paper"><div class="holo"></div><div class="grain"></div>
  <div class="tin">
    {HEAD}{TEAR}
    <div class="tt">{TITLE}</div>
    <div class="pp">{BODY}</div>
    {STATS}
    <div class="acts"><div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div></div>
    <div class="signrow">
      {AG("ag32")}
      <div><div class="sn">abfk<span>229182</span></div><div class="st">发帖于 2024-09-22</div></div>
      <span class="open topen-link">查看完整帖 ›</span>
    </div>
  </div></div>
</div>'''

VB = f'''<div class="ticket vB">
  <div class="perf ptop"></div><div class="perf pbot"></div>
  <div class="paper"><div class="holo"></div><div class="grain"></div>
  <div class="tin">
    {HEAD}{TEAR}
    <div class="tt">{TITLE}</div>
    <div class="pp">{BODY}</div>
    {STATS}{TEAR}
    <div class="sign">
      {AG("ag40")}
      <div><div class="sn">abfk<span>229182</span></div><div class="st">发帖于 2024-09-22 · 自由搏击区</div></div>
      <div class="miniacts"><div class="mini">☆</div><div class="mini">↗</div><div class="mini hide">🙈</div></div>
    </div>
    <div class="topen-fine">— 点击票面任意处 进入全文 —</div>
  </div></div>
</div>'''

VC = f'''<div class="ticket vC">
  <div class="perf ptop"></div><div class="perf pbot"></div>
  <div class="paper"><div class="holo"></div><div class="grain"></div>
  <div class="tin">
    <div class="cols">
      <div class="main">
        <div class="thead" style="padding-right:64px; position:relative;"><span class="big">STAGE1</span><span class="zone">自由搏击区</span><div class="stamp" style="top:26px; right:4px; width:48px; height:48px;"><span class="z1" style="font-size:10.5px;">热帖</span><span class="z2">HOT</span></div></div>
        <div class="tt" style="padding-top:4px;">{TITLE}</div>
        <div class="pp">{BODY}</div>
        <div class="stubstats" style="padding-right:10px;"><div class="stats">💬 <b>16789</b> · 👁 <b>2.1w</b> · ⭐ <b>456</b><br>座席 <b>E7</b> · 已验票</div><div class="barcode" style="width:34px;"></div></div>
        <div class="openlink"><span class="topen-link">查看完整帖 ›</span></div>
      </div>
      <div class="stubcol"><i class="t"></i><i class="b"></i>
        {AG("ag44")}
        <div class="cn">abfk</div><div class="ct">2024-09-22</div>
        <div class="vacts"><div class="vmini">☆</div><div class="vmini">↗</div><div class="vmini hide">🙈</div></div>
      </div>
    </div>
  </div></div>
</div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>全息门票 v3 · 三版布局</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">门票 v3</span>按你的四条需求 · 三版布局</h1>
  <div class="sub">2026-10-15 · ①作者信息移右下角 ②进帖与三功能二选一 ③右上角做文章=「热帖验票章」 ④头像换字母渐变（瀑布流兜底同款）· 进帖按钮全面去渐变、融票面</div>
</header>

<section>
  <h2>① 需求分析 · 为什么作者信息放右下角是对的</h2>
  <div class="card">
    <p><b>① 票根语法：</b>真实门票/机票的「持票人」从来印在<b>票根右下角</b> —— 那是「这票是谁的」的签名位。预览里作者就是持票人，放右下角符合票券视觉语法；<br>
    <b>② 视线动线：</b>读完正文后视线沿 Z 字收尾落在右下角，「看完内容、记住是谁」，作者信息在动线终点；<br>
    <b>③ 给正文让位：</b>名片头移走后正文可视高度多约 60dp —— 预览的核心目的（快速判断）直接受益；<br>
    <b>④ 撕裂线语义自洽：</b>线上=票面（帖子内容），线下=票根（持票人+操作）—— 原版作者夹在票身中间，语义反而不顺。</p>
  </div>
</section>

<section>
  <h2>② 三版布局（右上角=热帖验票章 · 头像=字母渐变 · 进帖全面融入票面）</h2>
  <div class="tri3">

    <div class="pcase">
      <div class="pt">版A · 票根签名 <span style="font-size:11px;color:var(--ok);">★ 推荐</span></div>
      <div class="ps">保留「查看完整帖」：三功能横排票根，作者签名+细字进帖链接同排右下。</div>
      <div class="stage">{VA}</div>
      <ul class="pnote">
        <li>作者信息 = <b>票根左下签名位</b>（32dp 头像+名+uid+日期）</li>
        <li>进帖 = <b>细字胶囊链接</b>（票面色，无渐变不突兀）</li>
        <li>信息密度最高，动线最顺：内容→数据→操作→签名收尾</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">版B · 撕票入场</div>
      <div class="ps">「查看完整帖」整个隐藏：<b>点票面任意处进入全文</b>，三功能缩为圆钮与作者同排。</div>
      <div class="stage">{VB}</div>
      <ul class="pnote">
        <li>作者签名升级为<b>40dp 头像+两行</b>（右下角存在感最强）</li>
        <li>三功能缩为 <b>32dp 圆钮</b>（隐藏按钮独立位）</li>
        <li>「点票面入场」= 撕票隐喻闭环；底部细字提示可留可去</li>
        <li>风险：误触进帖率略升（点正文就进去了）</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">版C · 竖票根</div>
      <div class="ps">电影票副券式：<b>竖向撕裂线</b>，右侧窄票根竖排作者+三功能，左区正文最大化。</div>
      <div class="stage">{VC}</div>
      <ul class="pnote">
        <li><b>竖撕裂线</b>（上下缺口）+ 86dp 票根列：圆头像+名+日期+三圆钮竖排</li>
        <li>左区正文行宽变窄 ~86dp，长标题折行增多</li>
        <li>最像「真票」（副券），但手机窄屏上正文行宽是硬伤</li>
      </ul>
    </div>

  </div>
</section>

<section>
  <h2>③ 公共改动说明</h2>
  <div class="card note">
    <b>右上角的「文章」= 热帖验票章：</b>虚线圆章（rotate -12° 盖章感），内文「热帖 / HOT 16789」—— 数据联动（回复数超阈值才显示），既是票据隐喻又是热帖标识，替换原来纯装饰的 No.编号（编号缩小并入座席行）。<br>
    <b>头像 = 字母渐变：</b>瀑布流无图作者的兜底头像同款（渐变底+白字母），彩丝细环描边 —— 和主题血统一致，也解决了照片头像违和的问题。<br>
    <b>进帖按钮去突兀：</b>三版都不再用粉白渐变大按钮 —— 版A/版C 细字胶囊链接，版B 干脆藏起来点票面入场。<br>
    <span class="small">我的意见：<b>版A</b>（动线最顺、密度最高、误触风险为零）；如果你喜欢「撕票入场」的隐喻彻底性，选版B并把底部提示行去掉。版C 建议只作观赏 —— 窄屏正文行宽吃亏。</span>
  </div>
</section>

<footer>全息门票 v3 · 三版布局 · 仅预览未改码 —— 定一版（或混搭点）即开工。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/全息门票v3三版.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
