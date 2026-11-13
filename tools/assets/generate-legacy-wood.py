"""Generate the checked-in 32px Legacy wood textures and material preview. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance
import math, random
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'src/main/resources/assets/riftfall/textures/block'
OUT.mkdir(parents=True,exist_ok=True)
N=32

def blend(a,b,t):return tuple(round(x+(y-x)*max(0,min(1,t))) for x,y in zip(a,b))
def clamp(c):return tuple(max(0,min(255,round(x))) for x in c)

def bark():
    rng=random.Random(72183);im=Image.new('RGB',(N,N))
    dark=(25,24,29);base=(61,59,65);light=(105,102,105)
    for y in range(N):
        for x in range(N):
            # Uneven, staggered plates; the ridges and fissures wrap at the tile edges.
            bend=math.sin(y*math.tau/N)*.8+math.sin(y*math.tau*2/N)*.35
            v=(x+bend)%7
            c=dark if v<1.2 else blend(base,light,(v-1.2)/4.8)
            if v>5.6:c=blend(c,dark,.48)
            chip=((y+int(x/7)*9)%14)
            if chip in (0,1) and 1<v<5:c=blend(c,dark,.48)
            variation=rng.choice([-8,-4,0,0,2,5,8]);c=clamp([a+variation for a in c])
            if 1.2<v<2.1 and chip in (3,4,5):c=blend(c,(110,35,47),.5)
            im.putpixel((x,y),c)
    # Short broken checks give the bark an aged, rough surface rather than clean stripes.
    d=ImageDraw.Draw(im)
    for x,y in [(5,7),(13,21),(21,11),(28,25)]:
        d.line([(x,y),(x+1,y+2),(x,y+4)],fill=dark,width=1)
        d.point((x+1,y+2),fill=(94,146,99))
    return im

def stripped():
    rng=random.Random(951);im=Image.new('RGB',(N,N))
    palette=[(67,18,29),(95,26,38),(121,33,43),(146,47,47),(154,78,43),(181,111,52),(118,36,39)]
    for y in range(N):
        for x in range(N):
            wave=x+1.1*math.sin(y*math.tau/N)+.45*math.sin(y*math.tau*2/N+x*.22)
            index=int(wave*1.3+.4*math.sin(x*.9))%len(palette)
            c=palette[index];variation=rng.choice([-5,-2,0,0,2,4])
            im.putpixel((x,y),clamp([v+variation for v in c]))
    # A small elliptical knot, still clearly wood rather than polished gemstone.
    for y in range(10,24):
        for x in range(9,21):
            r=math.hypot((x-15)/1.5,(y-17)/3.2)
            if r<3:
                c=palette[(int(r*2)+2)%len(palette)]
                im.putpixel((x,y),blend(im.getpixel((x,y)),c,.78))
    return im

def end(with_bark,bark_image):
    rng=random.Random(184);im=Image.new('RGB',(N,N))
    rings=[(71,17,28),(111,27,38),(142,41,46),(169,95,48),(131,60,36),(96,23,34)]
    for y in range(N):
        for x in range(N):
            dx=x-15.1;dy=y-15.9;angle=math.atan2(dy,dx)
            radius=(abs(dx)**4+abs(dy*.96)**4)**.25
            r=radius+.44*math.sin(angle*3)+.28*math.cos(angle*5)+.22*math.sin((x+y)/3)
            c=rings[int(r*2.05)%len(rings)]
            if radius<2.4:c=blend(c,(99,34,24),.3)
            delta=rng.choice([-4,-2,0,0,2,4]);c=clamp([v+delta for v in c])
            if with_bark and min(x,y,31-x,31-y)<2:c=bark_image.getpixel((x,y))
            im.putpixel((x,y),c)
    d=ImageDraw.Draw(im)
    d.line([(16,16),(18,20),(19,24),(21,28)],fill=(66,20,30),width=1)
    d.line([(15,14),(12,11),(11,7),(8,4)],fill=(90,27,36),width=1)
    return im

b=bark();s=stripped();top=end(True,b);st=end(False,b)
for name,texture in [('vermillion_legacy_log',b),('vermillion_legacy_log_top',top),('stripped_vermillion_legacy_log',s),('stripped_vermillion_legacy_log_top',st)]:
    frames=[]
    for pulse in [0,.35,.7,1]:
        frame=texture.copy()
        for py in range(N):
            for px in range(N):
                r,g,blue=texture.getpixel((px,py))
                if r>g*1.3 and r>blue*1.2:frame.putpixel((px,py),clamp((r+pulse*14,g+pulse*2,blue+pulse*5)))
                elif g>r*1.15:frame.putpixel((px,py),clamp((r+pulse*5,g+pulse*20,blue+pulse*7)))
        frames.append(frame)
    strip=Image.new('RGB',(N,N*4))
    for index,frame in enumerate(frames):strip.paste(frame,(0,index*N))
    strip.save(OUT/(name+'.png'))
    (OUT/(name+'.png.mcmeta')).write_text('{"animation":{"frametime":8,"interpolate":true,"frames":[0,1,2,3,2,1]}}\n')

# An irregular strip of bark, with a warm inner edge and deeply fissured outer face.
item=Image.new('RGBA',(32,32));mask=Image.new('L',(32,32));md=ImageDraw.Draw(mask)
md.polygon([(10,2),(22,3),(26,7),(24,16),(22,25),(17,30),(7,27),(5,22),(8,13),(7,7)],fill=255)
item.paste(b,(0,0),mask);idraw=ImageDraw.Draw(item)
idraw.line([(22,4),(25,8),(23,16),(21,24),(17,28)],fill=(150,43,50,255),width=2)
idraw.line([(10,3),(8,8),(9,13),(6,22),(8,26)],fill=(116,115,119,255),width=1)
item_dir=ROOT/'src/main/resources/assets/riftfall/textures/item';item_dir.mkdir(parents=True,exist_ok=True)
item.save(item_dir/'vermillion_legacy_bark.png')
amber=Image.new('RGBA',(32,32));ad=ImageDraw.Draw(amber)
ad.polygon([(12,3),(22,6),(27,15),(22,25),(12,29),(5,21),(6,11)],fill=(75,19,30),outline=(45,13,23))
ad.polygon([(12,5),(21,8),(23,17),(17,24),(9,22),(8,12)],fill=(158,44,40))
ad.polygon([(12,5),(15,14),(8,20),(8,12)],fill=(209,105,47))
ad.polygon([(15,14),(23,17),(19,25),(12,27),(9,22)],fill=(107,24,36))
ad.line([(12,6),(10,11),(10,14)],fill=(244,178,91),width=2)
ad.line([(15,15),(18,17),(17,21)],fill=(183,111,45),width=1)
ad.point((22,11),fill=(238,165,79))
amber.save(item_dir/'glinting_sap_amber.png')

# An isometric material preview; shading is for the preview only, not baked into game faces.
preview=Image.new('RGB',(640,292),(26,29,31));draw=ImageDraw.Draw(preview)
def face(texture,origin,a,b,shade=1):
    texture=ImageEnhance.Brightness(texture).enhance(shade).convert('RGBA')
    ox,oy=origin;ax,ay=a;bx,by=b;det=ax*by-bx*ay
    coeff=(N*by/det,-N*bx/det,N*(bx*oy-by*ox)/det,-N*ay/det,N*ax/det,N*(ay*ox-ax*oy)/det)
    layer=texture.transform(preview.size,Image.Transform.AFFINE,coeff,resample=Image.Resampling.NEAREST)
    preview.paste(layer,(0,0),layer)
def cube(cx,cy,side,top):
    face(side,(cx-64,cy+32),(64,32),(0,112),.86)
    face(side,(cx,cy+64),(64,-32),(0,112),.66)
    face(top,(cx,cy), (64,32),(-64,32),1.12)
cube(110,35,b,top);cube(318,35,s,st)
draw.text((53,232),'VERMILLION LOG',fill=(226,215,190));draw.text((245,232),'STRIPPED HEARTWOOD',fill=(226,215,190))
for y,(label,t) in enumerate([('BARK',b),('CUT GRAIN',top),('HEARTWOOD',s)]):
    preview.paste(t.resize((64,64),Image.Resampling.NEAREST),(475,14+y*88))
    draw.text((548,38+y*88),label,fill=(226,215,190))
preview.paste(item.resize((48,48),Image.Resampling.NEAREST),(402,225),item.resize((48,48),Image.Resampling.NEAREST))
draw.text((388,278),'LEGACY BARK',fill=(226,215,190))
preview.paste(amber.resize((44,44),Image.Resampling.NEAREST),(178,226),amber.resize((44,44),Image.Resampling.NEAREST))
draw.text((165,278),'SAP-AMBER',fill=(226,215,190))
(ROOT/'docs/previews').mkdir(parents=True,exist_ok=True)
preview.save(ROOT/'docs/previews/legacy-wood.png')
