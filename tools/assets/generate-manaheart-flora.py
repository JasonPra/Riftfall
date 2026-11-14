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
fern=Image.new('RGBA',(N,64),(0,0,0,0));d=ImageDraw.Draw(fern)
d.line([(16,63),(15,35),(16,3)],fill=(152,152,152,255),width=1)
for row,y in enumerate(range(9,59,4)):
 length=int(3+11*math.sin((y-3)/64*math.pi));shade=128+row*6
 for sign in (-1,1):
  d.line([(16,y),(16+sign*length,y-6)],fill=(shade,)*3+(255,),width=1)
  for k in range(2,length,2):
   x=16+sign*k;yy=y-round(k*6/length)
   d.line([(x,yy),(x+sign*2,yy-3)],fill=(min(225,shade+20),)*3+(255,),width=1)
fern.crop((0,0,32,32)).save(T/'whisperfrond_fern_upper.png')
fern.crop((0,32,32,64)).save(T/'whisperfrond_fern_lower.png')
fern.resize((32,32),Image.Resampling.NEAREST).save(T/'whisperfrond_fern.png')
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

# Real double-plant models, with one sprite per block and native half-state handling.
for half in ['lower','upper']:
 model=json.loads((M/'whisperfrond_fern.json').read_text());model['textures']['plant']='riftfall:block/whisperfrond_fern_'+half;model['textures']['particle']=model['textures']['plant']
 out(Path('models/block')/('whisperfrond_fern_'+half+'.json'),model)
out(Path('blockstates/whisperfrond_fern.json'),{'variants':{f'half={half}':{'model':'riftfall:block/whisperfrond_fern_'+half} for half in ['lower','upper']}})
# A low woody shrub, distinct from ferns and flowers.
shr=Image.new('RGBA',(32,32),(0,0,0,0));d=ImageDraw.Draw(shr);rng=random.Random(7829)
for i in range(150):
 x=rng.randrange(32);y=rng.randrange(32);shade=rng.randrange(80,172);d.ellipse((x,y,x+3,y+2),fill=(shade,shade,shade,255))
shr.save(T/'tangleveil_shrub.png')
parts=[cube([7,0,7],[9,8,9],'wood','wood'),cube([1,6,1],[15,13,15],'leaf','leaf'),cube([3,13,3],[13,15,13],'leaf','leaf')]
for part in parts[1:]:
 for face in part['faces'].values():face['tintindex']=0
out(Path('models/block/tangleveil_shrub.json'),{'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped','textures':{'leaf':'riftfall:block/tangleveil_shrub','wood':'riftfall:block/vermillion_legacy_log','particle':'riftfall:block/tangleveil_shrub'},'elements':parts})
out(Path('blockstates/tangleveil_shrub.json'),{'variants':{'':{'model':'riftfall:block/tangleveil_shrub'}}})
out(Path('models/item/tangleveil_shrub.json'),{'parent':'riftfall:block/tangleveil_shrub'})
# Native WolfModel uses a 64x32 atlas. Keep detail on every mapped face,
# rather than covering the head/body/legs with solid rectangles.
fur=Image.new('RGBA',(64,32));d=ImageDraw.Draw(fur);rng=random.Random(3862)
for y in range(32):
 for x in range(64):
  v=rng.randrange(-15,16)
  fur.putpixel((x,y),(max(0,151+v),max(0,174+v),max(0,191+v),255))
def patch(box,base):
 for y in range(box[1],box[3]+1):
  for x in range(box[0],box[2]+1):
   v=rng.randrange(-13,14)
   fur.putpixel((x,y),tuple(max(0,min(255,c+v)) for c in base)+(255,))
# Head: crown, sides, face and chin; six-pixel front at (4,4).
patch((4,0,9,3),(112,140,161))
patch((0,4,3,9),(163,185,201));patch((10,4,13,9),(163,185,201))
patch((4,4,9,9),(202,216,220));patch((10,0,15,3),(218,226,224))
# Brow and cheek tufts, mirrored across both sides of the face.
for x in (4,9):
 d.point((x,5),fill=(66,88,108,255))
 d.point((x,6),fill=(47,215,203,255))
for x in (3,10):
 d.point((x,5),fill=(66,88,108,255))
 d.point((x,6),fill=(47,215,203,255))
 d.point((x,7),fill=(211,226,226,255))
# Muzzle front is (4,14)-(6,16), not the head's forehead.
patch((0,10,13,16),(216,226,224))
d.rectangle((4,14,6,14),fill=(31,44,57,255))
d.point((5,15),fill=(44,57,68,255))
d.line((4,16,6,16),fill=(87,106,115,255))
# Shared ear UV: dark outer fur and warm shaded inner ear.
patch((16,15,21,16),(87,113,135));d.rectangle((17,15,18,16),fill=(159,161,170,255))
# Body and ruff: pale belly/chest contrasted with blue-grey back.
patch((24,14,29,19),(102,132,155));patch((30,14,35,19),(205,219,219))
patch((18,20,23,28),(145,171,190));patch((24,20,29,28),(178,199,209))
patch((30,20,35,28),(145,171,190));patch((36,20,41,28),(192,210,214))
patch((28,0,35,6),(106,136,159));patch((36,0,43,6),(213,224,222))
patch((21,7,27,12),(156,181,199));patch((28,7,35,12),(197,216,218))
patch((36,7,42,12),(156,181,199));patch((43,7,50,12),(193,212,217))
# Fur strands, paws and a dark tail tip remain visible on all sides.
for x,y in [(22,9),(24,10),(39,9),(41,10),(19,23),(21,25),(31,23),(33,25),(26,22),(28,25),(45,9),(48,10)]:
 d.line((x,y,x,y+1),fill=(114,145,168,255))
patch((0,26,7,29),(75,100,121));patch((9,26,16,29),(66,92,114))
for x,y in [(23,8),(24,9),(25,10),(38,8),(39,9),(40,10),(20,21),(21,22),(22,23),(32,21),(33,22),(34,23)]:
 d.point((x,y),fill=(76,199,191,255))
entity=A/'textures/entity';entity.mkdir(parents=True,exist_ok=True);fur.save(entity/'ry.png')
