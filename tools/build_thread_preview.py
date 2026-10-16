# -*- coding: utf-8 -*-
"""帖子长按预览 · 三方案交互演示（场景1 快闪 / 场景2 个人名片 / 场景3 操作台）"""
import base64, os

A = "preview_iridescent/assets"

def b64(name):
    mime = "jpeg" if name.endswith(".jpg") else "png"
    with open(os.path.join(A, name), "rb") as f:
        return "data:image/%s;base64," % mime + base64.b64encode(f.read()).decode()

AVA = b64("avatar.jpg")
PHOTO = b64("photo_dark.jpg")

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
.card{background:var(--card); border:1px solid var(--line); border-radius:14px; padding:18px 20px; margin-top:12px;}
.note{background:linear-gradient(100deg,#FDF0F8,#F3EBFE); border:1px solid #F0D9EE;}
.note b{color:var(--pink);}
table{border-collapse:collapse; width:100%; font-size:13px; margin-top:10px;}
th,td{border:1px solid var(--line); padding:7px 10px; text-align:left; vertical-align:top;}
th{background:#FAF6FC; font-weight:600; white-space:nowrap;}
.spec{color:var(--pink); font-weight:700;}
.ok{color:var(--ok); font-weight:700;}
.tri{display:flex; gap:20px; flex-wrap:wrap; margin-top:14px; align-items:flex-start;}
.case{flex:1; min-width:330px; max-width:400px; padding:12px 0;}
.case.rec{background:linear-gradient(180deg,#FFF7FC,rgba(255,255,255,0)); border:1px solid #F4D9EC; border-radius:16px; padding:12px;}
.case .tag{font-size:14px; font-weight:800; margin-bottom:3px;}
.case .tag .no{display:inline-block; width:22px; height:22px; border-radius:50%; color:#fff; text-align:center; line-height:22px; font-size:12px; margin-right:7px; background:#9A8FA6;}
.case.rec .tag{color:var(--pink);}
.case.rec .tag .no{background:var(--pink);}
.case .scene{font-size:12px; color:var(--ink2); margin-bottom:8px; padding:5px 10px; background:#F4EEF8; border-radius:9px;}
.case .scene b{color:#8B3FD9;}
/* ---------- 手机 mockup ---------- */
.phone{width:320px; border-radius:26px; overflow:hidden; box-shadow:0 8px 28px rgba(70,30,90,.18); border:1px solid rgba(0,0,0,.06); position:relative;}
.mock{background:linear-gradient(180deg,#16101F,#1E1428); padding:0 0 10px; position:relative; height:600px; overflow:hidden;}
.mk-status{display:flex; justify-content:space-between; padding:7px 14px 3px; font-size:10.5px; font-weight:600; color:#A99BBE;}
.mk-topbar{display:flex; gap:6px; padding:4px 10px 8px; align-items:center;}
.mk-chip{font-size:10.5px; padding:3px 10px; border-radius:99px; font-weight:600; background:rgba(255,255,255,.10); color:#A99BBE;}
.mk-chip.sel{background:#FF6FB5; color:#221830;}
.mk-grid{display:grid; grid-template-columns:1fr 1fr; gap:7px; padding:0 8px;}
.mk-card{border-radius:11px; overflow:hidden; position:relative; background:#221830; border:1px solid rgba(255,255,255,.10); box-shadow:0 2px 8px rgba(0,0,0,.45);}
.mk-photo{height:70px; overflow:hidden;}
.mk-photo img{width:100%; height:100%; object-fit:cover; display:block;}
.mk-body{padding:6px 8px 8px;}
.mk-title{font-size:10.5px; font-weight:700; line-height:1.35; height:28px; overflow:hidden; color:#F4ECFA;}
.mk-meta{display:flex; align-items:center; gap:4px; margin-top:4px;}
.mk-ava{width:13px; height:13px; border-radius:50%; object-fit:cover;}
.mk-meta .a{font-size:8.5px; flex:1; overflow:hidden; white-space:nowrap; color:#A99BBE;}
.mk-meta .r{font-size:8.5px; color:#A99BBE;}
.mk-foot{display:flex; justify-content:space-between; padding:0 8px 7px; font-size:8px; color:#A99BBE;}
/* 长按目标卡高亮 */
.target{outline:2px solid #FF6FB5; outline-offset:-2px;}
/* 长按指示圈（循环动画）*/
.ring{position:absolute; width:52px; height:52px; border:2.5px solid rgba(255,111,181,.9); border-radius:50%; z-index:6; display:flex; align-items:center; justify-content:center;}
.ring::after{content:"长按"; font-size:9px; color:#FF9CCB; background:rgba(34,24,48,.9); padding:1px 7px; border-radius:99px;}
.ring, .ring2, .ring3{animation:ringPulse 9s infinite;}
@keyframes ringPulse{0%{opacity:0; transform:scale(.6);} 3%{opacity:1; transform:scale(1);} 6%{opacity:1; transform:scale(1.06);} 9%{opacity:0; transform:scale(1.1);} 100%{opacity:0;}}
/* 三案错相位（任意时刻都有一案处于保持期，观看不齐闪）*/
.c1 .ovA,.c1 .pcA,.c1 .closehint,.c1 .ring{animation-delay:-2s;}
.c2 .ovA,.c2 .pcB,.c2 .closehint,.c2 .ring2{animation-delay:-4.5s;}
.c3 .ov3,.c3 .pcC,.c3 .closehint,.c3 .ring3,.c3 .stamp,.c3 .pulse-ring,.c3 .snack{animation-delay:-7s;}
/* ---------- 预览浮层公共 ---------- */
.ov{position:absolute; inset:0; z-index:10; background:rgba(10,6,16,.62); opacity:0; pointer-events:none;}
.ovA{animation:ovLoop 9s infinite;}
@keyframes ovLoop{0%,8%{opacity:0;} 14%,74%{opacity:1;} 82%,100%{opacity:0;}}
.pcv{position:absolute; left:16px; right:16px; opacity:0;}
.pcA{top:64px; animation:pcLoop 9s infinite;}
@keyframes pcLoop{0%,8%{opacity:0; transform:scale(.9);} 15%,72%{opacity:1; transform:scale(1);} 80%,100%{opacity:0; transform:scale(.92);}}
/* 关闭提示 */
.closehint{position:absolute; top:14px; right:18px; z-index:12; font-size:9px; color:#D8CCE6; background:rgba(34,24,48,.85); border:1px solid rgba(255,111,181,.35); border-radius:99px; padding:2px 9px; opacity:0; animation:ovLoop 9s infinite;}
/* ---------- 方案一 快闪卡 ---------- */
.flash{border-radius:14px; overflow:hidden; background:#241A33; border:1.5px solid transparent;
  background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7,#C636E7);
  background-origin:border-box; background-clip:padding-box,border-box; box-shadow:0 14px 40px rgba(0,0,0,.6);}
.flash .fimg{height:118px; overflow:hidden;}
.flash .fimg img{width:100%; height:100%; object-fit:cover; display:block;}
.flash .fbody{padding:9px 11px 10px;}
.flash .ft{font-size:12.5px; font-weight:800; color:#F7EFFC; line-height:1.4;}
.flash .fs{font-size:10px; color:#B9A9CC; margin-top:5px; line-height:1.55; height:44px; overflow:hidden;}
.flash .fmeta{display:flex; align-items:center; gap:5px; margin-top:7px; padding-top:7px; border-top:1px solid rgba(255,111,181,.18);}
.flash .fmeta img{width:16px; height:16px; border-radius:50%; object-fit:cover;}
.flash .fmeta .n{font-size:9.5px; color:#D8CCE6; flex:1;}
.flash .fmeta .t{font-size:9px; color:#8F80A3;}
/* ---------- 方案二 个人名片 ---------- */
.vcard{border-radius:16px; overflow:hidden; background:#221830;
  border:1.5px solid transparent;
  background-image:linear-gradient(#221830,#221830), linear-gradient(135deg,#FF6FB5,#B66BF7,#C636E7,#FF8FC7);
  background-origin:border-box; background-clip:padding-box,border-box; box-shadow:0 18px 44px rgba(0,0,0,.65);}
.vc-head{display:flex; align-items:center; gap:10px; padding:12px 13px 10px;
  background:radial-gradient(120% 140% at 12% 0%, rgba(182,107,247,.22), transparent 60%);}
.vc-head img{width:46px; height:46px; border-radius:12px; object-fit:cover; border:1.5px solid rgba(255,111,181,.5);}
.vc-id .n{font-size:13.5px; font-weight:800; color:#F7EFFC;}
.vc-id .n span{font-size:9px; font-weight:400; color:#9C8CB2; margin-left:5px;}
.vc-id .badges{margin-top:3px; display:flex; gap:5px; align-items:center;}
.vc-pill{font-size:8px; padding:1px 7px; border-radius:99px; background:#FF6FB5; color:#221830; font-weight:700;}
.vc-pill.ghost{background:rgba(255,111,181,.14); color:#FF9CCB; border:1px solid rgba(255,111,181,.35);}
.vc-follow{margin-left:auto; font-size:9.5px; font-weight:700; color:#FF9CCB; border:1.2px solid rgba(255,111,181,.55); padding:4px 11px; border-radius:99px;}
.vc-sep{height:1px; background:linear-gradient(90deg, rgba(255,111,181,.4), rgba(182,107,247,.15), transparent);}
.vc-body{padding:10px 13px 4px;}
.vc-title{font-size:12.5px; font-weight:800; color:#F7EFFC; line-height:1.45;}
.vc-abs{font-size:10px; color:#B9A9CC; line-height:1.6; margin-top:5px; height:64px; overflow:hidden;}
.vc-stats{display:flex; gap:12px; padding:8px 13px; font-size:9.5px; color:#9C8CB2;}
.vc-stats b{color:#E8DDF4; font-weight:700;}
.vc-cta{margin:4px 13px 12px; text-align:center; font-size:11px; font-weight:800; color:#221830;
  background:linear-gradient(100deg,#FF6FB5,#C97BF0); border-radius:11px; padding:8px 0;}
/* 名片专用动画：滑入+微翻转 */
.pcB{top:52px; animation:vcLoop 9s infinite; transform-origin:50% 0;}
@keyframes vcLoop{0%,8%{opacity:0; transform:translateY(34px) rotateX(9deg) scale(.96);} 15%,72%{opacity:1; transform:translateY(0) rotateX(0) scale(1);} 80%,100%{opacity:0; transform:translateY(20px) scale(.94);}}
/* ---------- 方案三 操作台 ---------- */
.console{border-radius:14px; overflow:hidden; background:#241A33; border:1.5px solid transparent;
  background-image:linear-gradient(#241A33,#241A33), linear-gradient(135deg,#FF6FB5,#B66BF7,#C636E7);
  background-origin:border-box; background-clip:padding-box,border-box; box-shadow:0 14px 40px rgba(0,0,0,.6);}
.console .fimg{height:86px; overflow:hidden;}
.console .fimg img{width:100%; height:100%; object-fit:cover; display:block;}
.console .fbody{padding:8px 11px 6px;}
.console .ft{font-size:11.5px; font-weight:800; color:#F7EFFC; line-height:1.4; height:32px; overflow:hidden;}
.console .fmeta{display:flex; align-items:center; gap:5px; margin-top:4px;}
.console .fmeta img{width:14px; height:14px; border-radius:50%; object-fit:cover;}
.console .fmeta .n{font-size:9px; color:#D8CCE6; flex:1;}
.acts{display:flex; border-top:1px solid rgba(255,111,181,.18); margin-top:7px;}
.act{flex:1; text-align:center; padding:8px 0 9px; font-size:10px; font-weight:700; color:#C9B8DE;}
.act b{display:block; font-size:13px; margin-bottom:1px;}
.act.hide{color:#FF8FA8; border-left:1px solid rgba(255,111,181,.14);}
/* 收藏印章徽章 */
.stamp{position:absolute; top:9px; right:9px; z-index:5; font-size:9px; font-weight:800; color:#FFD76E;
  background:rgba(34,24,48,.92); border:1.5px solid #FFD76E; border-radius:99px; padding:2px 9px;
  opacity:0; animation:stampLoop 9s infinite;}
@keyframes stampLoop{0%,24%{opacity:0; transform:scale(1.7) rotate(-14deg);} 29%{opacity:1; transform:scale(1) rotate(-8deg);} 100%{opacity:1; transform:scale(1) rotate(-8deg);}}
/* 卡片脉冲（收藏反馈）*/
.pulse-ring{position:absolute; inset:-3px; border-radius:16px; border:2px solid rgba(255,215,110,.85); opacity:0; z-index:4; animation:pulseLoop 9s infinite;}
@keyframes pulseLoop{0%,26%{opacity:0;} 32%{opacity:1;} 45%{opacity:0;}}
/* 隐藏折叠：目标卡收起 + 提示条 */
.ov3{position:absolute; inset:0; z-index:10; background:rgba(10,6,16,.62); opacity:0; animation:ov3Loop 9s infinite; pointer-events:none;}
@keyframes ov3Loop{0%,8%{opacity:0;} 14%,52%{opacity:1;} 60%,100%{opacity:0;}}
.pcC{top:64px; animation:pc3Loop 9s infinite;}
@keyframes pc3Loop{0%,8%{opacity:0; transform:scale(.9);} 15%,50%{opacity:1; transform:scale(1);} 58%,100%{opacity:0; transform:scale(.94) translateY(24px);}}
.snack{position:absolute; left:16px; right:16px; bottom:16px; z-index:12; font-size:9.5px; color:#F4ECFA;
  background:rgba(34,24,48,.95); border:1px solid rgba(255,111,181,.35); border-radius:10px; padding:7px 11px;
  display:flex; justify-content:space-between; opacity:0; animation:snackLoop 9s infinite;}
@keyframes snackLoop{0%,56%{opacity:0; transform:translateY(14px);} 62%,84%{opacity:1; transform:translateY(0);} 92%,100%{opacity:0;}}
.snack .u{color:#FF9CCB; font-weight:800;}
/* 规格徽章 */
.hap{display:inline-block; font-size:10.5px; padding:2px 9px; border-radius:8px; background:#F0E7F7; color:#6E5A85; margin:2px 4px 2px 0;}
.hap b{color:#C2247F;}
footer{margin-top:44px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

def feed_grid():
    def photo_card(title, author, replies, target=False, extra_cls=""):
        tg = ' target' if target else ''
        return f'''<div class="mk-card{tg} {extra_cls}">
      <div class="mk-photo"><img src="{PHOTO}"></div>
      <div class="mk-body"><div class="mk-title">{title}</div>
      <div class="mk-meta"><img class="mk-ava" src="{AVA}"><span class="a">{author}</span><span class="r">💬 {replies}</span></div></div>
      <div class="mk-foot"><span>其他</span><span>1天前</span></div>
    </div>'''
    return ('<div class="mk-grid">'
        + photo_card('北京IJIOY漫展微微NSFW图', '永远健康希…', '9')
        + photo_card('给加土豆，她曾解释过不同菜品', '法鲁西奥', '25')
        + photo_card('【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼', 'abfk', '16789', target=True)
        + photo_card('说中国人种最优秀成了法西斯名言', '张哦哦', '17')
        + photo_card('投诉ID：万里独行 田伯光', '蒋公奕萌', '0')
        + photo_card('可以解锁狗叔以前清明节的帖子吗？', 'Gmlazy', '0')
        + '</div>')

def phone_case(inner, ring_xy=None, ring_id="ring"):
    r = ''
    if ring_xy:
        x, y = ring_xy
        r = f'<div class="{ring_id} ring" style="left:{x}px; top:{y}px;"></div>'
    return f'''<div class="phone"><div class="mock">
      <div class="mk-status"><span>05:35</span><span>5G&nbsp;&nbsp;64</span></div>
      <div class="mk-topbar"><span class="mk-chip sel">自由搏击区</span><span class="mk-chip">归墟</span><span class="mk-chip">游戏论坛</span></div>
      {feed_grid()}
      {r}
      {inner}
    </div></div>'''

# 方案一：快闪
CASE1 = phone_case('''
  <div class="ov ovA"></div>
  <div class="closehint">点外部或下滑关闭</div>
  <div class="pcv pcA"><div class="flash">
    <div class="fimg"><img src="{PHOTO}"></div>
    <div class="fbody">
      <div class="ft">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论</div>
      <div class="fs">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵到各版……（首楼摘要 3 行截断）</div>
      <div class="fmeta"><img src="{AVA}"><span class="n">abfk</span><span class="t">2024-09-22 · 💬 16789</span></div>
    </div>
  </div></div>
'''.replace('{PHOTO}', PHOTO).replace('{AVA}', AVA), ring_xy=(150, 210))

# 方案二：个人名片
CASE2 = phone_case('''
  <div class="ov ovA"></div>
  <div class="closehint">下滑关闭 · 点全文进详情</div>
  <div class="pcv pcB"><div class="vcard">
    <div class="vc-head">
      <img src="{AVA}">
      <div class="vc-id">
        <div class="n">abfk <span>229182</span></div>
        <div class="badges"><span class="vc-pill">宝可梦go!</span><span class="vc-pill ghost">stage1 5332 天</span></div>
      </div>
      <div class="vc-follow">+ 关注</div>
    </div>
    <div class="vc-sep"></div>
    <div class="vc-body">
      <div class="vc-title">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论</div>
      <div class="vc-abs">先说结论：本次黑泥事件的源头是上周的鉴证贴，之后逐步发酵……1 楼楼主整理了完整时间线，53 楼有当事方的回应截图，112 楼是版务处理结果。欢迎补充证据链，注意人身攻击直接删……</div>
    </div>
    <div class="vc-stats"><span>💬 <b>16789</b> 回复</span><span>👁 <b>2.1w</b> 查看</span><span>⭐ <b>456</b> 评分</span></div>
    <div class="vc-cta">查看全文 ›</div>
  </div></div>
'''.replace('{AVA}', AVA), ring_xy=(150, 210), ring_id="ring2")

# 方案三：操作台（含收藏→隐藏流程）
CASE3 = phone_case('''
  <div class="ov3"></div>
  <div class="closehint">直接在预览里操作</div>
  <div class="pcv pcC"><div class="console">
    <div class="pulse-ring" style="position:absolute;"></div>
    <div class="stamp">☆ 已收藏</div>
    <div class="fimg"><img src="{PHOTO}"></div>
    <div class="fbody">
      <div class="ft">【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论</div>
      <div class="fmeta"><img src="{AVA}"><span class="n">abfk · 2024-09-22 · 💬 16789</span></div>
      <div class="acts">
        <div class="act"><b>☆</b>收藏</div>
        <div class="act"><b>↗</b>分享</div>
        <div class="act hide"><b>🙈</b>隐藏</div>
      </div>
    </div>
  </div></div>
  <div class="snack"><span>已隐藏该帖 · 将不再出现</span><span class="u">撤销</span></div>
'''.replace('{PHOTO}', PHOTO).replace('{AVA}', AVA), ring_xy=(150, 210), ring_id="ring3")

html = f'''<!DOCTYPE html>
<html lang="zh"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>帖子长按预览 · 三方案</title><style>{CSS}</style></head><body><div class="wrap">

<header>
  <h1><span class="badge">帖子预览 · 三方案</span>长按瀑布流卡片 → 预览（幻彩深板演示）</h1>
  <div class="sub">2026-10-15 · 按你的三个场景各出一案，其中方案② = 你要的「个人名片」效果 · 长按直接进帖已默认排除 · 每案含动画/震动规格</div>
</header>

<section>
  <h2>① 场景 → 方案 映射</h2>
  <div class="card">
    <table>
      <tr><th>你的场景</th><th>使用心理</th><th>对应方案</th><th>设计重心</th></tr>
      <tr><td><b>场景1</b> 滑动中长按瞄一眼 → 关闭</td><td>不打断刷的节奏，快进快出</td><td class="spec">方案① 快闪</td><td>弹出/关闭最快，信息密度最高，无多余按钮</td></tr>
      <tr><td><b>场景2</b> 长按预览 → 点详情 → 放弃关闭</td><td>判断「值不值得进去」，看作者也看内容</td><td class="spec">方案② 个人名片</td><td>作者身份 + 内容摘要双主角，进详情有过渡、返回保持状态</td></tr>
      <tr><td><b>场景3</b> 预览 → 收藏/分享 → 碍眼直接隐藏</td><td>预览即操作，不想多跳一步</td><td class="spec">方案③ 操作台</td><td>操作按钮内嵌预览，收藏/隐藏各有专属动画+震动</td></tr>
    </table>
  </div>
</section>

<section>
  <h2>② 三方案循环演示（每 9 秒一轮：长按 → 弹出 → 交互 → 关闭）</h2>
  <div class="tri">

    <div class="case c1">
      <div class="tag"><span class="no">①</span>快闪 · 场景1</div>
      <div class="scene"><b>场景：</b>滑动首页，没想点进帖，长按瞄一眼就关</div>
      {CASE1}
      <div class="card">
        <p style="font-size:13px;"><b>形态：</b>首图 + 标题 + 首楼摘要 3 行 + 作者行，无任何操作按钮 —— 纯「看一眼」。</p>
        <p class="small" style="margin-top:6px;"><b>动效/震动规格：</b></p>
        <span class="hap"><b>弹出</b> 240ms 弹簧缩放（1.02 轻超调）</span>
        <span class="hap"><b>长按触发</b> 震动 TICK（10ms）</span>
        <span class="hap"><b>关闭</b> 200ms 缩回原卡位 + 原卡彩丝闪一下（「刚看的是它」）</span>
        <span class="hap"><b>关闭震动</b> LIGHT</span>
        <p class="small" style="margin-top:6px;">实现量级：最轻（纯弹窗，不碰瀑布流数据）。</p>
      </div>
    </div>

    <div class="case rec c2">
      <div class="tag"><span class="no">②</span>个人名片 · 场景2 <span style="font-size:11px;color:var(--ok);">★ 你的意向</span></div>
      <div class="scene"><b>场景：</b>长按预览 → 点详情看了会 → 放弃关闭</div>
      {CASE2}
      <div class="card">
        <p style="font-size:13px;"><b>名片映射：</b>头像=作者头像 · 姓名=作者名+uid · 电话行=身份徽章（stage1 5332 天 / 头衔）· 详情=帖子摘要+数据行（回复/查看/评分）· 底部「查看全文」主按钮。</p>
        <p class="small" style="margin-top:6px;"><b>动效/震动规格：</b></p>
        <span class="hap"><b>弹出</b> 300ms 底部滑入 + rotateX 9°→0（像递名片，克制版）</span>
        <span class="hap"><b>进详情</b> container transform：名片放大过渡成详情页 + 震动 MEDIUM</span>
        <span class="hap"><b>放弃返回</b> 名片<b>原位等待</b>（状态保持，已看楼层有记录）</span>
        <span class="hap"><b>关闭</b> 下滑手势缩回 + LIGHT</span>
        <p class="small" style="margin-top:6px;">实现量级：中。深色名片底 #221830 + 彩丝描边（现成元素），头像/徽章/天数都是现有数据。</p>
      </div>
    </div>

    <div class="case c3">
      <div class="tag"><span class="no">③</span>操作台 · 场景3</div>
      <div class="scene"><b>场景：</b>预览后觉得有意思 → 收藏/分享 → 碍眼直接隐藏</div>
      {CASE3}
      <div class="card">
        <p style="font-size:13px;"><b>形态：</b>快照卡 + 底部操作栏（☆收藏 / ↗分享 / 🙈隐藏——沿用抹布贴语义）。<b>演示循环里可以看到完整流程</b>：弹出 → 收藏（金星印章 + 金色彩丝脉冲）→ 隐藏（卡片折叠消失）→ 底部「已隐藏 · 撤销」提示条。</p>
        <p class="small" style="margin-top:6px;"><b>动效/震动规格：</b></p>
        <span class="hap"><b>收藏</b> 金色印章盖章动画（scale 1.7→1 旋转 -14°→-8°）+ 卡缘金色彩丝脉冲一圈 + 震动 success（双段）</span>
        <span class="hap"><b>隐藏</b> 卡片高度折叠 260ms + 瀑布流下方卡上滑补位（交错 40ms）+ 震动 LIGHT + Snackbar 5s 可撤销</span>
        <span class="hap"><b>按钮按下</b> scale 0.94 + TICK</span>
        <p class="small" style="margin-top:6px;">实现量级：最重（要动瀑布流数据：隐藏需落库/会话内过滤 + 补位动画）。但「隐藏」与已有抹布贴功能天然衔接。</p>
      </div>
    </div>

  </div>
</section>

<section>
  <h2>③ 我的看法</h2>
  <div class="card note">
    <b>为什么「名片」适合瀑布流预览：</b>瀑布流卡片本来只有「标题+作者小行」，长按预览的价值就是<b>补全判断信息</b> —— 而判断一个帖子值不值得进，先看人（作者等级/活跃度）再看内容（摘要+热度数据）。名片式双区布局正好把这两层信息分开：头部是「人」（头像大图+身份徽章），身体是「事」（标题+摘要），底部数据行收尾。而且头像、徽章、注册天数、回复/查看/评分<b>全是 App 现成数据</b>，不用新增接口。<br><br>
    <b>三案不互斥：</b>它们是同一个预览浮层的<b>三种内容密度</b> —— 底层骨架（弹出/关闭手势、背景压暗）是同一套，方案①是骨架+快照，方案②换名片内容区，方案③在快照下加操作栏。真要做，可以<b>先做骨架+方案①</b>（最轻，一版就能上手验），名片和操作台作为同骨架的两种内容模式逐步加。<br>
    <span class="small">震动统一走现有 Haptic 封装（success/medium 都有现成方法）；动画全部 Compose 原生（scale/rotateX/AnimatedVisibility），无新依赖。</span>
  </div>
</section>

<footer>帖子长按预览 · 三方案演示 · 仅预览未改码 —— 选一个起点（建议①打骨架），或说改动点。</footer>
</div></body></html>'''

os.makedirs("preview_iridescent", exist_ok=True)
out = "preview_iridescent/帖子长按预览三方案.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("生成:", out, len(html) // 1024, "KB")
