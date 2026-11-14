"""Original pixel art for Starweave ground cover and Manaheart flowers; no vanilla texture copies."""
from pathlib import Path
from PIL import Image,ImageDraw
import math,random,json
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/riftfall';T=A/'textures/block';M=A/'models/block';N=32

def out(path,data):
 p=A/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def mix(a,b,t):return tuple(round(x+(y-x)*max(0,min(t,1))) for x,y in zip(a,b))
def noise(x,y,scale,seed):
 x/=scale;y/=scale;ix=math.floor(x);iy=math.floor(y);u=x-ix;v=y-iy;u=u*u*(3-2*u);v=v*v*(3-2*v)
 def at(a,b):return random.Random(seed+a*731+b*997).random()
 return (at(ix,iy)*(1-u)+at(ix+1,iy)*u)*(1-v)+(at(ix,iy+1)*(1-u)+at(ix+1,iy+1)*u)*v

def ground(name,stone=False,side=False):
 im=Image.new('RGBA',(N,N));glow=Image.new('RGBA',(N,N),(0,0,0,0));rng=random.Random(873+int(side));mask={}
 for y in range(N):
  for x in range(N):
   n=noise(x,y,6,713)+noise(x,y,2,629)*.35
   moss=not stone or n+(1-y/31)*.35>.72
   if moss:
    c=mix((22,47,39),(100,142,76),n*.7+rng.uniform(-.12,.12));c=mix(c,(44,97,73),.16 if side else .04)
   else:
    ridge=abs(math.sin(x*.43+math.sin(y*.37)*1.5));c=mix((39,46,52),(108,114,114),n*.65+ridge*.12)
    if ridge<.09:c=mix(c,(24,28,34),.6)
   im.putpixel((x,y),(*c,255));mask[x,y]=moss
 # Thin discontinuous luminous mycelial threads and rare pinprick stars.
 d=ImageDraw.Draw(glow)
 for i in range(7 if not stone else 4):
  x=rng.randrange(N);y=rng.randrange(N)
  for j in range(rng.randrange(3,8)):
   if mask[x%N,y%N]:d.point((x%N,y%N),fill=(113+rng.randrange(35),206+rng.randrange(25),183+rng.randrange(40),255))
   x+=rng.choice([-1,0,1]);y+=1
 for i in range(4):
  x=rng.randrange(N);y=rng.randrange(N)
  if mask[x,y]:d.point((x,y),fill=(213,240,205,255))
 im.save(T/(name+'.png'));glow.save(T/(name+'_glow.png'))
ground('starweave_moss_top');ground('starweave_moss_side',side=True);ground('starweave_boulderstone',stone=True)

# Neutral fern foliage receives Minecraft's biome grass tint.
fern=Image.new('RGBA',(N,N),(0,0,0,0));d=ImageDraw.Draw(fern)
d.line([(16,31),(15,18),(16,3)],fill=(173,187,168,255),width=1)
for row,y in enumerate(range(6,28,3)):
 length=int(3+9*math.sin((y-3)/28*math.pi));shade=170+row*7
 for sign in (-1,1):
  end=(16+sign*length,y-3);d.line([(16,y),end],fill=(shade,shade,shade,255),width=2)
  for k in range(2,length,2):
   x=16+sign*k;yy=y-round(k*3/length);d.line([(x,yy),(x+sign*2,yy-3)],fill=(min(245,shade+20),)*3+(255,),width=1)
fern.save(T/'whisperfrond_fern.png')
for name,warm in [('dawnstar_blossom',True),('moonveil_bloom',False)]:
 im=Image.new('RGBA',(N,N),(0,0,0,0));glow=Image.new('RGBA',(N,N),(0,0,0,0));d=ImageDraw.Draw(im);g=ImageDraw.Draw(glow)
 d.line([(16,31),(15,20),(16,12)],fill=(66,111,65,255),width=1)
 d.polygon([(15,26),(8,20),(10,25),(15,28)],fill=(75,137,77,255));d.polygon([(16,22),(23,17),(22,22),(16,25)],fill=(108,157,91,255))
 centers=[(16,10)] if warm else [(11,12),(22,8),(17,18)]
 if not warm:
  d.line([(15,20),(11,15),(11,12)],fill=(68,115,82,255),width=1);d.line([(16,17),(21,12),(22,8)],fill=(68,115,82,255),width=1)
 for cx,cy in centers:
  radius=7 if warm else 4
  for i in range(5):
   a=i*math.tau/5-math.pi/2;ex=cx+math.cos(a)*radius;ey=cy+math.sin(a)*radius
   left=(cx+math.cos(a-.48)*radius*.55,cy+math.sin(a-.48)*radius*.55);right=(cx+math.cos(a+.48)*radius*.55,cy+math.sin(a+.48)*radius*.55)
   d.polygon([(cx,cy),left,(ex,ey),right],fill=(239,231-i*4,184+i*7,255) if warm else (191+i*8,201+i*5,244,255))
   g.point((round(ex),round(ey)),fill=(255,237,162,255) if warm else (166,222,255,255))
  d.rectangle((cx-1,cy-1,cx+1,cy+1),fill=(217,158,54,255) if warm else (107,146,208,255))
  g.point((cx,cy),fill=(255,238,158,255) if warm else (181,239,255,255))
 im.save(T/(name+'.png'));glow.save(T/(name+'_glow.png'))

FACES=('north','south','east','west','up','down')
def cube(a,b,top,side,glow=False):
 p={'from':a,'to':b,'faces':{f:{'texture':'#'+(top if f in ('up','down') else side),'uv':[0,0,16,16]} for f in FACES}}
 if glow:p.update(shade=False,neoforge_data={'block_light':6,'sky_light':6,'ambient_occlusion':False})
 return p
for name,carpet,stone in [('starweave_moss',False,False),('starweave_moss_carpet',True,False),('starweave_boulderstone',False,True)]:
 top='starweave_boulderstone' if stone else 'starweave_moss_top';side=top if stone else 'starweave_moss_side';height=1 if carpet else 16
 tex={'top':'riftfall:block/'+top,'side':'riftfall:block/'+side,'gt':'riftfall:block/'+top+'_glow','gs':'riftfall:block/'+side+'_glow','particle':'riftfall:block/'+top}
 parts=[cube([0,0,0],[16,height,16],'top','side'),cube([-.01,-.01,-.01],[16.01,height+.01,16.01],'gt','gs',True)]
 out(Path('models/block')/(name+'.json'),{'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped','textures':tex,'elements':parts})
for name in ['whisperfrond_fern','dawnstar_blossom','moonveil_bloom']:
 tex={'plant':'riftfall:block/'+name,'particle':'riftfall:block/'+name};parts=[]
 for angle in [-45,45]:
  faces={f:{'texture':'#plant','uv':[0,0,16,16]} for f in ['north','south']}
  if name=='whisperfrond_fern':
   for f in faces.values():f['tintindex']=0
  parts.append({'from':[0,0,8],'to':[16,16,8],'rotation':{'origin':[8,8,8],'axis':'y','angle':angle,'rescale':True},'shade':False,'faces':faces})
  if name!='whisperfrond_fern':
   tex['glow']='riftfall:block/'+name+'_glow'
   parts.append({'from':[0,0,8.015],'to':[16,16,8.015],'rotation':{'origin':[8,8,8],'axis':'y','angle':angle,'rescale':True},'shade':False,'faces':{f:{'texture':'#glow','uv':[0,0,16,16]} for f in ['north','south']},'neoforge_data':{'block_light':8,'sky_light':8,'ambient_occlusion':False}})
 out(Path('models/block')/(name+'.json'),{'parent':'minecraft:block/block','render_type':'minecraft:cutout','ambientocclusion':False,'textures':tex,'elements':parts})
for name in ['starweave_moss','starweave_moss_carpet','starweave_boulderstone','whisperfrond_fern','dawnstar_blossom','moonveil_bloom']:
 out(Path('blockstates')/(name+'.json'),{'variants':{'':{'model':'riftfall:block/'+name}}})
 if name in ['whisperfrond_fern','dawnstar_blossom','moonveil_bloom']:
  item={'parent':'minecraft:item/generated','textures':{'layer0':'riftfall:block/'+name}}
  if name!='whisperfrond_fern':
   item.update(loader='neoforge:item_layers',neoforge_data={'layers':{'1':{'block_light':8,'sky_light':8,'ambient_occlusion':False}}});item['textures']['layer1']='riftfall:block/'+name+'_glow'
  out(Path('models/item')/(name+'.json'),item)
 else:out(Path('models/item')/(name+'.json'),{'parent':'riftfall:block/'+name})
print('Generated original Starweave moss/stone, Whisperfrond fern, Dawnstar and Moonveil artwork/models')

# Dynamic Trees' root-bearing soil retains the same magical ground cover.
out(Path('blockstates/rooty_starweave_moss.json'),{'multipart':[{'apply':{'model':'riftfall:block/starweave_moss'}},{'apply':{'model':'dynamictrees:block/roots'}}]})
