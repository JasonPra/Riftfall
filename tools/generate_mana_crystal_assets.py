"""Original translucent pixel textures and cuboid crystal geometry. Python standard library only."""
import json,math,random,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
COLORS={'red':0xB94D57,'orange':0xCB8450,'yellow':0xD0BB67,'green':0x619A72,'cyan':0x65A8B4,'blue':0x526FAE,'violet':0x8A64AD,'white':0xD8DFE8}
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def texture(path,color,cut=False,opaque=False,tip=False,cap=False):
 r,g,b=(color>>16)&255,(color>>8)&255,color&255;noise=random.Random(41);raw=bytearray()
 for y in range(32):
  raw.append(0)
  for x in range(32):
   if cut:
    inside=5<=x<=26 and 4<=y<=27 and 10<=x+y<=49 and -20<=x-y<=21
    factor=1.15 if x<10 or y<9 else .65 if x>22 or y>23 else .95
    alpha=255 if inside else 0
   else:
    cloud=.12*math.sin(x*.62+y*.27)+.08*math.sin(y*.81-x*.21)+noise.uniform(-.06,.06)
    factor=.89+cloud+(.09 if x%11 in (2,3) else 0);alpha=max(165,min(225,round(198+cloud*80)))
   if tip:
    fade=max(0,min(1,(.30-y/31)/.30));fade=fade*fade*(3-2*fade)
    alpha=round(alpha*(1-.42*fade))
   if cap:alpha=round(alpha*.58)
   if opaque:alpha=255
   for value in (r,g,b):raw.append(max(0,min(255,round(value*factor+12))))
   raw.append(alpha)
 def chunk(tag,data):return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data)&0xffffffff)
 data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(raw)))+chunk(b'IEND',b'')
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
def box(a,b):
 faces={side:{'texture':'#crystal'}for side in ('north','south','east','west','up','down')}
 for side,axis,value in [('west',0,0),('east',0,16),('down',1,0),('up',1,16),('north',2,0),('south',2,16)]:
  if (a[axis] if value==0 else b[axis])==value:faces[side]['cullface']=side
 return {'from':a,'to':b,'faces':faces}
# Photo reference: Naica's intergrown gypsum columns. Keep blunt Minecraft prisms,
# varied lengths and lean, and broad roots only where an actual neighboring cluster exists.
rotations={'up':{},'down':{'x':180},'north':{'x':90},'east':{'x':90,'y':90},'south':{'x':90,'y':180},'west':{'x':90,'y':270}}
model_dir=Path('assets/riftfall/models/block/mana_crystals')
part_id=0
def geometry(name,elements,base_count=2):
 global part_id
 for index,element in enumerate(elements):
  for face in element['faces'].values():
   face['tintindex']=part_id | (65536 if index<base_count else 0)
   if index<base_count:face['texture']='#bed'
  part_id+=1
 write(model_dir/(name+'.json'),{'parent':'minecraft:block/block','render_type':'minecraft:translucent','ambientocclusion':False,'textures':{'particle':'#crystal','bed':'#crystal','tip':'#crystal','tip_cap':'#crystal'},'elements':elements})
def column(x,z,w,d,height,h,axis=None,angle=0):
 base=min(1.25,h*.20);top=max(base+.25,height*h)
 if axis and h>=6:
  half=(w if axis=='z'else d)/2;theta=math.radians(abs(angle))
  top=min(top,base+(h-base-half*math.sin(theta))/math.cos(theta))
 result=box([x,base,z],[x+w,top,z+d])
 if height>=.65 and top-base>=5:
  # Only the terminal 30% fades; normalize vertical UVs along the prism before attachment/lean rotations.
  horizontal={'north':(16-x-w,16-x),'south':(x,x+w),'west':(z,z+d),'east':(16-z-d,16-z)}
  for side,(start,end) in horizontal.items():result['faces'][side].update(texture='#tip',uv=[start,0,end,16])
  result['faces']['up']['texture']='#tip_cap'
 if axis and h>=6:result['rotation']={'origin':[x+w/2,base,z+d/2],'axis':axis,'angle':angle,'rescale':False}
 return result
for layer in range(1,9):
 h=layer*2
 for variant in range(2):
  elements=[box([3,0,4],[12,min(1.2,h*.3),12]),box([6,0,2],[10,min(1.4,h*.35),13])]
  if variant==0:
   columns=[(6,6,4.5,4,1,None,0),(3,5,4,4,.86,'z',-22.5),(8,4,3.5,4,.77,'x',22.5),(4,9,3,3,.52,'z',22.5),(10,9,2,2,.38,None,0)]
  else:
   columns=[(6,6,4.5,4.5,.95,'z',22.5),(8,4,3.5,4,.92,'x',22.5),(3,8,3.5,3,.65,'z',-22.5),(8,10,3,3,.46,'x',-22.5),(4,3,2.5,3,.34,None,0)]
  for x,z,w,d,height,axis,angle in columns:elements.append(column(x,z,w,d,height,h,axis,angle))
  # Small offset daughter growths share the larger column's root, not a regular row of posts.
  elements.extend([box([2,0,6],[5,min(h*.32,4),9]),box([9,0,3],[12,min(h*.28,3.5),6])])
  geometry(f'template_{layer}_{variant}',elements)
 # North-facing root/column slice. Rotate these vertices BEFORE the attachment rotation:
 # this preserves exact tangent directions even on walls and ceilings.
 join=[box([0,0,0],[16,min(1.2,h*.3),2]),box([0,0,2],[5,min(1.2,h*.3),6]),box([5,0,2],[11,min(1.2,h*.3),8]),box([11,0,2],[16,min(1.2,h*.3),5]),box([5.5,min(.4,h*.1),0],[9.5,h*.42,7]),box([9,0,0],[12,h*.22,4])]
 for side in range(4):
  elements=[]
  for element in join:
   corners=[]
   for x in (element['from'][0],element['to'][0]):
    for z in (element['from'][2],element['to'][2]):
     rx,rz=x,z
     for _ in range(side):rx,rz=16-rz,rx
     corners.append((rx,rz))
   elements.append(box([min(x for x,z in corners),element['from'][1],min(z for x,z in corners)],[max(x for x,z in corners),element['to'][1],max(z for x,z in corners)]))
  geometry(f'template_{layer}_join_{side}',elements,base_count=4)
for tier,color in COLORS.items():
 block=tier+'_mana_crystal_cluster';raw='raw_'+tier+'_mana_crystal';cut='cut_'+tier+'_mana_crystal'
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal.png'),color)
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal_bed.png'),color,opaque=True)
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal_tip.png'),color,tip=True)
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal_tip_cap.png'),color,cap=True)
 texture(Path('assets/riftfall/textures/item')/(cut+'.png'),color,True)
 multipart=[]
 for layer in range(1,9):
  models=[]
  for variant in range(2):
   name=f'{tier}_{layer}'+('_wild'if variant else '')
   write(model_dir/(name+'.json'),{'parent':f'riftfall:block/mana_crystals/template_{layer}_{variant}','render_type':'minecraft:translucent','textures':{'crystal':f'riftfall:block/{tier}_mana_crystal','bed':f'riftfall:block/{tier}_mana_crystal_bed','tip':f'riftfall:block/{tier}_mana_crystal_tip','tip_cap':f'riftfall:block/{tier}_mana_crystal_tip_cap'}})
   models.append(f'riftfall:block/mana_crystals/{name}')
  for side in range(4):write(model_dir/f'{tier}_{layer}_join_{side}.json',{'parent':f'riftfall:block/mana_crystals/template_{layer}_join_{side}','render_type':'minecraft:translucent','textures':{'crystal':f'riftfall:block/{tier}_mana_crystal','bed':f'riftfall:block/{tier}_mana_crystal_bed','tip':f'riftfall:block/{tier}_mana_crystal_tip','tip_cap':f'riftfall:block/{tier}_mana_crystal_tip_cap'}})
  for facing,rotation in rotations.items():
   condition={'facing':facing,'layers':str(layer)}
   multipart.append({'when':condition,'apply':[{'model':model,**rotation}for model in models]})
   for side,direction in enumerate(('north','east','south','west')):
    multipart.append({'when':{**condition,'join_'+direction:'true'},'apply':{'model':f'riftfall:block/mana_crystals/{tier}_{layer}_join_{side}',**rotation}})
 write(Path('assets/riftfall/blockstates')/(block+'.json'),{'multipart':multipart})
 write(Path('assets/riftfall/models/item')/(raw+'.json'),{'parent':f'riftfall:block/mana_crystals/{tier}_8'})
 write(Path('assets/riftfall/models/item')/(cut+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'riftfall:item/{cut}'}})
 children=[]
 for layer in range(1,9):children.append({'type':'minecraft:item','name':'riftfall:'+raw,'conditions':[{'condition':'minecraft:block_state_property','block':'riftfall:'+block,'properties':{'layers':str(layer)}}],'functions':[{'function':'minecraft:set_count','count':layer}]})
 write(Path('data/riftfall/loot_table/blocks')/(block+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:alternatives','children':children}]}]})
 write(Path('data/riftfall/recipe')/(cut+'.json'),{'type':'minecraft:stonecutting','ingredient':{'item':'riftfall:'+raw},'result':{'id':'riftfall:'+cut,'count':1}})
write(Path('data/riftfall/tags/block/mana_crystal_supports.json'),{'replace':False,'values':['#minecraft:base_stone_overworld','#minecraft:base_stone_nether','minecraft:calcite','minecraft:end_stone','#c:ores']})
# Future crafting recipes can use cut-only tags; raw crystals are never included.
write(Path('data/riftfall/tags/item/cut_mana_crystals.json'),{'replace':False,'values':['riftfall:cut_'+tier+'_mana_crystal'for tier in COLORS]})
lang=ROOT/'assets/riftfall/lang/en_us.json';data=json.loads(lang.read_text())
for tier in COLORS:
 data['block.riftfall.'+tier+'_mana_crystal_cluster']=tier.title()+' Mana Crystals'
 data['item.riftfall.raw_'+tier+'_mana_crystal']='Raw '+tier.title()+' Mana Crystal'
 data['item.riftfall.cut_'+tier+'_mana_crystal']='Cut '+tier.title()+' Mana Crystal'
lang.write_text(json.dumps(data,indent=2)+'\n')
