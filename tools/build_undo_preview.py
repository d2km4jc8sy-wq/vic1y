# -*- coding: utf-8 -*-
"""隐藏撤销 · 两种方案对比（票据风格打磨）：就近票根撤销 / 手动恢复提示"""
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
.pcase.rec{border-color:#F0C7E2; background:linear-gradient(180deg,#FFF7FC,#FFF);}
.pcase .pt{font-size:14.5px; font-weight:800; margin-bottom:2px;}
.pcase.rec .pt{color:var(--pink);}
.pcase .ps{font-size:12px; color:var(--ink2); margin-bottom:10px; min-height:36px;}
.stage{background:radial-gradient(120% 100% at 50% 0%, #3B2C4E, #191223); border-radius:14px; padding:0 14px; height:520px; position:relative; overflow:hidden; display:flex; justify-content:center;}
.pnote{margin-top:10px;}
.pnote li{margin:4px 0; list-style:none; padding-left:16px; position:relative; font-size:12px; color:#4A4152;}
.pnote li::before{content:"·"; position:absolute; left:2px; color:var(--pink); font-weight:900;}
.pnote li b{color:#241A2E;}
table{border-collapse:collapse; width:100%; font-size:13px; margin-top:10px;}
th,td{border:1px solid var(--line); padding:6px 10px; text-align:left;}
th{background:#FAF6FC; font-weight:600;}
.ok{color:var(--ok); font-weight:700;}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
/* ═══ 场景道具：撕票后的瀑布流（半透明残影示意）═══ */
.feedhint{position:absolute; inset:0; display:flex; align-items:center; justify-content:center;
  color:rgba(255,255,255,.16); font-size:38px; letter-spacing:6px; font-weight:900;}
/* ═══ 手法：真撕票两层（演示里直接呈分离后状态回放）═══ */
.ticket{width:296px; position:absolute; top:36px; left:50%; margin-left:-148px;}
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
.tt{font-size:13.5px; font-weight:800; color:#FFF; line-height:1.45; padding:9px 16px 0;}
.pp{font-size:11px; color:rgba(240,230,250,.84); line-height:1.64; padding:6px 16px 0; max-height:72px; overflow:hidden;}
.acts{display:flex; gap:7px; padding:9px 16px 0;}
.tact{flex:1; text-align:center; font-size:11px; font-weight:700; color:#FFF;
  background:rgba(255,255,255,.10); border:1px solid rgba(255,214,240,.28); border-radius:9px; padding:7px 0;}
.tact.hide{color:#FFC7DE;}
.signrow{display:flex; align-items:center; gap:8px; padding:9px 16px 12px;}
.ava-g{width:30px; height:30px; border-radius:9px; flex-shrink:0;
  background:linear-gradient(135deg,#FF6FB5,#B66BF7); color:#FFF; font-weight:900; font-size:13px;
  display:flex; align-items:center; justify-content:center; border:1.5px solid rgba(255,214,240,.55);}
.signrow .sn{font-size:10.5px; font-weight:700; color:#FFF;}
.topen{margin-left:auto; font-size:10px; font-weight:700; color:#E9C8EE;
  border:1px solid rgba(233,200,238,.4); border-radius:99px; padding:4px 12px;}
/* 撕票两层（分离后定格回放）*/
.tTop{clip-path: inset(0 0 47% 0); transform:translate(-56px,-120px) rotate(-17deg); opacity:.28; position:absolute; top:36px; left:50%; margin-left:-148px; filter:drop-shadow(0 8px 8px rgba(0,0,0,.3));}
.tBot{clip-path: inset(53% 0 0 0); transform:translate(40px,170px) rotate(13deg); opacity:.22; position:absolute; top:36px; left:50%; margin-left:-148px; filter:drop-shadow(0 8px 8px rgba(0,0,0,.3));}
/* ═══ 方案一：就近票根撤销钮 ═══ */
.stub-undo{position:absolute; z-index:20; left:50%; top:332px; margin-left:-104px; width:208px;
  background:#241A33; border-radius:12px; overflow:hidden;
  border:1.5px solid transparent; background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7);
  background-origin:border-box; background-clip:padding-box,border-box;
  box-shadow:0 12px 30px rgba(0,0,0,.55);
  display:flex; align-items:stretch;
  animation:stubIn 9s infinite;}
@keyframes stubIn{0%,36%{opacity:0; transform:translateY(-16px) scale(.9);} 42%,88%{opacity:1; transform:translateY(0) scale(1);} 96%,100%{opacity:0; transform:translateY(-8px);}}
.stub-undo .msg{flex:1; padding:9px 12px; font-size:11px; font-weight:700; color:#F4ECFA; line-height:1.3;}
.stub-undo .msg i{display:block; font-style:normal; font-size:8.5px; font-weight:400; color:#C9B8DE; margin-top:1px;}
.stub-undo .undo{padding:9px 15px; font-size:11.5px; font-weight:900; color:#221830;
  background:linear-gradient(100deg,#FFD76E,#FFC7DE); display:flex; align-items:center;
  border-left:1.6px dashed rgba(255,214,240,.5);}
.stub-undo .undo b{font-weight:900;}
/* 撤销成功反馈（点撤销后票根变绿勾）*/
.stub-ok{position:absolute; z-index:21; left:50%; top:332px; margin-left:-104px; width:208px;
  background:rgba(14,159,110,.16); border:1.5px solid rgba(14,159,110,.7); border-radius:12px;
  padding:9px 12px; font-size:11px; font-weight:800; color:#7CE8C4; text-align:center;
  animation:okIn 9s infinite;}
@keyframes okIn{0%,88%{opacity:0; transform:scale(.9);} 91%{opacity:1; transform:scale(1);} 97%{opacity:1;} 100%{opacity:0;}}
/* ═══ 方案二：顶部细条提示（手动恢复）═══ */
.topnote{position:absolute; z-index:20; left:14px; right:14px; top:14px;
  background:rgba(36,26,51,.92); border:1px solid rgba(255,111,181,.35); border-radius:11px;
  padding:9px 13px; font-size:11px; font-weight:700; color:#F4ECFA; text-align:center;
  animation:tnIn 9s infinite;}
.topnote i{font-style:normal; color:#C9B8DE; font-weight:400;}
@keyframes tnIn{0%,38%{opacity:0; transform:translateY(-12px);} 44%,86%{opacity:1; transform:translateY(0);} 94%,100%{opacity:0;}}
/* 时间轴标注 */
.timeline{font-size:11.5px; color:var(--ink2); margin-top:8px; display:flex; gap:14px; flex-wrap:wrap;}
.timeline span b{color:var(--pink);}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

TICKET_INNER = '''
  <div class="holo"></div>
  <div class="tin">
    <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
    <div style="height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;"></div>
    <div class="tt">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼</div>
    <div class="pp">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线。</div>
    <div style="display:flex; gap:7px; padding:9px 16px 0;">
      <div class="tact">☆ 收藏</div><div class="tact">↗ 分享</div><div class="tact hide">🙈 隐藏</div>
    </div>
    <div class="signrow"><div class="ava-g">a</div>
      <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
      <span class="topen">查看完整帖 ›</span></div>
  </div>'''

CASE_A = f'''<div class="stage">
  <div class="feedhint">瀑布流</div>
  <div class="ticket tTop"><div class="paper">{TICKET_INNER}</div></div>
  <div class="ticket tBot"><div class="paper">{TICKET_INNER}</div></div>
  <div class="stub-undo">
    <div class="msg">已隐藏该帖<i>可在 我的-抹布贴 恢复</i></div>
    <div class="undo"><b>撤销</b></div>
  </div>
  <div class="stub-ok">✓ 已撤销 · 帖子已恢复</div>
</div>'''

CASE_B = f'''<div class="stage">
  <div class="feedhint">瀑布流</div>
  <div class="ticket tTop"><div class="paper">{TICKET_INNER}</div></div>
  <div class="ticket tBot"><div class="paper">{TICKET_INNER}</div></div>
  <div class="topnote">已隐藏 · 恢复请到 <i>我的 - 抹布贴</i></div>
</div>'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>隐藏撤销 · 两种方案对比</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">隐藏撤销</span>就近票根撤销 vs 手动恢复 · 票据风格打磨</h1>
  <div class="sub">2026-10-15 · 两者都不是普通形状：跟随真撕票动画出现 · 9s 循环演示「点隐藏 → 撕票 → 撤销/提示」</div>
</header>

<section>
  <h2>① 你点出的问题 → 方案对策</h2>
  <div class="card">
    <table>
      <tr><th>你的反馈</th><th>对策</th></tr>
      <tr><td>撤销条在<b>最下面</b>，撞色不显眼</td><td>撤销钮出现在<b>手指刚点「隐藏」的位置附近</b>（记住按钮屏幕坐标就地浮现），不再沉底</td></tr>
      <tr><td>按钮太长</td><td>短票根 208×40dp，左半「已隐藏该帖」右半「撤销」，一指可点</td></tr>
      <tr><td>点撤销没提示成功没</td><td>点撤销 → 票根原地翻转成<b>「✓ 已撤销 · 帖子已恢复」</b>绿章反馈 0.6s</td></tr>
      <tr><td>要随票据风格打磨，不是普通形状</td><td>撤销钮本体 = <b>迷你票根</b>：同款深紫纸面 + 彩丝描边 + 中缝虚线（票根语义：撕下那半票的存根）</td></tr>
      <tr><td>瀑布流难往下看</td><td>撤销不再沉底挡视线；撕票动画后浮现于原操作区，7s 自动消失</td></tr>
    </table>
  </div>
</section>

<section>
  <h2>② 两案对比 · 9s 循环（点隐藏 → 撕票 → 撤销/提示）</h2>
  <div class="duo">

    <div class="pcase rec">
      <div class="pt">方案一 · 就近票根撤销 <span style="font-size:11px;color:var(--ok);">★ 推荐</span></div>
      <div class="ps">撕票两半飞出后，一枚<b>迷你票根</b>在原「隐藏」按钮位置淡入：左半「已隐藏该帖」，右半金色「撤销」——点撤销原地变绿章「已恢复」。</div>
      <div class="stage">{CASE_A}</div>
      <div class="timeline"><span><b>0s</b> 点隐藏</span><span><b>0.43s</b> 撕票完成</span><span><b>1.5s</b> 票根浮现于原按钮位</span><span><b>7s</b> 自动消失（未点=默认保持隐藏）</span></div>
      <ul class="pnote">
        <li><b>就近反悔</b>：手指原位，抬手即可点撤销</li>
        <li>撤销成功有<b>绿色确认反馈</b>（你点出的「没提示」）</li>
        <li>7s 不点 → 自动消失，等同确认隐藏（不挡后续浏览）</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">方案二 · 手动恢复（v72 现状路线）</div>
      <div class="ps">不做撤销钮：顶部细条提示「已隐藏 · 恢复请到 我的-抹布贴」，恢复走手动路径。</div>
      <div class="stage">{CASE_B}</div>
      <div class="timeline"><span><b>0s</b> 点隐藏</span><span><b>0.43s</b> 撕票完成</span><span><b>顶部细条 3s</b> 自动消失</span></div>
      <ul class="pnote">
        <li>最安静、零误触；但<b>反悔要跨三个页面</b>（我的→抹布贴→恢复）</li>
        <li>顶部条与门票同风格深紫（也做了票据化打磨）</li>
      </ul>
    </div>

  </div>
</section>

<section>
  <h2>③ 我的意见</h2>
  <div class="card note">
    <b>方案一</b>。你点出的四个痛点（沉底/撞色/太长/无反馈）它一次全解，而且「票根存根」和撕票是同一套隐喻——撕掉的那半票根飘回来问你「反悔吗」，叙事是闭环的。实现量可控：Peek 操作行用 onGloballyPositioned 记下「隐藏」按钮的屏幕坐标传给调用方，撕票完成后在该坐标 Popup 一枚票根（复用门票绘制语法），点撤销原地翻转绿章。<br>
    <span class="small">两案都保留「我的-抹布贴」作为最终手动恢复入口，互不冲突。</span>
  </div>
</section>

<section>
  <h2>④ 待你勾选 · 「首页跳转页面改为详情那种滚轮」</h2>
  <div class="card">
    <p>这条我仍不敢猜着动（详情页是 ViewPager 左右翻页，v63 定位逻辑的地基）。两个候选解读：</p>
    <p><b>a) 转场解读</b>：Peek 点「查看完整帖」进入详情的<b>过渡动画</b>改成滑动式（比如票面滑入变详情首页），详情页本身翻页方式不变；<br>
    <b>b) 交互解读</b>：详情页本身<b>从左右翻页改成上下滚动</b>（像论坛流一滚到底）——这是核心交互重构，需要单独出一版方案（翻页定位/阅读进度/投票卡联动都要跟着动）。</p>
    <p class="small">回复 a 或 b（或描述你想要的动作），a 我直接做，b 我先出方案。</p>
  </div>
</section>

<footer>隐藏撤销两案 + 滚轮确认 · 撕票已确认采用（原票追加）· 定撤销方案 + 回复 a/b 即装机。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/隐藏撤销两案.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
