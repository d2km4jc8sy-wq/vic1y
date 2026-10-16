# -*- coding: utf-8 -*-
"""幻彩边框方案对比预览（封面固定为建议方案；A=原版统一 / B=彩色亮丝）"""
import base64, os

A = "preview_iridescent/assets"

def b64(name, mime="png"):
    with open(os.path.join(A, name), "rb") as f:
        return "data:image/%s;base64," % mime + base64.b64encode(f.read()).decode()

IMG = {k: b64(v, "jpeg" if v.endswith(".jpg") else "png") for k, v in {
    "photo_light": "photo_light.jpg", "photo_dark": "photo_dark.jpg",
    "avatar": "avatar.jpg",
}.items()}

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
.cmp{display:flex; gap:22px; flex-wrap:wrap; margin-top:12px;}
.side{flex:1; min-width:320px; max-width:560px;}
.side .tag{font-size:13px; font-weight:700; margin-bottom:8px;}
.side .tag.a{color:var(--ink2);}
.side .tag.b{color:var(--pink);}
.phone{width:320px; border-radius:26px; overflow:hidden; box-shadow:0 8px 28px rgba(70,30,90,.18); border:1px solid rgba(0,0,0,.06);}
.legend{margin-top:12px; font-size:12.5px;}
.legend li{margin:5px 0; list-style:none; padding-left:24px; position:relative;}
.legend li i{position:absolute; left:0; top:1px; width:17px; height:17px; border-radius:50%; background:#8A8296; color:#fff; font-size:11px; font-style:normal; display:flex; align-items:center; justify-content:center; font-weight:700;}
.legend li.pink i{background:var(--pink);}
/* ---------- mockup ---------- */
.mock{background:linear-gradient(180deg,#F2EAF6,#FBE4F0); padding:0 0 10px;}
.mock.dark{background:linear-gradient(180deg,#16101F,#1E1428);}
.mk-status{display:flex; justify-content:space-between; padding:7px 14px 3px; font-size:10.5px; font-weight:600;}
.mock.light .mk-status{color:#3D2438;}
.mock.dark .mk-status{color:#A99BBE;}
.mk-topbar{display:flex; gap:6px; padding:4px 10px 8px; align-items:center;}
.mk-chip{font-size:10.5px; padding:3px 10px; border-radius:99px; font-weight:600;}
.mock.light .mk-chip{background:rgba(255,255,255,.72); color:#8A6B85;}
.mock.light .mk-chip.sel{background:#D6339C; color:#fff;}
.mock.dark .mk-chip{background:rgba(255,255,255,.10); color:#A99BBE;}
.mock.dark .mk-chip.sel{background:#FF6FB5; color:#221830;}
.mk-grid{display:grid; grid-template-columns:1fr 1fr; gap:7px; padding:0 8px;}
/* A 方案：原版统一（白发丝 / 无边框）*/
.mk-card{border-radius:11px; overflow:hidden; position:relative;}
.mock.light .mk-card{background:#FFFFFF; border:1px solid rgba(255,255,255,.65); box-shadow:0 2px 6px rgba(120,60,120,.10);}
.mock.dark .mk-card{background:#221830; border:1px solid rgba(255,255,255,.10); box-shadow:0 2px 8px rgba(0,0,0,.45);}
/* B 方案：彩色亮丝（1.5px 渐变 + 微光）*/
.mk-card.hair{padding:1.5px; border:none; background:linear-gradient(135deg,#D6339C,#FF6FA6,#B66BF7,#FF8FC7); box-shadow:0 0 5px rgba(214,51,156,.10);}
.mock.dark .mk-card.hair{background:linear-gradient(135deg,#FF6FB5,#B66BF7,#C636E7,#BE80F8); box-shadow:0 0 5px rgba(255,111,181,.10);}
.mk-card.hair>.in{background:#FFFFFF; border-radius:9.5px; height:100%;}
.mock.dark .mk-card.hair>.in{background:#221830;}
/* 流光封面（固定，两方案一致）*/
.flow{position:relative; height:104px; overflow:hidden;}
.flow .base{position:absolute; inset:0;}
.mock.light .flow .base{background:linear-gradient(180deg,#F7DAEC,#F2CFE4);}
.mock.dark .flow .base{background:linear-gradient(180deg,#241735,#1E1428);}
.flow .blob{position:absolute; border-radius:50%;}
.flow .b1{width:150px; height:150px; left:-34px; top:-52px; background:radial-gradient(circle at 50% 50%, #FF6FA6 0%, rgba(255,111,166,0) 68%); animation:drift1 24s ease-in-out infinite;}
.flow .b2{width:130px; height:130px; right:-30px; top:-34px; background:radial-gradient(circle at 50% 50%, #B66BF7 0%, rgba(182,107,247,0) 68%); animation:drift2 28s ease-in-out infinite;}
.flow .b3{width:165px; height:165px; left:16%; bottom:-78px; background:radial-gradient(circle at 50% 50%, #FF8FC7 0%, rgba(255,143,199,0) 70%); animation:drift3 22s ease-in-out infinite;}
.mock.dark .flow .b1{background:radial-gradient(circle at 50% 50%, #B66BF7 0%, rgba(182,107,247,0) 68%);}
.mock.dark .flow .b2{background:radial-gradient(circle at 50% 50%, #C636E7 0%, rgba(198,54,231,0) 68%);}
.mock.dark .flow .b3{background:radial-gradient(circle at 50% 50%, #BE80F8 0%, rgba(190,128,248,0) 70%);}
@keyframes drift1{0%,100%{transform:translate(0,0) scale(1);}50%{transform:translate(20px,12px) scale(1.08);}}
@keyframes drift2{0%,100%{transform:translate(0,0) scale(1);}50%{transform:translate(-16px,14px) scale(1.06);}}
@keyframes drift3{0%,100%{transform:translate(0,0) scale(1);}50%{transform:translate(14px,-12px) scale(1.1);}}
.flow .frost{position:absolute; inset:0;}
.mock.light .flow .frost{background:rgba(255,255,255,.22);}
.mock.dark .flow .frost{background:rgba(20,10,30,.16);}
.flow .veil{position:absolute; left:0; right:0; bottom:0; height:62%;}
.mock.light .flow .veil{background:linear-gradient(to top,rgba(255,255,255,.94) 46%,rgba(255,255,255,.55) 72%,transparent);}
.mock.dark .flow .veil{background:linear-gradient(to top,rgba(18,9,28,.92) 46%,rgba(18,9,28,.55) 72%,transparent);}
.flow .shine{position:absolute; left:-42%; top:12%; width:30%; height:150%; background:linear-gradient(100deg,transparent,rgba(255,255,255,.26),transparent); transform:rotate(16deg); animation:shinemove 10s linear infinite;}
@keyframes shinemove{0%{left:-42%;}100%{left:132%;}}
.flow .t{position:absolute; left:8px; right:8px; bottom:8px; font-size:11px; font-weight:700; line-height:1.35; text-align:center;}
.mock.light .flow .t{color:#2E1B38;}
.mock.dark .flow .t{color:#FFFFFF;}
.mk-photo{height:96px; overflow:hidden;}
.mk-photo img{width:100%; height:100%; object-fit:cover; display:block;}
.mk-body{padding:6px 8px 8px;}
.mk-title{font-size:11px; font-weight:700; line-height:1.35; height:30px; overflow:hidden;}
.mock.light .mk-title{color:#3D2438;}
.mock.dark .mk-title{color:#F4ECFA;}
.mk-meta{display:flex; align-items:center; gap:4px; margin-top:5px;}
.mk-ava{width:14px; height:14px; border-radius:50%; object-fit:cover;}
.mk-meta .a{font-size:9px; flex:1; overflow:hidden; white-space:nowrap;}
.mock.light .mk-meta .a{color:#8A6B85;}
.mock.dark .mk-meta .a{color:#A99BBE;}
.mk-meta .r{font-size:9px; display:flex; align-items:center; gap:2px;}
.mock.light .mk-meta .r{color:#8A6B85;}
.mock.dark .mk-meta .r{color:#A99BBE;}
.mk-foot{display:flex; justify-content:space-between; padding:0 8px 7px; font-size:8.5px;}
.mock.light .mk-foot{color:#8A6B85;}
.mock.dark .mk-foot{color:#A99BBE;}
/* 我的 */
.mk-mine{padding:0 10px 12px;}
.mk-nav{display:flex; align-items:center; justify-content:space-between; padding:10px 4px 8px; font-size:13px; font-weight:700;}
.mock.light .mk-nav{color:#3D2438;}
.mock.dark .mk-nav{color:#F4ECFA;}
.mk-mcard{margin-top:7px; border-radius:13px; overflow:hidden;}
.mock.light .mk-mcard{background:rgba(255,255,255,.82);}
.mock.dark .mk-mcard{background:rgba(34,24,48,.88);}
.mk-mcard.hair{padding:1.5px; background:linear-gradient(135deg,#D6339C,#FF6FA6,#B66BF7,#FF8FC7); box-shadow:0 0 5px rgba(214,51,156,.10);}
.mock.dark .mk-mcard.hair{background:linear-gradient(135deg,#FF6FB5,#B66BF7,#C636E7,#BE80F8); box-shadow:0 0 5px rgba(255,111,181,.10);}
.mk-mcard.hair>.in{border-radius:11.5px; height:100%;}
.mock.light .mk-mcard.hair>.in{background:rgba(255,255,255,.82);}
.mock.dark .mk-mcard.hair>.in{background:rgba(34,24,48,.88);}
.mk-user{display:flex; align-items:center; gap:8px; padding:9px 10px;}
.mk-user img{width:34px; height:34px; border-radius:9px; object-fit:cover;}
.mk-user .n{font-size:12px; font-weight:700;}
.mock.light .mk-user .n{color:#3D2438;}
.mock.dark .mk-user .n{color:#F4ECFA;}
.mk-user .u{font-size:9px;}
.mock.light .mk-user .u{color:#8A6B85;}
.mock.dark .mk-user .u{color:#A99BBE;}
.mk-user .t2{font-size:9px; margin-top:2px; display:flex; gap:5px; align-items:center;}
.mk-pill{background:#D6339C; color:#fff; font-size:8px; padding:1px 6px; border-radius:99px; font-weight:600;}
.mock.dark .mk-pill{background:#FF6FB5; color:#221830;}
.mk-user .gear{margin-left:auto; width:26px; height:26px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:12px;}
.mock.light .mk-user .gear{background:rgba(214,51,156,.10); color:#D6339C;}
.mock.dark .mk-user .gear{background:rgba(255,111,181,.14); color:#FF6FB5;}
.mk-row{display:flex; justify-content:space-around; padding:9px 10px; font-size:10.5px;}
.mock.light .mk-row{color:#3D2438;}
.mock.dark .mk-row{color:#F4ECFA;}
.mk-row b{font-size:15px; font-weight:800; margin-right:3px;}
.mk-line{display:flex; align-items:center; gap:7px; padding:8px 10px; font-size:11px;}
.mock.light .mk-line{color:#3D2438;}
.mock.dark .mk-line{color:#F4ECFA;}
.mk-line .ic{width:16px; height:16px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:9px; font-weight:700;}
.mock.light .mk-line .ic{background:rgba(139,63,217,.12); color:#8B3FD9;}
.mock.dark .mk-line .ic{background:rgba(201,155,240,.16); color:#C99BF0;}
.mk-line .ar{margin-left:auto; opacity:.45; font-size:11px;}
.mk-sep{height:1px; margin:0 10px;}
.mock.light .mk-sep{background:rgba(214,51,156,.10);}
.mock.dark .mk-sep{background:rgba(255,111,181,.12);}
/* 规格表 */
table{border-collapse:collapse; width:100%; font-size:13px; margin-top:10px;}
th,td{border:1px solid var(--line); padding:7px 10px; text-align:left; vertical-align:top;}
th{background:#FAF6FC; font-weight:600; white-space:nowrap;}
.spec{color:var(--pink); font-weight:700;}
footer{margin-top:44px; padding-top:16px; border-top:1px solid var(--line); font-size:12px; color:var(--ink2);}
"""

def phone(inner, dark=False):
    return f'<div class="phone mock {"dark" if dark else "light"}">{inner}</div>'

def feed_mock(dark=False, hair=False):
    cc = ' hair' if hair else ''
    status = '<div class="mk-status"><span>02:50</span><span>5G&nbsp;&nbsp;64</span></div>'
    topbar = ('<div class="mk-topbar"><span class="mk-chip sel">自由搏击区</span>'
              '<span class="mk-chip">归墟</span><span class="mk-chip">游戏论坛</span>'
              '<span class="mk-chip">内野</span><span class="mk-chip">炉石传</span></div>')
    photo = IMG["photo_dark"] if dark else IMG["photo_light"]
    ava = IMG["avatar"]

    def photo_card(title, author, replies, tag, time):
        return f'''<div class="mk-card{cc}"><div class="in">
      <div class="mk-photo"><img src="{photo}"></div>
      <div class="mk-body"><div class="mk-title">{title}</div>
      <div class="mk-meta"><img class="mk-ava" src="{ava}"><span class="a">{author}</span><span class="r">💬 {replies}</span></div></div>
      <div class="mk-foot"><span>{tag}</span><span>{time}</span></div>
    </div></div>'''

    def flow_card(title, author, replies, tag, time):
        return f'''<div class="mk-card{cc}"><div class="in">
      <div class="flow"><div class="base"></div>
        <div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>
        <div class="frost"></div><div class="shine"></div><div class="veil"></div>
        <div class="t">{title}</div></div>
      <div class="mk-body"><div class="mk-meta"><img class="mk-ava" src="{ava}"><span class="a">{author}</span><span class="r">💬 {replies}</span></div></div>
      <div class="mk-foot"><span>{tag}</span><span>{time}</span></div>
    </div></div>'''

    grid = ('<div class="mk-grid">'
            + photo_card('北京IJIOY漫展微微NSFW图', '永远健康希…', '9', '角色扮演', '1天前')
            + photo_card('给加土豆，她曾解释过不同菜品炸后缩水率不同', '法鲁西奥', '25', '其他', '1天前')
            + flow_card('【集中讨论】【黑泥2】二游圈吃瓜/搬运/黑泥讨论楼，欢迎来综合讨论', 'abfk', '16789', '页游', '2024-09-22')
            + flow_card('说中国人种最优秀成了法西斯霍霍名言', '张哦哦', '17', '其他', '2天前')
            + '</div>')
    return status + topbar + grid

def mine_card(inner, dark=False, hair=False):
    if hair:
        return f'<div class="mk-mcard hair"><div class="in">{inner}</div></div>'
    return f'<div class="mk-mcard">{inner}</div>'

def mine_mock(dark=False, hair=False):
    ava = IMG["avatar"]
    nav = '<div class="mk-nav"><span>←</span><span>我的</span><span>⚙</span></div>'
    hero = mine_card(f'''<div class="mk-user">
      <img src="{ava}"><div><div class="n">口袋妖怪 <span style="font-weight:400;font-size:9px;">229182</span></div>
      <div class="t2"><span class="mk-pill">宝可梦go!</span><span style="opacity:.75;">救救我</span></div>
      <div class="t2" style="opacity:.6;">欢迎加入 stage1 <b style="font-size:10.5px;">5332</b> 天</div></div>
      <div class="gear">≡</div></div>''', dark, hair)
    tripod = mine_card(f'''<div class="mk-row">
      <span><b>0</b>提醒</span><span style="opacity:.3;">|</span><span><b>5</b>好友</span></div>''', dark, hair)
    theme = mine_card(f'''
      <div class="mk-line"><span class="ic">◔</span>浏览历史<span style="opacity:.55;font-size:9px;margin-left:4px;">最近看过的 26 个帖子</span><span class="ar">›</span></div>
      <div class="mk-sep"></div>
      <div class="mk-line"><span class="ic">✓</span>签到<span style="opacity:.55;font-size:9px;margin-left:4px;">已连续签到 1 天</span><span style="margin-left:auto;opacity:.55;font-size:9px;">已完成</span></div>''', dark, hair)
    fav = mine_card(f'''
      <div class="mk-line"><span class="ic">☆</span>我的收藏<span class="ar">›</span></div>
      <div class="mk-sep"></div>
      <div class="mk-line"><span class="ic">▤</span>帖子<span class="ar">›</span></div>
      <div class="mk-sep"></div>
      <div class="mk-line"><span class="ic">💬</span>回复<span class="ar">›</span></div>''', dark, hair)
    social = mine_card(f'''
      <div class="mk-line"><span class="ic">ⓘ</span>小黑屋<span class="ar">›</span></div>
      <div class="mk-sep"></div>
      <div class="mk-line"><span class="ic">🚫</span>黑名单<span class="ar">›</span></div>
      <div class="mk-sep"></div>
      <div class="mk-line"><span class="ic">🙈</span>抹布贴<span class="ar">›</span></div>''', dark, hair)
    return '<div class="mk-mine">' + nav + hero + tripod + theme + fav + social + '</div>'

def legend(items, pink=False):
    lis = "".join(f'<li class="{"pink" if pink else ""}"><i>{"B" if pink else "A"}</i>{t}</li>' for t in items)
    return f'<ul class="legend">{lis}</ul>'

LEG_A_FEED = [
    "白发丝 1px：与其他主题完全一致（白/黑竖向渐变、内侧顶高光），幻彩身份只由封面和页面底承担",
    "粗细 1.1dp（浅）/ 1.5dp（深）—— 各主题现值，幻彩不特殊",
]
LEG_B_FEED = [
    "彩色亮丝 <b>1.5dp</b>：s1RingColors 四色渐变（主色→粉→紫→浅粉），斜 135°，透明度 0.88，内缩半线宽",
    "与封面<b>同一组色</b>（coverColors 取色），搭配不冲突；外圈一层 0.08 微光提「亮」，可整层关掉",
    "全 App 边框粗细统一 1.5dp（瀑布流 + 我的），消除「自家边框互不协调」",
]
LEG_A_MINE = [
    "无边框：与其他主题的「我的」页完全一致（纯玻璃卡，无描边、无彩环、无主次分离）",
    "icon / 布局 / 圆角 16dp 全部不动",
]
LEG_B_MINE = [
    "彩色亮丝 <b>1.5dp</b>：与瀑布流<b>同一套语法、同一粗细</b>（斜 135° 四色渐变 + 0.88 + 微光）",
    "六张卡一致对待，无主次分离；卡面与瀑布流同源（浅 #FFFFFF / 深 #221830）",
    "icon / 布局 / 圆角全部不动，只换「边」",
]

html = f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>幻彩边框方案 · 带效果 vs 不带效果</title>
<style>{CSS}</style>
</head>
<body>
<div class="wrap">

<header>
  <h1><span class="badge">边框篇</span>幻彩边框：原版统一 vs 彩色亮丝</h1>
  <p class="sub">S1Client · 2026-10-15 · 封面固定为建议方案（三色径向光斑慢漂 + 磨砂霜 + 底部纱 + 弱亮带，浏览器内为实时动画），本轮只对比边框</p>
</header>

<section id="clarify">
  <h2>〇 · 先说清「删分支」是什么（不是 git 分支）</h2>
  <div class="card note">
    <p>我之前写的「删除分支」指的是<b>代码里的一个条件判断（if/else）</b>，不是 git 分支，也不是删功能。以「我的」页为例，现在的代码是：</p>
    <p style="margin:8px 0; padding:10px 14px; background:#fff; border:1px solid #E5D5F0; border-radius:9px; font-family:Consolas,monospace; font-size:12.5px;">
    如果 当前主题是幻彩 → 画彩色环（3dp）<br>
    否则 → 普通玻璃卡（无边框）</p>
    <p>这条「如果…否则…」就是一条分支。所谓「删除/改写分支」只是改这一小段判断——<b>不碰其他主题、不动 git、不碰布局和 icon</b>。而且按你最新的意思，这条分支<b>不删了，改写</b>：把「画 3dp 彩环」改成「画 1.5dp 彩色亮丝」，其他主题的那一半原样不动。</p>
  </div>
</section>

<section id="feed-light">
  <h2>一 · 首页瀑布流 · 浅色板</h2>
  <div class="cmp">
    <div class="side">
      <div class="tag a">▍方案 A · 原版统一（不带效果）</div>
      {phone(feed_mock(False, hair=False))}
      {legend(LEG_A_FEED)}
    </div>
    <div class="side">
      <div class="tag b">▍方案 B · 彩色亮丝搭配封面（带效果）</div>
      {phone(feed_mock(False, hair=True))}
      {legend(LEG_B_FEED, pink=True)}
    </div>
  </div>
</section>

<section id="feed-dark">
  <h2>二 · 首页瀑布流 · 深色板</h2>
  <div class="cmp">
    <div class="side">
      <div class="tag a">▍方案 A · 原版统一（不带效果）</div>
      {phone(feed_mock(True, hair=False))}
      {legend(LEG_A_FEED)}
    </div>
    <div class="side">
      <div class="tag b">▍方案 B · 彩色亮丝搭配封面（带效果）</div>
      {phone(feed_mock(True, hair=True))}
      {legend(LEG_B_FEED, pink=True)}
    </div>
  </div>
</section>

<section id="mine-light">
  <h2>三 · 「我的」· 浅色板</h2>
  <div class="cmp">
    <div class="side">
      <div class="tag a">▍方案 A · 原版统一（不带效果）</div>
      {phone(mine_mock(False, hair=False))}
      {legend(LEG_A_MINE)}
    </div>
    <div class="side">
      <div class="tag b">▍方案 B · 彩色亮丝（带效果，与瀑布流同语法）</div>
      {phone(mine_mock(False, hair=True))}
      {legend(LEG_B_MINE, pink=True)}
    </div>
  </div>
</section>

<section id="mine-dark">
  <h2>四 · 「我的」· 深色板</h2>
  <div class="cmp">
    <div class="side">
      <div class="tag a">▍方案 A · 原版统一（不带效果）</div>
      {phone(mine_mock(True, hair=False))}
      {legend(LEG_A_MINE)}
    </div>
    <div class="side">
      <div class="tag b">▍方案 B · 彩色亮丝（带效果，与瀑布流同语法）</div>
      {phone(mine_mock(True, hair=True))}
      {legend(LEG_B_MINE, pink=True)}
    </div>
  </div>
</section>

<section id="rec">
  <h2>五 · 我的推荐：B（彩色亮丝），按克制档做</h2>
  <div class="card">
  <table>
    <tr><th style="width:110px;">项</th><th>推荐规格</th></tr>
    <tr><td><b>粗细</b></td><td class="spec">1.5dp，全 App 统一（瀑布流 + 我的）</td></tr>
    <tr><td><b>颜色</b></td><td class="spec">s1RingColors 四色（主色 #D6339C / #FF6FB5 + coverColors 粉·紫·浅粉），斜 135°</td></tr>
    <tr><td><b>透明度</b></td><td class="spec">0.88（细若丝但有存在感；不会再犯 4dp 的「过强」）</td></tr>
    <tr><td><b>微光</b></td><td class="spec">外圈 0.08 同色柔光（「亮」的来源），可整层关掉</td></tr>
    <tr><td><b>画法</b></td><td>描边内缩半个线宽（不被卡片圆角裁掉）；删掉现在「我的」彩环下那 1dp 灰兜底线</td></tr>
  </table>
  <p style="margin-top:12px;">推荐 B 的理由：① 你要的「和其他普通主题不一样」——白发丝是所有主题共用的，彩丝是幻彩独有；② 与封面<b>同色系取色</b>（同一组 coverColors），天然不冲突；③ 1.5dp 细若丝，解决了上轮「边框太强化、和自家我的卡发丝不协调」——现在全 App 都是 1.5dp 同一语法；④ 六张「我的」卡一致对待，无主次分离。<br>
  <span class="small">想更安静可把渐变换成双色（主色→紫）或去掉微光；这两项都在实现 diff 里标成可推翻。</span></p>
  </div>
</section>

<footer>
  <p>封面为建议定稿（光斑=径向渐变圆+漂移动画、磨砂霜=半透明叠层、纱=底部渐变、亮带=斜向柔边 10s 慢扫）；边框示意用 CSS 近似（渐变描边=padding 双层法），最终像素以 Compose 实现为准。<br>
  取证与参考：一加13T真机截图 + uiverse.io 九参考（fast-badger-70 多层 radial / blue-mole-92 磨砂纱 / modern-lizard-75 亮带与发丝边）。</p>
</footer>

</div>
</body>
</html>'''

out = "preview_iridescent/幻彩边框方案预览.html"
with open(out, "w", encoding="utf-8") as f:
    f.write(html)
print("written:", out, "%.1f KB" % (len(html.encode("utf-8")) / 1024))
