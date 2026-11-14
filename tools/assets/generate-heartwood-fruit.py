"""Original Heartwood bud/blossom/fruit pixel art and native NeoForge emissive models."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math,random
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/riftfall'
TEX=ASSETS/'textures/block';MODELS=ASSETS/'models/block'
N=32

def blend(a,b,t):return tuple(round(x+(y-x)*max(0,min(1,t))) for x,y in zip(a,b))
def save(name,im):im.save(TEX/(name+'.png'))
def skin(name,dark,light):
    im=Image.new('RGBA',(N,N));rng=random.Random(1709)
    for y in range(N):
        for x in range(N):
            shine=.4+.25*math.sin(x*.16)+.12*math.sin((x+y)*.35)-y*.004+rng.uniform(-.045,.045)
            c=blend(dark,light,shine)
            if 5<=x<=7 and 5<=y<=16:c=blend(c,light,.55)
            im.putpixel((x,y),(*c,255))
    save(name,im);return im
stem=skin('heartwood_fruit_stem',(39,37,23),(111,102,49))
bud=skin('heartwood_fruit_bud',(36,65,40),(125,161,89))
petal=skin('heartwood_fruit_petal',(180,192,204),(255,255,246))
pollen=skin('heartwood_fruit_pollen',(151,104,35),(255,226,129))
silver=skin('heartwood_fruit_silver',(78,99,119),(239,248,255))
gold=skin('heartwood_fruit_gold',(132,71,17),(255,222,105))
for name,color in [('silver',(182,230,244)),('gold',(255,211,97))]:
    im=Image.new('RGBA',(N,N),(0,0,0,0));d=ImageDraw.Draw(im)
    d.line([(8,18),(12,12),(16,9),(20,12),(24,18),(20,22),(16,24),(12,22),(8,18)],fill=(*color,255),width=1)
    d.line([(16,4),(16,9)],fill=(*color,255),width=1)
    d.line([(16,24),(16,28)],fill=(*color,255),width=1)
    d.ellipse((14,15,18,19),fill=(*color,255))
    save('heartwood_fruit_'+name+'_glow',im)

DIRECTIONS=('north','south','east','west','up','down')
def element(a,b,texture,glow=0):
    j={'from':a,'to':b,'faces':{face:{'texture':'#'+texture,'uv':[0,0,16,16]} for face in DIRECTIONS}}
    if glow:j.update(shade=False,neoforge_data={'block_light':glow,'sky_light':glow,'ambient_occlusion':False})
    return j
for stage in range(4):
    textures={'stem':'riftfall:block/heartwood_fruit_stem','particle':'riftfall:block/heartwood_fruit_stem'}
    stem_plane=element([7.5,13,8],[8.5,16,8],'stem')
    stem_plane['faces']={face:stem_plane['faces'][face] for face in ('north','south')}
    parts=[stem_plane]
    if stage==0:
        textures['skin']='riftfall:block/heartwood_fruit_bud'
        parts += [element([6,7,6],[10,13,10],'skin'),element([7,6,7],[9,7,9],'skin')]
    elif stage==1:
        textures.update(petal='riftfall:block/heartwood_fruit_petal',pollen='riftfall:block/heartwood_fruit_pollen')
        # Double-sided, zero-thickness petals; eight pixels across instead of twelve.
        for a,b in [([6.5,13,4],[9.5,13,7.5]),([6.5,13,8.5],[9.5,13,12]),([4,13,6.5],[7.5,13,9.5]),([8.5,13,6.5],[12,13,9.5])]:
            petal=element(a,b,'petal',2)
            petal['faces']={face:petal['faces'][face] for face in ('up','down')}
            parts.append(petal)
        parts.append(element([7,12.9,7],[9,13.3,9],'pollen',6))
    else:
        name='silver' if stage==2 else 'gold'
        textures.update(skin='riftfall:block/heartwood_fruit_'+name,glow='riftfall:block/heartwood_fruit_'+name+'_glow')
        fruit_parts = [element([4,2,4],[12,10,12],'skin'),element([5,0,5],[11,2,11],'skin'),element([5,10,5],[11,12,11],'skin'),element([3.99,1.99,3.99],[12.01,10.01,12.01],'glow',6),element([4.99,-.01,4.99],[11.01,2.01,11.01],'glow',6),element([4.99,9.99,4.99],[11.01,12.01,11.01],'glow',6)]
        for part in fruit_parts:
            part['from'][1]+=1;part['to'][1]+=1
        parts += fruit_parts
    model={'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped','textures':textures,'elements':parts}
    (MODELS/f'heartwood_fruit_{stage}.json').write_text(json.dumps(model,indent=2)+'\n')
(ASSETS/'blockstates/heartwood_fruit.json').write_text(json.dumps({'variants':{f'stage={stage}':{'model':f'riftfall:block/heartwood_fruit_{stage}'} for stage in range(4)}},indent=2)+'\n')

# Ripe fruit inventory icon with a matching restrained emissive eye/vein layer.
item=Image.new('RGBA',(N,N),(0,0,0,0));glow=Image.new('RGBA',(N,N),(0,0,0,0));d=ImageDraw.Draw(item)
d.ellipse((5,7,27,29),fill=(106,58,17,255));d.ellipse((6,7,26,27),fill=(222,163,43,255))
for y in range(8,28):
    for x in range(6,27):
        if item.getpixel((x,y))[3]:
            r=math.hypot((x-16)/11,(y-17)/11)
            c=blend((129,69,19),(255,228,121),max(0,1-r)*.6+max(0,(17-x)/24))
            item.putpixel((x,y),(*c,255))
d=ImageDraw.Draw(item);d.line([(16,8),(17,4),(20,2)],fill=(86,65,28,255),width=2)
d.polygon([(17,6),(20,4),(25,5),(22,8)],fill=(87,133,64,255))
d.line([(9,11),(8,15),(9,18)],fill=(255,237,158,255),width=1)
g=ImageDraw.Draw(glow);g.line([(11,18),(16,13),(21,18),(16,23),(11,18)],fill=(255,222,128,255),width=1);g.ellipse((15,17,17,19),fill=(255,236,171,255))
item_out=ASSETS/'textures/item';item.save(item_out/'heartwood_fruit.png');glow.save(item_out/'heartwood_fruit_glow.png')
(ASSETS/'models/item/heartwood_fruit.json').write_text(json.dumps({'parent':'minecraft:item/generated','loader':'neoforge:item_layers','textures':{'layer0':'riftfall:item/heartwood_fruit','layer1':'riftfall:item/heartwood_fruit_glow'},'neoforge_data':{'layers':{'1':{'block_light':6,'sky_light':6,'ambient_occlusion':False}}}},indent=2)+'\n')
print('Generated bud, white blossom, silver/gold fruit, emissive detail and ripe item icon')
