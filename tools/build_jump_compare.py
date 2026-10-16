# -*- coding: utf-8 -*-
"""跳转方案对比预览：01 长按滚轮（楼层级精确跳）vs 02 iPod 转盘（页级手感翻）
可交互演示 + 想法/难点/实用性分析"""
import os

CSS = """
:root{--pri:#5E6AD2;--pri-soft:rgba(94,106,210,.16);--bg:#08090A;--surf:#0F1011;--surf2:#141517;
--bd:rgba(255,255,255,.08);--bds:rgba(255,255,255,.14);--tx:#EDEEF2;--sub:#8A8F98;--dim:#62666D;
--pink:#D6339C;--gold:#FFD76E;--font:"PingFang SC","Microsoft YaHei",system-ui;--mono:ui-monospace,Consolas;}
*{box-sizing:border-box;margin:0;padding:0}
body{background:var(--bg);color:var(--tx);font-family:var(--font);padding:32px 20px 90px;-webkit-font-smoothing:antialiased}
.wrap{max-width:1180px;margin:0 auto}
header .k{font-family:var(--mono);font-size:11px;letter-spacing:.18em;color:var(--pri);text-transform:uppercase}
header h1{font-size:26px;font-weight:650;margin-top:8px}header h1 em{font-style:normal;color:var(--pri)}
header p{margin-top:10px;font-size:13px;color:var(--sub);line-height:1.8}
h2{font-size:18px;border-left:3px solid var(--pri);padding-left:10px;margin:34px 0 14px}
.duo{display:grid;grid-template-columns:1fr 1fr;gap:22px}
@media(max-width:980px){.duo{grid-template-columns:1fr}}
.case{background:var(--surf);border:1px solid var(--bd);border-radius:16px;padding:14px}
.case .ct{font-size:14.5px;font-weight:700}.case .cs{font-size:11.5px;color:var(--sub);margin:4px 0 10px;line-height:1.7}
.phone{background:#141517;border:1px solid var(--bds);border-radius:26px;overflow:hidden;height:560px;position:relative;display:flex;flex-direction:column}
.appbar{padding:12px 14px;background:rgba(8,9,10,.85);border-bottom:1px solid var(--bd);font-size:12.5px;font-weight:600;position:relative;z-index:5}
.appbar span{display:block;font-family:var(--mono);font-size:10px;color:var(--dim);font-weight:400;margin-top:2px}
.list{flex:1;overflow-y:auto;padding:10px;scrollbar-width:none;position:relative}
.list::-webkit-scrollbar{width:0}
.floor{background:var(--surf2);border:1px solid var(--bd);border-radius:12px;padding:9px 11px;margin-bottom:8px;transition:background .45s,border-color .45s}
.floor.flash{background:var(--pri-soft);border-color:rgba(94,106,210,.55)}
.floor .h{display:flex;gap:6px;align-items:center;font-size:10.5px;color:var(--sub)}
.floor .h .n{margin-left:auto;font-family:var(--mono);font-size:9.5px;border:1px solid var(--bd);border-radius:99px;padding:1px 7px}
.floor .b{font-size:11.5px;color:var(--sub);margin-top:4px;line-height:1.6}
.pill{position:absolute;right:12px;bottom:14px;z-index:10;display:flex;align-items:center;gap:6px;height:34px;padding:0 12px;
background:rgba(20,21,23,.9);border:1px solid var(--bds);border-radius:99px;cursor:pointer;box-shadow:0 8px 22px rgba(0,0,0,.45);transition:border-color .2s}
.pill:hover{border-color:rgba(94,106,210,.6)}
.pill .dot{width:5px;height:5px;border-radius:50%;background:var(--pri)}
.pill .v{font-family:var(--mono);font-size:11px}.pill .v i{font-style:normal;color:var(--dim)}
.pill .ch{color:var(--dim);font-size:10px}
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.55);opacity:0;pointer-events:none;transition:opacity .3s;z-index:14}
.scrim.show{opacity:1;pointer-events:auto}
.sheet{position:absolute;left:0;right:0;bottom:0;background:var(--surf2);border-top:1px solid var(--bds);border-radius:18px 18px 0 0;
padding:9px 0 20px;transform:translateY(103%);transition:transform .38s cubic-bezier(.22,.61,.36,1);z-index:15;box-shadow:0 -20px 50px rgba(0,0,0,.5)}
.sheet.show{transform:translateY(0)}
.grab{width:34px;height:4px;border-radius:99px;background:rgba(255,255,255,.12);margin:0 auto 9px}
.sh{display:flex;justify-content:space-between;align-items:center;padding:0 16px 10px}
.sh h4{font-size:12px}.chip{font-family:var(--mono);font-size:9.5px;color:var(--dim);border:1px solid var(--bd);border-radius:99px;padding:3px 9px;transition:all .25s}
.chip.armed{color:var(--pri);border-color:rgba(94,106,210,.5);background:var(--pri-soft)}
.wwrap{position:relative;height:200px;overflow:hidden}
.wwrap::before,.wwrap::after{content:"";position:absolute;left:0;right:0;height:66px;z-index:3;pointer-events:none}
.wwrap::before{top:0;background:linear-gradient(var(--surf2),rgba(20,21,23,0))}
.wwrap::after{bottom:0;background:linear-gradient(rgba(20,21,23,0),var(--surf2))}
.band{position:absolute;left:10px;right:10px;top:78px;height:44px;background:var(--pri-soft);border:1px solid rgba(94,106,210,.35);border-radius:10px;z-index:1}
.jband{position:absolute;left:10px;right:10px;top:78px;height:44px;z-index:4;cursor:pointer;touch-action:none}
.jband .ring{position:absolute;inset:0;border-radius:10px;border:1.5px solid var(--pri);opacity:0;transform:scaleX(0);transform-origin:left center}
.jband.holding .ring{opacity:1;animation:rf 1000ms linear forwards}
.jband .hh{position:absolute;left:50%;transform:translateX(-50%);top:calc(100% + 8px);font-family:var(--mono);font-size:9.5px;color:var(--dim);white-space:nowrap}
.jband.holding .hh{color:var(--pri)}
.jband.done .ring{opacity:1;transform:scaleX(1);background:var(--pri-soft)}
@keyframes rf{from{transform:scaleX(0)}to{transform:scaleX(1)}}
.wheel{height:200px;overflow-y:scroll;scrollbar-width:none;z-index:2;position:relative;padding:78px 0}
.wheel::-webkit-scrollbar{width:0}
.wit{height:44px;display:flex;align-items:center;justify-content:center;gap:8px;font-family:var(--mono);font-size:14px;color:var(--dim);opacity:.42;transition:all .18s;user-select:none}
.wit .lb{font-family:var(--font);font-size:10px}
.wit.sel{opacity:1;color:var(--tx);transform:scale(1.05)}.wit.sel .lb{color:var(--pri)}
.toast{position:absolute;left:50%;bottom:96px;transform:translateX(-50%);background:rgba(20,21,23,.95);border:1px solid var(--bds);
font-family:var(--mono);font-size:10.5px;padding:8px 15px;border-radius:99px;opacity:0;transition:all .25s;z-index:30;pointer-events:none}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}
/* ═══ 转盘 ═══ */
.pod{flex:1;display:flex;align-items:center;justify-content:center;position:relative;background:radial-gradient(90% 70% at 50% 40%,#17181B,#0B0C0E)}
.pview{position:absolute;inset:0;overflow-y:auto;padding:12px;scrollbar-width:none;scroll-behavior:smooth}
.pview::-webkit-scrollbar{width:0}
.pcard{background:var(--surf2);border:1px solid var(--bd);border-radius:12px;padding:10px 12px;margin-bottom:8px}
.pcard .h{display:flex;gap:6px;font-size:10.5px;color:var(--sub);margin-bottom:3px}
.pcard .b{font-size:11.5px;color:var(--sub);line-height:1.6}
.pnum{position:absolute;top:12px;left:14px;font-family:var(--mono);font-size:10px;color:var(--dim);z-index:2}
.dial{position:absolute;bottom:12px;left:50%;transform:translateX(-50%);width:210px;height:210px;border-radius:50%;z-index:5;
background:radial-gradient(circle at 50% 42%,#232529,#15161A 68%);border:1px solid var(--bds);
box-shadow:0 14px 40px rgba(0,0,0,.6),inset 0 0 0 1px rgba(255,255,255,.02);touch-action:none;cursor:grab;user-select:none}
.dial:active{cursor:grabbing}
.dial .ticks{position:absolute;inset:0;border-radius:50%}
.dial .tk{position:absolute;left:50%;top:50%;width:2px;height:9px;background:rgba(255,255,255,.14);border-radius:2px;transform-origin:50% 0}
.dial .tk.tick{background:#8B95E8;box-shadow:0 0 8px rgba(139,149,232,.8)}
.dial .cbtn{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:64px;height:64px;border-radius:50%;
background:radial-gradient(circle at 50% 40%,#2A2C31,#191A1E);border:1px solid var(--bds);display:flex;align-items:center;justify-content:center;
font-size:9px;color:var(--sub);letter-spacing:1px;cursor:pointer}
.dial .wbtn{position:absolute;width:0;height:0;display:flex;align-items:center;justify-content:center;font-size:9px;color:var(--sub);cursor:pointer}
.dial .wbtn span{position:absolute;white-space:nowrap;font-family:var(--mono)}
.dial .boost{position:absolute;top:-24px;left:50%;transform:translateX(-50%);font-family:var(--mono);font-size:9px;color:var(--pri);opacity:0;transition:opacity .2s}
.dial.boosting .boost{opacity:1}
.pstat{position:absolute;top:12px;right:14px;font-family:var(--mono);font-size:10px;color:var(--pri);z-index:2}
.ana{display:grid;grid-template-columns:1fr;gap:14px;margin-top:14px}
.card{background:var(--surf);border:1px solid var(--bd);border-radius:14px;padding:14px 18px}
.card h3{font-size:14.5px;margin-bottom:8px}.card h3 b{color:var(--pri)}
.card p{font-size:13px;color:var(--sub);line-height:1.8}
.card table{width:100%;border-collapse:collapse;font-size:12.5px;margin-top:8px}
.card th,.card td{border:1px solid var(--bd);padding:6px 10px;text-align:left}
.card th{background:rgba(255,255,255,.03)}
.ok{color:#0E9F6E;font-weight:700}.mid{color:#C9A227;font-weight:700}.bad{color:#E5484D;font-weight:700}
footer{margin-top:34px;padding-top:14px;border-top:1px solid var(--bd);font-size:12px;color:var(--dim)}
"""

JS = """
/* ═══ Demo A：01 长按滚轮（楼层 1..200）═══ */
(function(){
  const list = document.getElementById('a-list');
  let hf = '';
  for (let i=1;i<=200;i++) hf += `<div class="floor" data-r="${i}"><div class="h"><span>楼友${(i*7)%20+1}</span><span class="n">#${String(i).padStart(3,'0')}</span></div><div class="b">楼层内容占位 ${i} · 长按页码胶囊弹滚轮跳层。</div></div>`;
  list.innerHTML = hf;

  const pill=document.getElementById('a-pill'),pillV=document.getElementById('a-pv'),
        scrim=document.getElementById('a-scrim'),sheet=document.getElementById('a-sheet'),
        wheel=document.getElementById('a-wheel'),jb=document.getElementById('a-jb'),
        hh=document.getElementById('a-hh'),chip=document.getElementById('a-chip'),
        toast=document.getElementById('a-toast'),ITEM=44;
  let wheelRow=1, settled=false, holdT=null, holding=false, toastT=null;
  for (let i=1;i<=200;i++){const d=document.createElement('div');d.className='wit';d.dataset.r=i;d.innerHTML=`<span>${String(i).padStart(3,'0')}</span><span class="lb">楼层</span>`;wheel.appendChild(d);}
  function sel(){const idx=Math.round(wheel.scrollTop/ITEM)+1;wheelRow=Math.min(Math.max(idx,1),200);
    for(const it of wheel.children)it.classList.toggle('sel',+it.dataset.r===wheelRow);}
  pill.onclick=()=>{scrim.classList.add('show');sheet.classList.add('show');arm(false);
    requestAnimationFrame(()=>{wheel.scrollTop=(wheelRow-1)*ITEM;sel();});};
  scrim.onclick=closeA;
  function closeA(){scrim.classList.remove('show');sheet.classList.remove('show');cancelHold();}
  function arm(on){settled=on;chip.classList.toggle('armed',on);chip.textContent=on?'可长按跳转':'滚轮停稳后长按';
    hh.textContent=on?'按住 1s 跳转':'先停稳滚轮';}
  let sT=null;
  wheel.addEventListener('scroll',()=>{sel();arm(false);clearTimeout(sT);sT=setTimeout(()=>arm(true),180);},{passive:true});
  function startHold(){if(!settled){flash('滚轮还在滚动，停稳后才能长按');return;}
    if(holding)return;holding=true;jb.classList.add('holding');hh.textContent='按住…不要松手';
    holdT=setTimeout(()=>{jb.classList.remove('holding');jb.classList.add('done');hh.textContent='已跳转';
      const t=list.querySelector(`.floor[data-r="${wheelRow}"]`);
      if(t){t.scrollIntoView({behavior:'smooth',block:'start'});setTimeout(()=>{t.classList.add('flash');flash(`已跳转到 #${String(wheelRow).padStart(3,'0')} 楼层`);
        setTimeout(()=>t.classList.remove('flash'),1400);},320);}
      pillV.textContent=wheelRow;setTimeout(()=>{jb.classList.remove('done');closeA();},900);holding=false;},1000);}
  function cancelHold(){clearTimeout(holdT);if(holding){jb.classList.remove('holding');hh.textContent=settled?'按住 1s 跳转':'先停稳滚轮';}holding=false;}
  jb.addEventListener('pointerdown',e=>{e.preventDefault();startHold();});
  jb.addEventListener('pointerup',cancelHold);jb.addEventListener('pointerleave',cancelHold);
  function flash(m){toast.textContent=m;toast.classList.add('show');clearTimeout(toastT);toastT=setTimeout(()=>toast.classList.remove('show'),1500);}
})();

/* ═══ Demo B：02 iPod 转盘（12 页）═══ */
(function(){
  const PAGES=12, RAD_PER=Math.PI/6, TICK=Math.PI/30;
  const pv=document.getElementById('b-list'),dial=document.getElementById('b-dial'),
        pnum=document.getElementById('b-num'),stat=document.getElementById('b-stat');
  let hf='';
  for(let p=1;p<=PAGES;p++)for(let j=0;j<3;j++){const n=(p-1)*3+j+1;
    hf+=`<div class="pcard" data-p="${p}"><div class="h"><span>第 ${p} 页 · #${n}</span></div><div class="b">页 ${p} 内容块 ${j+1} —— 沿转环滑动翻页，每档 tick。</div></div>`;}
  pv.innerHTML=hf;
  // 刻度 24 个
  const tk=document.getElementById('b-ticks');
  for(let i=0;i<24;i++){const d=document.createElement('div');d.className='tk';d.style.transform=`translate(-50%,0) rotate(${i*15}deg) translateY(18px)`;tk.appendChild(d);}
  // 中央按钮 + 四向
  document.getElementById('b-center').onclick=()=>{const t=prompt('跳到第几页？(1-12)',String(cur+1));if(t)gotoP(Math.min(Math.max(+t-1,0),PAGES-1));};
  const offset={v:0};let cur=0,lastA=0,lastT=0,tickAcc=0,emaV=0,rot=false,raf=0,boostT=null,boosting=false;
  function apply(o){offset.v=o;
    // 视口滚到对应位置（连续）
    const maxScroll=pv.scrollHeight-pv.clientHeight;
    pv.scrollTop=Math.max(0,Math.min(maxScroll,(o/(PAGES-1))*maxScroll));}
  function render(){cur=Math.round(Math.min(Math.max(offset.v,0),PAGES-1));
    pnum.textContent=`页 ${cur+1} / ${PAGES}`;stat.textContent=`offset ${offset.v.toFixed(2)}`;
    pv.querySelectorAll('.pcard').forEach(c=>c.classList.toggle('flash',+c.dataset.p-1===cur));}
  function tickFx(){const tks=tk.children,i=Math.floor(((offset.v%2)+2)%2*12);for(const t of tks)t.classList.remove('tick');
    if(tks[i])tks[i].classList.add('tick');setTimeout(()=>tks[i]&&tks[i].classList.remove('tick'),110);
    if(navigator.vibrate)try{navigator.vibrate(10);}catch(e){}
    dial.style.transform=`translateX(-50%) rotate(${(offset.v*30)%360}deg)`;}
  function angleOf(e){const r=dial.getBoundingClientRect();return Math.atan2(e.clientY-(r.top+r.height/2),e.clientX-(r.left+r.width/2));}
  function norm(d){while(d>Math.PI)d-=Math.PI*2;while(d<-Math.PI)d+=Math.PI*2;return d;}
  dial.addEventListener('pointerdown',e=>{if(e.target.closest('.cbtn'))return;
    cancelAnimationFrame(raf);rot=true;lastA=angleOf(e);lastT=performance.now();tickAcc=0;emaV=0;
    clearTimeout(boostT);boostT=setTimeout(()=>{boosting=true;dial.classList.add('boosting');},600);
    dial.setPointerCapture(e.pointerId);e.preventDefault();});
  dial.addEventListener('pointermove',e=>{if(!rot)return;const now=performance.now();const a=angleOf(e);const d=norm(a-lastA);lastA=a;
    const dt=Math.max(now-lastT,1);lastT=now;emaV=emaV*0.8+(d/dt)*0.2;
    const per=boosting?(RAD_PER/1.5):RAD_PER;
    apply(offset.v+d/per);tickAcc+=d;
    while(Math.abs(tickAcc)>=TICK){tickFx();tickAcc-=Math.sign(tickAcc)*TICK;}});
  function end(){if(!rot)return;rot=false;clearTimeout(boostT);boosting=false;dial.classList.remove('boosting');
    const v=emaV/RAD_PER;if(Math.abs(v)>0.0006)fling();else snap(Math.round(offset.v));render();}
  dial.addEventListener('pointerup',end);dial.addEventListener('pointercancel',end);
  function fling(){cancelAnimationFrame(raf);const s=()=>{apply(offset.v+ (arguments,0) || apply(offset.v+vel*16));};
    let vel=emaV/RAD_PER;const step=()=>{apply(offset.v+vel*16);vel*=0.94;
      if(Math.abs(vel)>0.0008)raf=requestAnimationFrame(step);else snap(Math.round(offset.v));};step();}
  function snap(t){t=Math.min(Math.max(t,0),PAGES-1);const st=offset.v,dist=t-st,t0=performance.now();
    const a=(now)=>{const p=Math.min((now-t0)/200,1);offset.v=st+dist*(1-Math.pow(1-p,3));apply(offset.v);render();
      if(p<1)requestAnimationFrame(a);else{offset.v=t;apply(t);render();if(navigator.vibrate)try{navigator.vibrate(25);}catch(e){}}};
    requestAnimationFrame(a);}
  function gotoP(n){cancelAnimationFrame(raf);snap(n);}
  // 初始
  apply(0);render();
})();
"""

# Demo A / B 的 HTML 骨架
DEMO_A = '''
<div class="phone">
  <div class="appbar">【讨论】关于新版本更新后的使用感受 <span>Stage1st · 200 楼</span></div>
  <div class="list" id="a-list"></div>
  <div class="pill" id="a-pill"><div class="dot"></div><div class="v"><span id="a-pv">1</span><i> / 200</i></div><div class="ch">▲</div></div>
  <div class="scrim" id="a-scrim"></div>
  <div class="sheet" id="a-sheet">
    <div class="grab"></div>
    <div class="sh"><h4>跳转到楼层</h4><div class="chip" id="a-chip">滚轮停稳后长按</div></div>
    <div class="wwrap">
      <div class="band"></div>
      <div class="wheel" id="a-wheel"></div>
      <div class="jband" id="a-jb"><div class="ring"></div><div class="hh" id="a-hh">按住 1s 跳转</div></div>
    </div>
  </div>
  <div class="toast" id="a-toast"></div>
</div>
'''

DEMO_B = '''
<div class="phone">
  <div class="appbar">ipod 转盘翻页 <span>沿转环滑动 · 每档 tick · 松手吸附</span></div>
  <div class="pod">
    <div class="pnum" id="b-num">页 1 / 12</div>
    <div class="pstat" id="b-stat">offset 0.00</div>
    <div class="pview" id="b-list"></div>
    <div class="dial" id="b-dial">
      <div class="boost">加速 ×1.5</div>
      <div class="ticks" id="b-ticks"></div>
      <div class="cbtn" id="b-center">MENU</div>
    </div>
  </div>
</div>
'''

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>跳转方案对比 · 01 长按滚轮 vs 02 iPod 转盘</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <div class="k">Interaction Comparison · S1Client 跳转方案选型</div>
  <h1>两个原型 <em>可运行对比</em>：01 长按滚轮 vs 02 iPod 转盘</h1>
  <p>两个演示都<strong>真实可玩</strong>：左边点页码胶囊弹滚轮 → 停稳后按住选中带 1 秒跳层；右边沿转环拖动翻页（每档 tick + 震动）、松手惯性吸附、长按转环加速 ×1.5、中央 MENU 跳页。</p>
</header>

<h2>① 并排可玩演示</h2>
<div class="duo">
  <div class="case">
    <div class="ct">方案 01 · 长按滚轮（BottomSheet 楼层跳转）</div>
    <div class="cs">定位：<b style="color:var(--pri)">详情页楼层级精确跳转</b> + 首页页码跳转。交付包自带 550 行 Android 原生工程（WheelPickerView 319 / HoldJumpButton 125 / Sheet 106），可直接移植。</div>
    {DEMO_A}
  </div>
  <div class="case">
    <div class="ct">方案 02 · iPod 点按式转盘（页级手感翻页）</div>
    <div class="cs">定位：<b style="color:var(--pri)">相邻页连续翻页手感</b>（ ViewPager 替代/补充）。转环 atan2 角度映射 + 每档 tick + 惯性吸附，30°=1 页。</div>
    {DEMO_B}
  </div>
</div>

<h2>② 想法 · 难点 · 实用性</h2>
<div class="ana">
  <div class="card">
    <h3>方案 01 · <b>长按滚轮</b>（楼层级）</h3>
    <p><b>想法</b>：跳转是「低频但要求准」的操作——滚轮负责<b>看得见的全量索引</b>（001-200 全部可见可滚），长按蓄力负责<b>防误触</b>。视觉重心在中央选中带，环形进度走在带上，视线不用离开。</p>
    <table>
      <tr><th>维度</th><th>评估</th></tr>
      <tr><td>落实难点</td><td><span class="ok">低</span> —— 交付包已含 550 行原生工程（View 体系，与现有 EdgePageBar 同栈），移植成本 ≈ 把 WheelPickerView 的数据源从 200 楼层换成 S1 楼层/页码，HoldJumpButton 原样搬。首页 4094 页需验证长列表滚动性能（Canvas 直绘理论无压力）。</td></tr>
      <tr><td>实用性</td><td><span class="ok">★★★★★</span> —— 楼层跳转是论坛 App 刚需；4094 页/200 楼都是「全量索引 + 精确落点」场景，滚轮是正解。</td></tr>
      <tr><td>与现有代码关系</td><td>现有 EdgePageBar（贴边窄滚轮+蓄力）可视为它的「未完成版」——交互三件套（停稳检测/中央选中带/长按 ring）正是之前缺的东西，等于<b>补完而非重写</b>。</td></tr>
    </table>
  </div>

  <div class="card">
    <h3>方案 02 · <b>iPod 转盘</b>（页级手感）</h3>
    <p><b>想法</b>：翻页是「高频且讲手感」的操作——转盘把<b>翻页变成玩具</b>：角度跟手、档位 tick（触觉+听觉）、惯性甩动吸附。iPod Classic 的情怀加成让「翻 12 页」这个动作本身有愉悦感。</p>
    <table>
      <tr><th>维度</th><th>评估</th></tr>
      <tr><td>落实难点</td><td><span class="mid">中</span> —— atan2 转盘 View ≈300 行 Kotlin（JS 逻辑直译）；tick 音 SoundPool 预载；<b>真难点是空间与场景</b>：转盘占屏大（常驻挡内容），需呼出式；且 S1 首页 4094 页用转盘翻要转 100+ 圈（加速档缓解但效率不如滚轮），<b>只适合详情页翻页（每帖几十页）</b>。</td></tr>
      <tr><td>实用性</td><td><span class="mid">★★★☆☆</span> —— 手感满分、效率受限：相邻页快翻是甜区；跨页跳转仍需回到方案 01。建议定位为<b>详情页翻页的手感升级</b>，而非跳转方案本身。</td></tr>
      <tr><td>风险</td><td>情怀交互对小众论坛 App 是双刃剑——喜欢的人爱不释手，无 iPod 记忆的用户有学习成本。建议做成<b>可关闭的实验开关</b>。</td></tr>
    </table>
  </div>

  <div class="card">
    <h3>③ <b>我的建议</b>：一主一辅</h3>
    <p><b>主方案 = 01 长按滚轮</b>：直接移植交付包工程——首页页码 + 详情页楼层共用一套 sheet（数据源切换），替换现有 EdgePageBar 的未完成形态；现有蓄力交互并入 HoldJumpButton（停稳检测 + ring 进度，正是之前缺的三件套）。</p>
    <p style="margin-top:8px"><b>辅方案 = 02 的「tick 灵魂」</b>：转盘 UI 缓一缓（或做成实验开关），但把「每档 tick 震动 + 机械轻响」移植进详情页左右翻页（ViewPager 翻页时一档一 tick）——用 20 行代码拿到 80% 的手感灵魂。</p>
    <p style="margin-top:8px" class="small">两案不互斥：01 解决「跳得准」，02 的 tick 解决「翻得爽」。先后顺序：先 01（有现成工程、刚需），02 作为二期彩蛋。</p>
  </div>
</div>

<script>{JS}</script>
<footer>S1Client 跳转方案选型 · 2026-10-15 · 交付包源：桌面/S1交互原型交付包 · 定案后我直接从 01 的 android/ 工程开搬。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/跳转方案对比.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html)//1024, "KB")
