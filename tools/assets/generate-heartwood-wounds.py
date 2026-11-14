"""Generate original pixel-art sap, red-amber resin and closing-bark sprites (Pillow)."""
from pathlib import Path
from PIL import Image,ImageDraw
import ast,math,random,json
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'src/main/resources/assets/riftfall/textures/block'
# Reuse the original procedural material functions without running that asset generator.
source=ast.parse((ROOT/'tools/assets/generate-legacy-wood.py').read_text())
functions=[n for n in source.body if isinstance(n,ast.FunctionDef) and n.name in ('blend','clamp','bark','stripped')]
ns=dict(Image=Image,ImageDraw=ImageDraw,math=math,random=random,N=32)
exec(compile(ast.Module(body=functions,type_ignores=[]),'heartwood_material_functions','exec'),ns)
wood=ns['stripped']().convert('RGBA');bark=ns['bark']().convert('RGBA');blend=ns['blend']
N=32
# Viscous honey-like runs thicken while leaving some timber visible between them.
def wet(thick,pulse):
    base=wood.copy();glow=Image.new('RGBA',(N,N),(0,0,0,0))
    for y in range(N):
        for x in range(N):
            for stream,cx in enumerate((4,15,26)):
                bend=.8*math.sin(y*math.tau/N+stream*1.7)
                dx=((x-cx-bend+N/2)%N)-N/2
                width=(3.4 if thick else 2.0)+.5*(1+math.sin(y*math.tau/N*2+stream*2))
                cross=abs(dx)/width
                if cross>=1:continue
                c=blend((108,36,14),(210,120,32),1-cross*.75)
                if cross>.86:c=blend(c,(81,24,13),.5)
                if -.65<dx/width<-.32:c=blend(c,(234,175,69),.4)
                if not thick:c=blend(wood.getpixel((x,y))[:3],c,.76)
                base.putpixel((x,y),(*c,255))
                c=tuple(round(v*(.96+.04*pulse)) for v in c)
                glow.putpixel((x,y),(*c,255))
                break
    return base,glow

def amber():
    im=Image.new('RGBA',(N,N))
    for y in range(N):
        for x in range(N):
            vein=math.sin(x*.37+y*.12)+.35*math.sin(y*.45-x*.22)
            c=blend((61,9,22),(179,47,31),(vein+1.35)/2.7)
            c=blend(c,(226,118,42),max(0,math.sin(x*.52+y*.19)-.65))
            # Fine trapped sap inclusions and hard, glassy red-amber highlights.
            if (x*17+y*31)%137==0:c=(238,154,68)
            if 3<=x<=5 and 4<=y<=14:c=blend(c,(255,207,142),.7 if x==4 else .3)
            if 21<=x<=23 and 20<=y<=26:c=blend(c,(247,151,79),.5)
            im.putpixel((x,y),(*c,255))
    return im

for stage in (1,2):
    bases=[];glows=[]
    for pulse in (0,.5,1,.5):
        a,b=wet(stage==2,pulse);bases.append(a);glows.append(b)
    for suffix,frames in (('',bases),('_glow',glows)):
        name=f'heartwood_wound_sap_{stage}{suffix}'
        strip=Image.new('RGBA',(N,N*4))
        for i,frame in enumerate(frames):strip.paste(frame,(0,i*N))
        strip.save(OUT/(name+'.png'))
        (OUT/(name+'.png.mcmeta')).write_text(json.dumps({'animation':{'frametime':14,'interpolate':True}})+'\n')
resin=amber();resin.save(OUT/'heartwood_wound_resin.png')
for stage,coverage in enumerate((.28,.58,.84),4):
    im=resin.copy()
    for y in range(N):
        for x in range(N):
            edge=min(x,N-1-x)/16
            irregular=.12*math.sin(y*.47+x*.18)+.07*math.sin(x*1.1-y*.28)
            if edge+irregular<coverage:im.putpixel((x,y),bark.getpixel((x,y)))
    im.save(OUT/f'heartwood_wound_closing_{stage-3}.png')
print('Generated emissive wet sap, red amber and three returning-bark stages')

# A filled glass vial for collection; ingredient item, with an empty-bottle crafting remainder.
ITEM_OUT=ROOT/'src/main/resources/assets/riftfall/textures/item'
ITEM_OUT.mkdir(parents=True,exist_ok=True)
vial=Image.new('RGBA',(N,N),(0,0,0,0));d=ImageDraw.Draw(vial)
shape=[(12,8),(19,8),(20,11),(24,14),(26,18),(26,26),(23,29),(9,29),(6,26),(6,18),(8,14),(11,11)]
d.polygon(shape,fill=(128,163,169,175),outline=(47,68,78,255))
d.rectangle((12,4,19,10),fill=(174,198,194,230),outline=(61,88,97,255))
d.rectangle((11,2,20,5),fill=(99,48,25,255),outline=(52,28,24,255));d.line((12,2,18,2),fill=(179,106,51,255),width=1)
for y in range(16,27):
    for x in range(8,25):
        if vial.getpixel((x,y))[3]:
            color=blend((116,36,14),(221,136,37),(26-y)/14+.12*math.sin(x*.7+y*.2))
            vial.putpixel((x,y),(*color,255))
d.ellipse((8,14,24,18),fill=(208,122,28,255),outline=(244,177,68,255))
d.line([(9,18),(9,24),(11,26)],fill=(250,217,149,255),width=1)
d.line([(8,14),(7,19),(7,24)],fill=(219,236,230,235),width=1)
d.line([(23,20),(23,25)],fill=(152,190,185,220),width=1)
vial.save(ITEM_OUT/'heartwood_sap_bottle.png')
