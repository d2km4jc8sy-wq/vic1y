# -*- coding: utf-8 -*-
"""滚轮交互 v2 · 蓄力确认 + 模态化 + 位置收拢 · 演示预览（含撕票+票根撤销确认稿）"""
import os

CSS = """
:root{--ink:#241A2E; --ink2:#5C5266; --line:#E7DFEE; --bg:#F6F2F9; --card:#FFF; --pink:#D6339C; --purple:#8B3FD9; --ok:#0E9F6E; --gold:#FFD76E;}
*{box-sizing:border-box; margin:0; padding:0;}
body{background:var(--bg); color:var(--ink); font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; line-height:1.65; padding:32px 20px 80px;}
.wrap{max-width:1080px; margin:0 auto;}
header h1{font-size:24px; letter-spacing:.5px;}
header .sub{color:var(--ink2); font-size:13px; margin-top:6px;}
.badge{display:inline-block; background:linear-gradient(100deg,var(--pink),var(--purple)); color:#fff; font-size:12px; padding:2px 10px; border-radius:99px; margin-right:8px; vertical-align:2px;}
section{margin-top:32px;}
h2{font-size:18px; border-left:4px solid var(--pink); padding-left:10px; margin-bottom:12px;}
h3{font-size:15px; margin:12px 0 6px;}
p{font-size:14px;}
.small{font-size:12.5px; color:var(--ink2);}
.card{background:var(--card); border:1px solid var(--line); border-radius:14px; padding:16px 20px; margin-top:12px;}
.note{background:linear-gradient(100deg,#FDF0F8,#F3EBFE); border:1px solid #F0D9EE;}
.note b{color:var(--pink);}
.duo{display:grid; grid-template-columns:1fr 1fr; gap:20px; margin-top:14px;}
@media (max-width:900px){.duo{grid-template-columns:1fr;}}
.pcase{background:#FFF; border:1px solid var(--line); border-radius:16px; padding:14px;}
.pcase .pt{font-size:14.5px; font-weight:800; margin-bottom:8px;}
.pnote{margin-top:10px;}
.pnote li{margin:5px 0; list-style:none; padding-left:16px; position:relative; font-size:12.5px; color:#4A4152;}
.pnote li::before{content:"·"; position:absolute; left:2px; color:var(--pink); font-weight:900;}
.pnote li b{color:#241A2E;}
table{border-collapse:collapse; width:100%; font-size:13px; margin-top:10px;}
th,td{border:1px solid var(--line); padding:6px 10px; text-align:left;}
th{background:#FAF6FC; font-weight:600;}
.ok{color:var(--ok); font-weight:700;}
.stage{background:radial-gradient(120% 100% at 50% 0%, #3B2C4E, #191223); border-radius:14px; height:560px; position:relative; overflow:hidden;}
.feedhint{position:absolute; inset:0; display:flex; align-items:center; justify-content:center;
  color:rgba(255,255,255,.14); font-size:40px; letter-spacing:8px; font-weight:900;}
/* ═══ 演示时间轴（12s 循环）═══
   0-6%   静止瀑布流+胶囊
   6-14%  遮罩暗化 + 滚轮自胶囊上方展开
   14-30% 滚动选页（数字轮转）
   30-44% 按住蓄力（进度环充满 + 震感分级标注）
   44-52% 满圈跳转闪光 + 滚轮收起
   52-60% 场景切换：点隐藏
   60-72% 真撕票两半分离
   72-90% 票根撤销浮现
   90-100% 复位 */
/* 胶囊 */
.pill{position:absolute; right:14px; bottom:16px; z-index:8; background:#2B2238; border:1px solid rgba(255,255,255,.16);
  border-radius:99px; padding:8px 14px; font-size:12.5px; font-weight:600; color:#D8CCE6;}
/* 遮罩 */
.scrim{position:absolute; inset:0; z-index:9; background:rgba(8,4,14,.55); opacity:0; animation:sc12 12s infinite;}
@keyframes sc12{0%,5%{opacity:0;} 9%,50%{opacity:1;} 56%,100%{opacity:0;}}
/* 滚轮面板：从胶囊上方展开 */
.wheelbox{position:absolute; right:14px; bottom:64px; z-index:12; width:112px;
  background:#241A33; border-radius:14px; overflow:hidden;
  border:1.5px solid transparent; background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7);
  background-origin:border-box; background-clip:padding-box,border-box;
  box-shadow:0 14px 34px rgba(0,0,0,.55);
  animation:wb12 12s infinite;}
@keyframes wb12{0%,5%{opacity:0; transform:translateY(26px) scale(.9);} 10%,50%{opacity:1; transform:translateY(0) scale(1);} 55%,100%{opacity:0; transform:translateY(16px);}}
.wheel-head{padding:6px 0 4px; text-align:center; font-size:8.5px; color:#C9B8DE; border-bottom:1px solid rgba(255,111,181,.22);}
.wheel-head b{color:#FFF; font-weight:800;}
/* 滚轮数字列 */
.wheel{position:relative; height:118px; overflow:hidden; margin:8px 10px 4px;}
.wheel .hl{position:absolute; left:0; right:0; top:50%; height:36px; transform:translateY(-50%);
  background:linear-gradient(100deg, rgba(255,111,181,.30), rgba(182,107,247,.30)); border-radius:9px;}
.wheel .col{position:absolute; left:0; right:0; top:0; text-align:center;}
.wheel .n{height:38px; line-height:38px; font-size:15px; font-weight:800; color:#C9B8DE;}
.wheel .n.on{font-size:19px; color:#FFF; text-shadow:0 0 12px rgba(255,111,181,.6);}
.wcol{animation:wcol12 12s infinite;}
@keyframes wcol12{0%,14%{transform:translateY(-38px);} 30%,44%{transform:translateY(-152px);} 55%,100%{transform:translateY(-38px);}}
/* 蓄力环（SVG）+ 按住提示 */
.ringbox{position:absolute; left:50%; top:50%; transform:translate(-50%,-50%); z-index:13; width:74px; height:74px; opacity:0; animation:ring12 12s infinite;}
@keyframes ring12{0%,29%{opacity:0;} 31%,44%{opacity:1;} 47%,100%{opacity:0;}}
.ringbox svg{transform:rotate(-90deg);}
.ringbox .hold{margin-top:3px; text-align:center; font-size:8px; color:#FFD76E; font-weight:700; letter-spacing:1px;}
.ring-track{fill:none; stroke:rgba(255,255,255,.18); stroke-width:4;}
.ring-fill{fill:none; stroke:#FFD76E; stroke-width:4; stroke-linecap:round;
  stroke-dasharray:207; stroke-dashoffset:207; animation:fill12 12s infinite;}
@keyframes fill12{0%,30%{stroke-dashoffset:207;} 44%{stroke-dashoffset:0;} 100%{stroke-dashoffset:207;}}
/* 震感分级标注 */
.haplog{position:absolute; right:14px; bottom:120px; z-index:13; text-align:right; font-size:8.5px; line-height:1.9;
  color:#9C8CB2; opacity:0; animation:hap12 12s infinite;}
@keyframes hap12{0%,29%{opacity:0;} 31%,46%{opacity:1;} 50%,100%{opacity:0;}}
.haplog b{color:#FFD76E; font-weight:800;}
.haplog .full{color:#FFF; font-size:10px; font-weight:900;}
/* 跳转闪光 */
.jump{position:absolute; inset:0; z-index:14; background:#FFF; opacity:0; animation:jump12 12s infinite;}
@keyframes jump12{0%,43%{opacity:0;} 45%{opacity:.5;} 50%,100%{opacity:0;}}
/* ═══ 第二幕：撕票+票根（已确认稿）═══ */
.ticket{width:296px; position:absolute; top:70px; left:50%; margin-left:-148px; z-index:6;}
.paper{position:relative; background:#241A33; overflow:hidden;}
.holo{position:absolute; inset:0; opacity:.45;
  background:
    radial-gradient(90% 60% at 12% 6%, rgba(255,111,181,.55), transparent 55%),
    radial-gradient(80% 55% at 88% 18%, rgba(182,107,247,.5), transparent 58%),
    radial-gradient(70% 50% at 85% 92%, rgba(255,111,181,.32), transparent 60%);}
.tin{position:relative; z-index:2;}
.thead{display:flex; align-items:center; gap:6px; padding:11px 14px 8px;}
.thead .zn{font-size:10px; font-weight:700; letter-spacing:1px; color:#E9C8EE;}
.thead .zn b{color:rgba(255,255,255,.9); font-weight:900; margin-right:5px;}
.tear{position:relative; height:0; border-top:1.6px dashed rgba(255,214,240,.45); margin:2px 0;}
.tear i{position:absolute; top:-7px; width:14px; height:14px; border-radius:50%; background:#191223;}
.tear i.l{left:-8px;} .tear i.r{right:-8px;}
.tt{font-size:13px; font-weight:800; color:#FFF; line-height:1.45; padding:9px 16px 0;}
.pp{font-size:10.5px; color:rgba(240,230,250,.82); line-height:1.6; padding:6px 16px 0; max-height:60px; overflow:hidden;}
.signrow{display:flex; align-items:center; gap:8px; padding:9px 16px 12px;}
.ava-g{width:30px; height:30px; border-radius:9px; flex-shrink:0;
  background:linear-gradient(135deg,#FF6FB5,#B66BF7); color:#FFF; font-weight:900; font-size:13px;
  display:flex; align-items:center; justify-content:center; border:1.5px solid rgba(255,214,240,.55);}
.signrow .sn{font-size:10.5px; font-weight:700; color:#FFF;}
.topen{margin-left:auto; font-size:10px; font-weight:700; color:#E9C8EE;
  border:1px solid rgba(233,200,238,.4); border-radius:99px; padding:4px 12px;}
.ripwrap{position:absolute; inset:0; z-index:6;}
.ripwrap .ticket{margin:0;}
.hT{clip-path:inset(0 0 46% 0); transform-origin:50% 100%; animation:ripT12 12s infinite;}
.hB{clip-path:inset(54% 0 0 0); transform-origin:50% 0%; animation:ripB12 12s infinite;}
@keyframes ripT12{0%,58%{transform:translate(0,0) rotate(0); opacity:1;}
  64%{transform:translate(-4px,-3px) rotate(-1.2deg);}
  74%,88%{transform:translate(-64px,-140px) rotate(-18deg); opacity:0;}
  94%,100%{transform:translate(0,0) rotate(0); opacity:1;}}
@keyframes ripB12{0%,58%{transform:translate(0,0) rotate(0); opacity:1;}
  66%{transform:translate(5px,3px) rotate(1.4deg);}
  76%,88%{transform:translate(44px,180px) rotate(14deg); opacity:0;}
  94%,100%{transform:translate(0,0) rotate(0); opacity:1;}}
.ripflash{position:absolute; left:6%; right:6%; top:340px; height:2px; z-index:9; opacity:0;
  background:linear-gradient(90deg,transparent,#FFE9F6 30%,#FFD76E 50%,#FFE9F6 70%,transparent);
  animation:rf12 12s infinite;}
@keyframes rf12{0%,60%{opacity:0;} 64%{opacity:1; box-shadow:0 0 20px 5px rgba(255,215,110,.7);} 74%,100%{opacity:0;}}
/* 票根撤销（就近浮现）*/
.stub{position:absolute; z-index:20; left:50%; top:388px; margin-left:-104px; width:208px;
  background:#241A33; border-radius:12px; overflow:hidden;
  border:1.5px solid transparent; background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7);
  background-origin:border-box; background-clip:padding-box,border-box;
  box-shadow:0 12px 30px rgba(0,0,0,.55); display:flex; align-items:stretch;
  animation:stub12 12s infinite;}
@keyframes stub12{0%,72%{opacity:0; transform:translateY(-14px) scale(.92);}
  78%,92%{opacity:1; transform:translateY(0) scale(1);}
  97%,100%{opacity:0;}}
.stub .msg{flex:1; padding:9px 12px; font-size:11px; font-weight:700; color:#F4ECFA; line-height:1.3;}
.stub .msg i{display:block; font-style:normal; font-size:8.5px; font-weight:400; color:#C9B8DE; margin-top:1px;}
.stub .undo{padding:9px 15px; font-size:11.5px; font-weight:900; color:#221830;
  background:linear-gradient(100deg,#FFD76E,#FFC7DE); display:flex; align-items:center;
  border-left:1.6px dashed rgba(255,214,240,.5);}
/* 时间轴说明 */
.timeline{font-size:11.5px; color:var(--ink2); margin-top:8px; display:flex; gap:12px; flex-wrap:wrap;}
.timeline span b{color:var(--pink);}
footer{margin-top:40px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>滚轮交互 v2 · 蓄力确认 + 模态化 + 位置收拢</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">滚轮 v2</span>蓄力确认 · 模态化 · 位置收拢（含真撕票+票根撤销确认稿）</h1>
  <div class="sub">2026-10-15 · 12s 循环演示：胶囊呼出 → 遮罩模态 → 滚动选页 → 按住蓄力（震感分级）→ 满圈跳转 → 切隐藏 → 真撕票 → 就近票根撤销</div>
</header>

<section>
  <h2>① 你的三条建议 → 我的看法</h2>
  <div class="card">
    <p><b>① 呼出滚轮时其他按键禁止操作 —— 同意，模态化。</b>误触版面的根源就是滚轮是「非模态浮条」：呼出后瀑布流还能点。改成呼出即<b>全屏暗化遮罩</b>（吞掉一切手势），点遮罩任意处 = 收起滚轮，滚轮成为唯一焦点。</p>
    <p style="margin-top:8px;"><b>② 长按蓄力到满震感才跳转 —— 同意，这是本轮最值得做的手感。</b>现状长按 400ms 一次重震：不知道要按多久、容易手滑。改成<b>按压蓄力</b>：按住不放 → 震感逐级爬升（tick→tick→中→满）约 0.88s → <b>满震瞬间跳转</b>；松手即取消，零误触。配套一根<b>金色进度环</b>把蓄力可视化（满圈=跳），「何时会跳」从玄学变成看得见。</p>
    <p style="margin-top:8px;"><b>③ 「共xx页」去掉、滚轮移进两者视线 —— 同意，收拢成一组。</b>滚轮改为<b>从页码胶囊正上方展开</b>（底部对齐胶囊上方），「共 xx 页」并进滚轮头部小字 —— 胶囊「2/17」→ 抬眼 → 滚轮+总页数，视线一条线；组件同步收窄。</p>
  </div>
</section>

<section>
  <h2>② 完整流程演示（12s 循环 · 滚轮蓄力 + 真撕票 + 票根撤销）</h2>
  <div class="duo">
    <div class="pcase">
      <div class="pt">第一幕 · 滚轮蓄力跳转</div>
      <div class="stage">
        <div class="feedhint">瀑布流</div>
        <div class="scrim"></div>
        <div class="wheelbox">
          <div class="wheel-head">滚动选页 · 共 <b>17</b> 页</div>
          <div class="wheel">
            <div class="hl"></div>
            <div class="col wcol">
              <div class="n">13</div><div class="n">14</div><div class="n">15</div><div class="n">16</div>
              <div class="n on">17</div><div class="n">1</div><div class="n">2</div><div class="n">3</div><div class="n">4</div><div class="n">5</div><div class="n">6</div>
            </div>
          </div>
        </div>
        <div class="ringbox">
          <svg width="74" height="74" viewBox="0 0 74 74">
            <circle class="ring-track" cx="37" cy="37" r="33"></circle>
            <circle class="ring-fill" cx="37" cy="37" r="33"></circle>
          </svg>
          <div class="hold">按住蓄力</div>
        </div>
        <div class="haplog">
          <b>tick</b> 0.22s<br><b>tick</b> 0.44s<br><b>中震</b> 0.66s<br><span class="full">满震 → 跳转</span>
        </div>
        <div class="jump"></div>
        <div class="pill">2 / 17</div>
      </div>
      <div class="timeline">
        <span><b>0.7s</b> 点胶囊·遮罩暗化·滚轮展开</span>
        <span><b>1.7-3.6s</b> 滚动选页（每刻度 tick）</span>
        <span><b>3.6-5.3s</b> 按住蓄力·环满即跳</span>
      </div>
      <ul class="pnote">
        <li><b>模态遮罩</b>：呼出即暗化，瀑布流零误触；点遮罩=收起</li>
        <li><b>蓄力环</b>：按住时外圈金色进度环 0.88s 充满，「何时跳」看得见；松手=取消</li>
        <li><b>位置收拢</b>：滚轮从胶囊上方展开，「共17页」并入滚轮头部；组件收窄</li>
      </ul>
    </div>

    <div class="pcase">
      <div class="pt">第二幕 · 隐藏 → 真撕票 → 票根撤销（已确认稿）</div>
      <div class="stage">
        <div class="feedhint">瀑布流</div>
        <div class="ripwrap">
          <div class="ticket"><div class="paper hT">
            <div class="holo"></div>
            <div class="tin">
              <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
              <div class="tear"><i class="l"></i><i class="r"></i></div>
              <div class="tt">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼</div>
              <div class="pp">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线。</div>
              <div class="signrow"><div class="ava-g">a</div>
                <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
                <span class="topen">查看完整帖 ›</span></div>
            </div>
          </div></div>
          <div class="ticket"><div class="paper hB">
            <div class="holo"></div>
            <div class="tin">
              <div class="thead"><span class="zn"><b>STAGE1</b>自由搏击区</span></div>
              <div class="tear"><i class="l"></i><i class="r"></i></div>
              <div class="tt">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼</div>
              <div class="pp">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线。</div>
              <div class="signrow"><div class="ava-g">a</div>
                <div><div class="sn">abfk</div><div class="st">2024-09-22</div></div>
                <span class="topen">查看完整帖 ›</span></div>
            </div>
          </div></div>
          <div class="ripflash"></div>
        </div>
        <div class="stub">
          <div class="msg">已隐藏该帖<i>可在 我的-抹布贴 恢复</i></div>
          <div class="undo"><b>撤销</b></div>
        </div>
      </div>
      <div class="timeline">
        <span><b>7s</b> 点隐藏·中震</span>
        <span><b>7.7s</b> 沿撕裂线撕成两半·金闪</span>
        <span><b>7.8-9.2s</b> 就近票根浮现（原按钮位）</span>
        <span><b>7s 不点</b> 自动确认</span>
      </div>
      <ul class="pnote">
        <li><b>真撕票</b>：沿撕裂线拆两层反向飞出 + 金色闪光（你已确认采用）</li>
        <li><b>就近票根</b>：208×40dp 出现在原「隐藏」按钮位，左半信息右半金色撤销</li>
        <li>点撤销 → 绿章「已恢复」反馈；7s 不点自动确认</li>
      </ul>
    </div>
  </div>
</section>

<section>
  <h2>③ 蓄力震感分级（系统兼容两档）</h2>
  <div class="card">
    <table>
      <tr><th>时刻</th><th>Android 10+（振幅可控）</th><th>旧系统（不支持振幅）</th></tr>
      <tr><td>0.22s</td><td>tick 振幅 30%</td><td>EFFECT_TICK</td></tr>
      <tr><td>0.44s</td><td>tick 振幅 55%</td><td>EFFECT_TICK</td></tr>
      <tr><td>0.66s</td><td>中震 振幅 80%</td><td>oneShot 18ms</td></tr>
      <tr><td class="ok">0.88s 满圈</td><td class="ok">HEAVY 振幅 100% → 立即跳转</td><td class="ok">oneShot 46ms → 立即跳转</td></tr>
      <tr><td>任意时刻松手</td><td colspan="2">取消蓄力，不跳转（零误触）</td></tr>
    </table>
    <p class="small" style="margin-top:8px;">实现注：蓄力环 = Canvas arc 随 Animatable 画弧（一次绘制层）；震动 = 现有 Haptic 扩展一个 <span class="spec">charge(ctx, onFull)</span> 方法（内部协程 0.88s 时间轴）；滚轮呼出遮罩 = 一层可点击 Box（宫格时代有现成写法）。总增量约 120 行，无新依赖。</p>
  </div>
</section>

<section>
  <h2>④ 一个取舍提前说</h2>
  <div class="card">
    <p>模态化后「3.5s 无操作自动淡出」可以保留也可以去掉 —— 保留的话，发呆 3.5s 滚轮自动收起（遮罩同收）；去掉则必须点遮罩才收。<b>我建议保留自动淡出</b>（呼出了又不想跳的场景，等它自己走比找地方点更省心），时长放到 4s。</p>
    <p class="small">另外滚轮宽度会从现在收窄约 20%（数字列+头部小字重排），样式与详情页保持同一套组件，只调尺寸参数 —— 两处视觉仍是同一物。</p>
  </div>
</section>

<footer>滚轮 v2 预览 · 未动码 —— 三条建议都已画进演示；无异议即按此实现（滚轮蓄力 + 模态 + 收拢 + 真撕票 + 就近票根一次装机）。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/滚轮交互v2蓄力预览.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
