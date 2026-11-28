"""Generate deterministic 64px pixel materials for the action-wheel panels (Pillow)."""
from pathlib import Path
import math
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/riftfall/textures/gui/wheels'
PALETTES = [
    ('pages', (62, 44, 25), (125, 96, 51)),
    ('runes', (44, 27, 68), (108, 67, 146)),
    ('slashes', (66, 24, 29), (137, 57, 61)),
    ('circuits', (18, 54, 48), (48, 117, 94)),
    ('waves', (22, 46, 70), (54, 103, 143)),
    ('scales', (65, 48, 17), (133, 106, 45)),
    ('stars', (35, 31, 70), (93, 85, 157)),
    ('crystals', (34, 49, 57), (83, 122, 136)),
]
ROOT.mkdir(parents=True, exist_ok=True)
for index, (name, base, accent) in enumerate(PALETTES):
    rng = random.Random(index + 1947)
    im = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            grain = rng.randint(-4, 4)
            im.putpixel((x, y), tuple(max(0, min(255, c + grain)) for c in base) + (255,))
    d = ImageDraw.Draw(im)
    ink = accent + (255,)
    if name == 'pages':
        for y in range(3, 64, 12):
            for x in range(3, 64, 20):
                d.line((x, y, x, y + 8), fill=ink)
                for row in (0, 3, 6): d.line((x + 3, y + row, x + 13, y + row), fill=ink)
    elif name == 'runes':
        for y in range(0, 80, 16):
            for x in range(0, 80, 16):
                d.line((x + 8, y, x + 15, y + 8, x + 8, y + 15, x, y + 8, x + 8, y), fill=ink)
                d.point((x + 8, y + 8), fill=(171, 133, 208, 255))
    elif name == 'slashes':
        for k in range(-64, 80, 12):
            d.line((k, 0, k + 64, 64), fill=ink, width=2)
            d.line((k + 3, 0, k + 67, 64), fill=(91, 34, 39, 255))
    elif name == 'circuits':
        for y in range(4, 64, 16):
            for x in range(4, 64, 16):
                d.rectangle((x - 2, y - 2, x + 2, y + 2), outline=ink)
                d.line((x + 2, y, x + 8, y, x + 8, y + 8, x + 16, y + 8), fill=ink)
    elif name == 'waves':
        for y in range(-5, 72, 12):
            points = [(x, y + int(3 * math.sin(x * math.pi / 16))) for x in range(64)]
            d.line(points, fill=ink, width=2)
    elif name == 'scales':
        for y in range(-8, 72, 12):
            for x in range(-12, 76, 16):
                dx = 8 if (y // 12) % 2 else 0
                d.arc((x + dx, y, x + dx + 16, y + 16), 0, 180, fill=ink)
    elif name == 'stars':
        for y in range(5, 64, 16):
            for x in range(5, 64, 16):
                px = x + (6 if (y // 16) % 2 else 0)
                d.line((px - 2, y, px + 2, y), fill=ink)
                d.line((px, y - 2, px, y + 2), fill=ink)
        for _ in range(25): d.point((rng.randrange(64), rng.randrange(64)), fill=(125, 115, 179, 255))
    else:
        for y in range(-4, 68, 20):
            for x in range(-4, 68, 20):
                d.polygon((x + 8, y, x + 14, y + 6, x + 8, y + 18, x + 2, y + 8), outline=ink)
                d.line((x + 8, y, x + 8, y + 18), fill=ink)
    im.save(ROOT / f'{name}.png')
