"""
S1Client 启动图标生成 —— 严格按 CONSTITUTION 第 15 条（数学秩序）+ 项目语言设计。

设计决策（逐条对应宪法）：
  · **一个容器只设一个主角**：主角是「S1」二字，别无其他装饰。
  · **竖杠**：项目最标志性的视觉语言（帖子卡片版块名、签名块都用它），
    放在 S1 左侧作为识别锚点，不与文字抢戏（细、高、克制）。
  · **数学秩序**：竖杠高度取文字高度的 φ(1.618) 倍，形成主次比；
    竖杠宽 = 画布 1024 的 1/21（斐波那契），文字字号沿黄金分割定档。
  · **可读性优先（宪法第 15 条上位约束）**：深底白字，任何桌面壁纸上都可读；
    不做透明渐变、不做投影、不做拟物。
  · **克制**：无圆角装饰、无高光、无阴影、无第二色。

输出：各密度 mipmap 的 ic_launcher.png / ic_launcher_round.png，
外加 512 预览图供人工核对。
"""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = r'D:\S1Client\app\src\main\res'
FONT = r'C:\Windows\Fonts\msyhbd.ttc'
CANVAS = 1024

# 取自项目「曜石玄夜」主题的 headerGradient（#1A1A1A → #000000）——中性、克制
BG_TOP = (0x1A, 0x1A, 0x1A)
BG_BOTTOM = (0x00, 0x00, 0x00)
FG = (0xFF, 0xFF, 0xFF)

# 斐波那契：竖杠宽 = 1024/21 ≈ 48
BAR_W = CANVAS // 21
FONT_SIZE = CANVAS // 3          # 341
PHI = 1.618


def render() -> Image.Image:
    img = Image.new('RGB', (CANVAS, CANVAS), BG_BOTTOM)
    d = ImageDraw.Draw(img)

    # 竖向渐变背景
    for y in range(CANVAS):
        t = y / (CANVAS - 1)
        c = tuple(round(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * t) for i in range(3))
        d.line([(0, y), (CANVAS, y)], fill=c)

    font = ImageFont.truetype(FONT, FONT_SIZE)
    text = 'S1'

    # 量出文字包围盒
    bbox = d.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]

    # 竖杠高度 = 文字高 × φ，垂直居中对齐文字
    bar_h = round(th * PHI)
    gap = BAR_W                        # 竖杠与文字之间留一个"竖杠宽"的呼吸
    unit_w = BAR_W + gap + tw

    # 整组居中（安全区：Android 会裁掉四周约 1/6，这里再收 8% 保险）
    safe = CANVAS * 0.08
    cx = (CANVAS - unit_w) / 2
    unit_h = max(bar_h, th)
    cy = (CANVAS - unit_h) / 2

    # 竖杠（白色，略收一点高度让它比字重但不喧宾夺主）
    bar_y = cy + (unit_h - bar_h) / 2
    d.rounded_rectangle(
        [cx, bar_y, cx + BAR_W, bar_y + bar_h],
        radius=BAR_W // 2,
        fill=FG
    )

    # 文字：基线对齐到竖杠的视觉中线
    tx = cx + BAR_W + gap
    ty = cy + (unit_h - th) / 2 - bbox[1]
    d.text((tx, ty), text, font=font, fill=FG)

    return img


def save(img: Image.Image, path: str, size: int, round_icon: bool = False) -> None:
    im = img.resize((size, size), Image.LANCZOS)
    if round_icon:
        mask = Image.new('L', (size * 4, size * 4), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, size * 4, size * 4], fill=255)
        mask = mask.resize((size, size), Image.LANCZOS)
        bg = Image.new('RGB', (size, size), BG_BOTTOM)
        bg.paste(im, (0, 0), mask)
        im = bg
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, 'PNG')
    print(f'  {path.split("res" + os.sep)[-1]:<52} {size}x{size}')


if __name__ == '__main__':
    base = render()
    base.resize((512, 512), Image.LANCZOS).save(r'C:\Users\Administrator\WorkBuddy\2026-10-14-04-01-58\s1_icon_preview_512.png')
    for folder, px in [('mipmap-mdpi', 48), ('mipmap-hdpi', 72), ('mipmap-xhdpi', 96),
                       ('mipmap-xxhdpi', 144), ('mipmap-xxxhdpi', 192)]:
        d = os.path.join(OUT, folder)
        save(base, os.path.join(d, 'ic_launcher.png'), px)
        save(base, os.path.join(d, 'ic_launcher_round.png'), px, round_icon=True)
    print('done')
