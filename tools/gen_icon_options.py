"""
S1Client 图标方案 —— 基于用户提供的两张素材做优化，出多方案供挑选。

用户反馈（对照上一版）：
  · 「没有记忆点」—— 纯文字型图标在桌面上一眼扫过去认不出来；
  · 「在桌面有点小」—— S1 占比太小，缩到 mdpi 后几乎看不见；
  · 「更喜欢原来的图标」—— 黑底 + 彩虹环那版色彩醒目、有辨识度；
  · 给了「人物」素材 + 豆包参考 —— 想往「人物头像」方向走。

所以本轮做 4 个方案，覆盖两条路线：
  A 黑底彩虹环（原图优化，S1 放大）
  B 豆包式人物（人物圆形裁切，弱化背景）
  C 人物 + 彩虹环角标（S1 徽章改成彩虹环，呼应原图）
  D 深色底人物（保留原图氛围，人物居中）
每个方案都出「桌面模拟图」——把图标按真实像素尺寸摆进模拟桌面，
直接看它在 48px 认不认得出，而不是只看 512 大图。
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

DESK = r'C:\Users\Administrator\WorkBuddy\2026-10-14-04-01-58'
SRC_ICON = r'C:\Users\Administrator\Desktop\原图标.png'
SRC_CHAR = r'C:\Users\Administrator\.workbuddy\clipboard-images\clipboard-2026-10-14T12-47-46-522Z-deacb268.jpg'
FONT_PATH = r'C:\Windows\Fonts\msyhbd.ttc'

S = 1024


def circle_mask(size):
    m = Image.new('L', (size * 4, size * 4), 0)
    ImageDraw.Draw(m).ellipse([0, 0, size * 4, size * 4], fill=255)
    return m.resize((size, size), Image.LANCZOS)


def rainbow_ring(size, width_ratio=0.052):
    """原图那条彩虹环：粉→紫→蓝→青，沿左上到右下"""
    ring = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(ring)
    w = int(size * width_ratio)
    pad = w // 2 + int(size * 0.012)
    box = [pad, pad, size - pad, size - pad]
    # 用多段弧近似原图的连续渐变（每段 1.5°）
    import math
    steps = 240
    for i in range(steps):
        a0 = -90 + i * (360 / steps)
        a1 = -90 + (i + 1) * (360 / steps) + 0.8
        t = i / (steps - 1)
        # 色标：粉 #FF3EA5 → 紫 #A855F7 → 蓝 #3B82F6 → 青 #22D3EE
        if t < 0.34:
            k = t / 0.34
            c = (255, int(0x3E + (0x85 - 0x3E) * k), int(0xA5 + (0xF7 - 0xA5) * k))
        elif t < 0.67:
            k = (t - 0.34) / 0.33
            c = (int(0xA8 + (0x3B - 0xA8) * k), int(0x55 + (0x82 - 0x55) * k), 247)
        else:
            k = (t - 0.67) / 0.33
            c = (int(0x3B + (0x22 - 0x3B) * k), int(0x82 + (0xD3 - 0x82) * k), int(0xF6 + (0xEE - 0xF6) * k))
        d.arc(box, a0, a1, fill=c + (255,), width=w)
    return ring


def s1_text(draw, center, font_size, fill=(255, 255, 255, 255)):
    font = ImageFont.truetype(FONT_PATH, font_size)
    txt = 'S1'
    b = draw.textbbox((0, 0), txt, font=font)
    draw.text((center[0] - (b[2] - b[0]) / 2 - b[0], center[1] - (b[3] - b[1]) / 2 - b[1]),
              txt, font=font, fill=fill)
    return b


def black_bg(size, top=(0x2A, 0x2A, 0x2E), bottom=(0x0A, 0x0A, 0x0C)):
    img = Image.new('RGB', (size, size), bottom)
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / (size - 1)
        d.line([(0, y), (size, y)],
               fill=tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img


# ————— 方案 A：黑底 + 彩虹环 + 大 S1（原图优化）—————
def plan_a(size=S):
    base = black_bg(size, (0x33, 0x33, 0x38), (0x08, 0x08, 0x0A))
    base = base.convert('RGBA')
    base.alpha_composite(rainbow_ring(size))
    d = ImageDraw.Draw(base)
    # S1 放大：字号从 ~300 提到 ~430，并把纯白换成略带蓝的冷白，避免死白
    s1_text(d, (size / 2, size / 2 + size * 0.01), int(size * 0.42), (0xF2, 0xF6, 0xFF, 255))
    return base


# ————— 人物素材预处理：抠出人物主体 ——————
def load_char():
    im = Image.open(SRC_CHAR).convert('RGB')
    return im.resize((S, S), Image.LANCZOS)


def cutout_face(size, zoom=1.0):
    """从人物图里取头部做圆形头像（豆包风格）。
    zoom>1 时向内裁切，用来切掉原素材那圈白色贴纸边。"""
    src = load_char()
    box = (int(S * 0.14), int(S * 0.00), int(S * 0.98), int(S * 0.84))
    face = src.crop(box)
    if zoom > 1.0:
        w, h = face.size
        cx, cy = w * 0.5, h * 0.46
        nw, nh = w / zoom, h / zoom
        face = face.crop((int(cx - nw / 2), int(cy - nh / 2), int(cx + nw / 2), int(cy + nh / 2)))
    return face.resize((size, size), Image.LANCZOS)


# ————— 方案 B：豆包式（浅底 + 圆形人物脸）—————
def plan_b(size=S):
    base = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    face = cutout_face(size)
    m = circle_mask(size)
    # 底：白→极浅蓝的柔和渐变（豆包是纯白，这里给一点点层次）
    bg = Image.new('RGB', (size, size), (255, 255, 255))
    d = ImageDraw.Draw(bg)
    for y in range(size):
        t = y / (size - 1)
        d.line([(0, y), (size, y)], fill=(255, int(255 - 8 * t), int(255 - 14 * t)))
    base.paste(bg, (0, 0), m)
    base.paste(face, (0, 0), m)
    return base


def plan_b_square(size=S):
    """方案 B 的方形满幅版：给 ic_launcher 用（系统遮罩会圆角）。
    方形版里 S1 角标要更大一点，否则被圆角裁掉后角标会缺一角。"""
    base = Image.new('RGBA', (size, size), (255, 255, 255, 255))
    base.paste(cutout_face(size), (0, 0))
    bd = size * 0.30
    bx, by = int(size * 0.94 - bd), int(size * 0.06)
    badge = Image.new('RGBA', (int(bd), int(bd)), (0, 0, 0, 0))
    bm = circle_mask(int(bd))
    badge.paste(Image.new('RGBA', (int(bd), int(bd)), (16, 16, 20, 255)), (0, 0), bm)
    badge.alpha_composite(rainbow_ring(int(bd), 0.10))
    bdd = ImageDraw.Draw(badge)
    s1_text(bdd, (bd / 2, bd / 2 + bd * 0.02), int(bd * 0.52))
    base.alpha_composite(badge, (bx, by))
    return base


# ————— 方案 C：人物 + 彩虹环 + S1 角标 ——————
def plan_c(size=S):
    base = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    src = load_char()
    m = circle_mask(size)
    bg = black_bg(size, (0x2E, 0x24, 0x2E), (0x10, 0x0C, 0x10)).convert('RGBA')
    # 人物整体铺满圆形
    base.paste(bg, (0, 0), m)
    base.paste(cutout_face(size), (0, 0), m)
    # 彩虹环
    base.alpha_composite(rainbow_ring(size, 0.045))
    # 右上角 S1 徽章（只留一个，去掉左下角那个以免重复）
    bd = size * 0.30
    bx, by = int(size * 0.915 - bd), int(size * 0.075)
    badge = Image.new('RGBA', (int(bd), int(bd)), (0, 0, 0, 0))
    bm = circle_mask(int(bd))
    bimg = Image.new('RGBA', (int(bd), int(bd)), (16, 16, 20, 255))
    badge.paste(bimg, (0, 0), bm)
    badge.alpha_composite(rainbow_ring(int(bd), 0.10))
    bdd = ImageDraw.Draw(badge)
    s1_text(bdd, (bd / 2, bd / 2 + bd * 0.02), int(bd * 0.52))
    base.alpha_composite(badge, (bx, by))
    return base


# ————— 方案 D：深色底 + 人物（保留原图氛围，弱化彩虹）—————
def plan_d(size=S):
    base = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    m = circle_mask(size)
    bg = black_bg(size, (0x2A, 0x22, 0x2C), (0x0C, 0x0A, 0x0E)).convert('RGBA')
    base.paste(bg, (0, 0), m)
    base.paste(cutout_face(size, zoom=1.22), (0, 0), m)
    # 一圈细白描边把人物和底分开
    d = ImageDraw.Draw(base)
    d.ellipse([size * 0.012, size * 0.012, size * 0.988, size * 0.988],
              outline=(255, 255, 255, 46), width=max(2, int(size * 0.008)))
    return base


def plan_a2(size=S):
    """方案 A 的圆形版：与人物方案形状统一，Android 桌面观感更整齐"""
    base = plan_a(size)
    m = circle_mask(size)
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), m)
    return out


PLANS = [('A  黑底彩虹环 + 大S1', plan_a), ('A2 彩虹环(圆形)', plan_a2),
         ('B  豆包式人物', plan_b),
         ('C  人物 + 彩虹环角标', plan_c), ('D  深色底人物', plan_d)]


def desktop_mock(images, path):
    """把各方案按真实密度像素尺寸摆到模拟桌面上做对比"""
    W, H = 1320, 980
    desk = Image.new('RGB', (W, H), (12, 13, 18))
    d = ImageDraw.Draw(desk)
    # 壁纸渐变，模拟深色桌面
    for y in range(H):
        t = y / (H - 1)
        d.line([(0, y), (W, y)], fill=(int(10 + 18 * t), int(11 + 16 * t), int(20 + 30 * t)))
    label_f = ImageFont.truetype(FONT_PATH, 26)
    x = 70
    for name, img in images:
        # 192 大图
        big = img.resize((192, 192), Image.LANCZOS)
        desk.paste(big, (x, 90), big if big.mode == 'RGBA' else None)
        d.text((x, 300), name, font=label_f, fill=(235, 235, 240))
        # 真实小尺寸 96 / 72 / 48
        yy = 350
        for s in (96, 72, 48):
            sm = img.resize((s, s), Image.LANCZOS)
            desk.paste(sm, (x + (192 - s) // 2, yy), sm if sm.mode == 'RGBA' else None)
            yy += s + 26
        x += 262
    desk.save(path)
    print('  ->', os.path.basename(path))


if __name__ == '__main__':
    outs = []
    for name, fn in PLANS:
        img = fn()
        safe = name.split()[0]
        p = os.path.join(DESK, f's1_icon_plan_{safe}.png')
        img.resize((512, 512), Image.LANCZOS).save(p)
        outs.append((name, img))
        print(f'{name} -> {os.path.basename(p)}')
    desktop_mock(outs, os.path.join(DESK, 's1_icon_plans_desktop.png'))
