#!/usr/bin/env python3
"""Deterministic original Legacy foliage and seed pixel sprites (Pillow)."""
from pathlib import Path
from PIL import Image,ImageDraw
import random
root=Path(__file__).resolve().parents[2]/'src/main/resources/assets/riftfall/textures'
rng=random.Random(38719)
leaf=Image.new('RGBA',(32,32));d=ImageDraw.Draw(leaf)
# Interlocking glossy, healthy leaves with dark gaps and sunlit edges.
for row in range(-1,6):
 for col in range(-1,6):
  x=col*7+(row%2)*3+rng.randrange(-1,2);y=row*7+rng.randrange(-1,2)
  green=rng.randrange(105,150)
  d.polygon([(x,y+3),(x+2,y),(x+6,y+1),(x+7,y+4),(x+4,y+6),(x+1,y+5)],fill=(22,green//2+28,36,255))
  d.polygon([(x+1,y+3),(x+3,y+1),(x+5,y+2),(x+5,y+4),(x+3,y+5)],fill=(39,green,57,255))
  d.line([(x+1,y+4),(x+5,y+2)],fill=(105,min(205,green+42),75,255),width=1)
# Neutral luminance: the biome supplies the color, as with vanilla leaf textures.
for y in range(32):
 for x in range(32):
  r,g,b,a=leaf.getpixel((x,y));value=min(255,round(max(r,g,b)*1.45))
  leaf.putpixel((x,y),(value,value,value,a))
(root/'block').mkdir(parents=True,exist_ok=True);leaf.save(root/'block/vermillion_legacy_leaves.png')
seed=Image.new('RGBA',(32,32));d=ImageDraw.Draw(seed)
d.polygon([(9,15),(13,11),(20,12),(24,17),(22,25),(17,28),(11,25),(8,20)],fill='#3b2026')
d.polygon([(12,15),(17,13),(21,16),(21,23),(17,26),(12,23),(10,19)],fill='#98442d')
d.line([(14,16),(13,21),(17,24)],fill='#d49b47',width=2)
d.polygon([(16,12),(15,7),(17,4),(18,11)],fill='#406b32')
d.polygon([(17,9),(20,3),(25,3),(24,7),(20,10)],fill='#49a44d')
d.line([(18,9),(23,5)],fill='#92ca69',width=1)
(root/'item').mkdir(exist_ok=True);seed.save(root/'item/vermillion_legacy_seed.png')
