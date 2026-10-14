"""
把指定方案写入 Android mipmap 资源（方形 → ic_launcher，圆形 → ic_launcher_round）。

用户桌面上的图标有圆形也有圆角方形混排（见截图：应用隐藏是圆、小鸡游戏世界是圆、
Balatro 是方形），所以两套都要给齐，且**方形版必须满幅出血** ——
如果方形版里画的是圆形，套上系统遮罩后会四周留白，在网格里明显比别人小一圈。
"""
import sys
import os
from PIL import Image

sys.path.insert(0, r'C:\Users\Administrator\WorkBuddy\2026-10-14-04-01-58')
import gen_icon_options as G

RES = r'D:\S1Client\app\src\main\res'
DENSITIES = [('mipmap-mdpi', 48), ('mipmap-hdpi', 72), ('mipmap-xhdpi', 96),
             ('mipmap-xxhdpi', 144), ('mipmap-xxxhdpi', 192)]

which = sys.argv[1] if len(sys.argv) > 1 else 'A2'
square_fn = G.plan_a if which in ('A', 'A2') else G.plan_b_square
round_fn = G.plan_a2 if which == 'A2' else G.plan_b


def circle_mask(size):
    m = Image.new('L', (size * 4, size * 4), 0)
    from PIL import ImageDraw
    ImageDraw.Draw(m).ellipse([0, 0, size * 4, size * 4], fill=255)
    return m.resize((size, size), Image.LANCZOS)


square = square_fn(1024).convert('RGBA')
rounded = round_fn(1024).convert('RGBA')

for folder, px in DENSITIES:
    d = os.path.join(RES, folder)
    os.makedirs(d, exist_ok=True)

    # 方形：满幅出血，不做圆角（系统遮罩会处理）
    square.resize((px, px), Image.LANCZOS).save(os.path.join(d, 'ic_launcher.png'), 'PNG')

    # 圆形：套圆形遮罩
    im = rounded.resize((px, px), Image.LANCZOS)
    out = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    out.paste(im, (0, 0), circle_mask(px))
    out.save(os.path.join(d, 'ic_launcher_round.png'), 'PNG')

    print(f'  {folder:<18} {px}x{px}')

print(f'{which} written')
