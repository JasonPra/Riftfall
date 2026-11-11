"""Paired glowing cores, translucent shells and crystal geometry. Python standard library only."""
import copy,json,math,random,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
COLORS={'red':0xB94D57,'orange':0xCB8450,'yellow':0xD0BB67,'green':0x619A72,'cyan':0x65A8B4,'blue':0x526FAE,'violet':0x8A64AD,'white':0xD8DFE8}
GLOW={'red':0xFF4764,'orange':0xFF953F,'yellow':0xFFE35C,'green':0x68F18C,'cyan':0x5CE9FF,'blue':0x6C9EFF,'violet':0xC28AFF,'white':0xF0FCFF}
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def texture(path,color,cut=False,core=False):
 r,g,b=(color>>16)&255,(color>>8)&255,color&255;noise=random.Random(41);raw=bytearray()
 for y in range(32):
  raw.append(0)
  for x in range(32):
   if cut:
    inside=5<=x<=26 and 4<=y<=27 and 10<=x+y<=49 and -20<=x-y<=21
    factor=1.15 if x<10 or y<9 else .65 if x>22 or y>23 else .95
    alpha=255 if inside else 0
   elif core:
    # Cloudy emission fades at the sprite edges instead of outlining an opaque insert.
    cloud=.09*math.sin(x*.31+y*.19)+.06*math.cos(y*.37-x*.14)+noise.uniform(-.025,.025)
    edge=max(0,min(1,min(x,y,31-x,31-y)/7))
    factor=.77+cloud;alpha=round(35+130*edge+cloud*70)
   else:
    cloud=.10*math.sin(x*.32+y*.19)+.07*math.sin(y*.41-x*.16)+noise.uniform(-.035,.035)
    # Denser cloudy mineral towards the lower face, without bright facet stripes.
    base=y/31
    factor=.94+cloud;alpha=max(205,min(250,round(218+base*23+cloud*55)))
   for value in (r,g,b):raw.append(max(0,min(255,round(value*factor+12))))
   raw.append(alpha)
 def chunk(tag,data):return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data)&0xffffffff)
 data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(raw)))+chunk(b'IEND',b'')
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
def box(a,b,matrix=False,edge=False):
 faces={side:{'texture':'#crystal'}for side in ('north','south','east','west','up','down')}
 for side,axis,value in [('west',0,0),('east',0,16),('down',1,0),('up',1,16),('north',2,0),('south',2,16)]:
  if (a[axis] if value==0 else b[axis])==value:faces[side]['cullface']=side
 return {'from':a,'to':b,'faces':faces,'_matrix':matrix,'_edge':edge}
# Photo reference: Naica's intergrown gypsum columns. Keep blunt Minecraft prisms,
# varied lengths and lean, and broad roots only where an actual neighboring cluster exists.
rotations={'up':{},'down':{'x':180},'north':{'x':90},'east':{'x':90,'y':90},'south':{'x':90,'y':180},'west':{'x':90,'y':270}}
model_dir=Path('assets/riftfall/models/block/mana_crystals')
part_id=1000
def geometry(name,elements):
 global part_id
 shells=[];cores=[]
 for element in elements:
  shell=copy.deepcopy(element);matrix=shell.pop('_matrix',False);edge=shell.pop('_edge',False);shell['shade']=True
  for face in shell['faces'].values():face['tintindex']=-1 if matrix else part_id|(1048576 if edge else 0)
  shells.append(shell)
  if matrix:
   part_id+=1
   continue
  core=copy.deepcopy(shell)
  for axis in range(3):
   span=element['to'][axis]-element['from'][axis];inset=min(span*.24,1.2)
   core['from'][axis]+=span*.35 if axis==1 else inset;core['to'][axis]-=min(span*.12,.65) if axis==1 else inset
  core['shade']=False
  # NeoForge 1.21.1 uses ExtraFaceData (vanilla light_emission is not supported here).
  core['neoforge_data']={'block_light':15,'sky_light':15,'ambient_occlusion':False}
  for face in core['faces'].values():face['texture']='#core';face['tintindex']=part_id|(1048576 if edge else 0);face.pop('cullface',None)
  part_id+=1
  cores.append(core)
 write(model_dir/(name+'.json'),{'parent':'minecraft:block/block','render_type':'minecraft:translucent','ambientocclusion':True,'textures':{'particle':'#crystal'},'elements':shells+cores})
def column(x,z,w,d,height,h,axis=None,angle=0):
 scale=.3+.7*h/16
 x+=w*(1-scale)/2;z+=d*(1-scale)/2;w*=scale;d*=scale
 base=min(1.25,h*.20) if axis and h>=6 else 0;top=max(base+.25,height*h)
 if axis and h>=6:
  half=(w if axis=='z'else d)/2;theta=math.radians(abs(angle))
  top=min(top,base+(h-base-half*math.sin(theta))/math.cos(theta))
 result=box([x,base,z],[x+w,top,z+d])
 if axis and h>=6:result['rotation']={'origin':[x+w/2,base,z+d/2],'axis':axis,'angle':angle,'rescale':False}
 return result
for layer in range(1,9):
 h=layer*2
 for variant in range(6):
  elements=[]
  if variant==0:
   columns=[(6,6,4.5,4,1,None,0),(3,5,4,4,.86,'z',-22.5),(8,4,3.5,4,.77,'x',22.5),(4,9,3,3,.52,'z',22.5),(10,9,2,2,.38,None,0)]
  elif variant==1:
   columns=[(6,6,4.5,4.5,.95,'z',22.5),(8,4,3.5,4,.92,'x',22.5),(3,8,3.5,3,.65,'z',-22.5),(8,10,3,3,.46,'x',-22.5),(4,3,2.5,3,.34,None,0)]
  else:
   # Distinct authored silhouettes, with the same contact buds at the perimeter.
   columns={
    2:[(4,6,4,4,.98,'z',-22.5),(8,7,4,3.5,.79,'x',-22.5),(6,3,3,3,.61,None,0),(10,8.5,2.5,3,.43,None,0),(4,9,3,3,.38,None,0)],
    3:[(7,5,4,4,1,'x',22.5),(5,7,3.5,4,.72,'z',22.5),(8,10,3,3,.64,None,0),(4,3,3,3,.4,None,0),(11,6,2,3,.31,None,0)],
    4:[(6.5,7,4,4,.96,'z',22.5),(8,5.5,4,4,.88,'x',-22.5),(3,4,3,3,.55,None,0),(9,9,3,3,.46,None,0),(4,9,2.5,3,.35,None,0)],
    5:[(7,7,4,4,1,None,0),(4,4,4,4,.83,'x',22.5),(9,4,3,3,.66,'z',-22.5),(4,9,3.5,3,.57,None,0),(9.5,9,2.5,2.5,.39,None,0)]
   }[variant]
  for x,z,w,d,height,axis,angle in columns:elements.append(column(x,z,w,d,height,h,axis,angle))
  # Small offset daughter growths share the larger column's root, not a regular row of posts.
  elements.extend([box([.5,0,6],[3.5,min(h,1.2+h*.25,4),9],edge=True),box([12.5,0,7],[15.5,min(h,1.2+h*.22,3.5),10],edge=True)] if variant==0 else [box([6,0,.5],[9,min(h,1.2+h*.25,4),3.5],edge=True),box([7,0,12.5],[10,min(h,1.2+h*.22,3.5),15.5],edge=True)])
  # Overlapping short prisms form a branching clump, never a continuous sheet.
  for cx,cz,fraction in [(4.5,7.5,.21),(8,7.5,.29),(11,8,.23),(7.5,10.5,.18),(7.5,4.5,.25)]:
   elements.append(box([cx-2,0,cz-2],[cx+2,min(h,1+h*fraction,5),cz+2]))
  geometry(f'template_{layer}_{variant}',elements)
 # North-facing root/column slice. Rotate these vertices BEFORE the attachment rotation:
 # this preserves exact tangent directions even on walls and ceilings.
 join=[column(5.5,1,3,3,.42,h)]
 # Actual shared seams carry small daughter crystals across the full edge.
 # Overlapping individual buds cover the shared edge, including both corners.
 for x,height,depth in [(0,.10,3),(3,.175,3.5),(6,.12,3),(9,.16,3.75),(12,.08,3.25)]:
  join.append(box([x,0,0],[x+4,min(h,1.2+h*height,4),depth],edge=True))
 # Staggered small pillars link the edge buds back into the central clump.
 for cx,cz,fraction in [(1.75,3.75,.19),(14.25,3.75,.23),(3.5,3.5,.22),(5.5,5,.27),(8,3,.24),(8,5.5,.3),(10.5,5,.2),(12,3.5,.26)]:
  join.append(box([cx-1.6,0,cz-1.75],[cx+1.6,min(h,1+h*fraction,5),cz+1.75]))
 for side in range(4):
  elements=[]
  for element in join:
   corners=[]
   for x in (element['from'][0],element['to'][0]):
    for z in (element['from'][2],element['to'][2]):
     rx,rz=x,z
     for _ in range(side):rx,rz=16-rz,rx
     corners.append((rx,rz))
   elements.append(box([min(x for x,z in corners),element['from'][1],min(z for x,z in corners)],[max(x for x,z in corners),element['to'][1],max(z for x,z in corners)],matrix=element.get('_matrix',False),edge=element.get('_edge',False)))
  geometry(f'template_{layer}_join_{side}',elements)
# Only complete mature 2x2x2 junctions use these vertical intergrown columns crowded toward their shared center.
# They join at the shared corner, with no full-width sheet or floating cap.
for corner in range(4):
 elements=[]
 for a,b in [([10,.5,2],[13.5,13,5.5]),([12,1,0],[15,15,3]),([14,.75,1],[16,16,4])]:
  points=[]
  for x in (a[0],b[0]):
   for z in (a[2],b[2]):
    rx,rz=x,z
    for _ in range(corner):rx,rz=16-rz,rx
    points.append((rx,rz))
  elements.append(box([min(x for x,z in points),a[1],min(z for x,z in points)],[max(x for x,z in points),b[1],max(z for x,z in points)]))
 geometry(f'template_junction_{corner}',elements)
for tier,color in COLORS.items():
 block=tier+'_mana_crystal_cluster';raw='raw_'+tier+'_mana_crystal';cut='cut_'+tier+'_mana_crystal'
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal.png'),color)
 texture(Path('assets/riftfall/textures/item')/(cut+'.png'),color,True)
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal_core.png'),GLOW[tier],core=True)
 for corner in range(4):write(model_dir/f'{tier}_junction_{corner}.json',{'parent':f'riftfall:block/mana_crystals/template_junction_{corner}','render_type':'minecraft:translucent','textures':{'crystal':f'riftfall:block/{tier}_mana_crystal','core':f'riftfall:block/{tier}_mana_crystal_core'}})
 variants={}
 for layer in range(1,9):
  models=[]
  for variant in range(6):
   name=f'{tier}_{layer}'+('_wild'if variant==1 else f'_preset_{variant}' if variant>1 else '')
   write(model_dir/(name+'.json'),{'parent':f'riftfall:block/mana_crystals/template_{layer}_{variant}','render_type':'minecraft:translucent','textures':{'crystal':f'riftfall:block/{tier}_mana_crystal','core':f'riftfall:block/{tier}_mana_crystal_core'}})
   models.append(f'riftfall:block/mana_crystals/{name}')
  for side in range(4):write(model_dir/f'{tier}_{layer}_join_{side}.json',{'parent':f'riftfall:block/mana_crystals/template_{layer}_join_{side}','render_type':'minecraft:translucent','textures':{'crystal':f'riftfall:block/{tier}_mana_crystal','core':f'riftfall:block/{tier}_mana_crystal_core'}})
  for facing,rotation in rotations.items():
   for variant,model in enumerate(models):variants[f'facing={facing},layers={layer},variant={variant}']={'model':model,**rotation}
 write(Path('assets/riftfall/blockstates')/(block+'.json'),{'variants':variants})
 write(Path('assets/riftfall/models/item')/(raw+'.json'),{'parent':f'riftfall:block/mana_crystals/{tier}_8'})
 write(Path('assets/riftfall/models/item')/(cut+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'riftfall:item/{cut}'}})
 children=[]
 for layer in range(1,9):children.append({'type':'minecraft:item','name':'riftfall:'+raw,'conditions':[{'condition':'minecraft:block_state_property','block':'riftfall:'+block,'properties':{'layers':str(layer)}}],'functions':[{'function':'minecraft:set_count','count':layer}]})
 write(Path('data/riftfall/loot_table/blocks')/(block+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:alternatives','children':children}]}]})
 write(Path('data/riftfall/recipe')/(cut+'.json'),{'type':'minecraft:stonecutting','ingredient':{'item':'riftfall:'+raw},'result':{'id':'riftfall:'+cut,'count':1}})
write(Path('data/riftfall/tags/block/mana_crystal_supports.json'),{'replace':False,'values':['#minecraft:base_stone_overworld','#minecraft:base_stone_nether','minecraft:calcite','minecraft:end_stone']})
# Future crafting recipes can use cut-only tags; raw crystals are never included.
write(Path('data/riftfall/tags/item/cut_mana_crystals.json'),{'replace':False,'values':['riftfall:cut_'+tier+'_mana_crystal'for tier in COLORS]})
lang=ROOT/'assets/riftfall/lang/en_us.json';data=json.loads(lang.read_text())
for tier in COLORS:
 data['block.riftfall.'+tier+'_mana_crystal_cluster']=tier.title()+' Mana Crystals'
 data['item.riftfall.raw_'+tier+'_mana_crystal']='Raw '+tier.title()+' Mana Crystal'
 data['item.riftfall.cut_'+tier+'_mana_crystal']='Cut '+tier.title()+' Mana Crystal'
lang.write_text(json.dumps(data,indent=2)+'\n')
