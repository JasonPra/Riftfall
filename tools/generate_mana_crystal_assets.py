"""Original translucent pixel textures and cuboid crystal geometry. Python standard library only."""
import json,math,random,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
COLORS={'red':0xB94D57,'orange':0xCB8450,'yellow':0xD0BB67,'green':0x619A72,'cyan':0x65A8B4,'blue':0x526FAE,'violet':0x8A64AD,'white':0xD8DFE8}
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def texture(path,color,cut=False):
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
rotations={'up':{},'down':{'x':180},'north':{'x':90},'east':{'x':90,'y':90},'south':{'x':90,'y':180},'west':{'x':90,'y':270}}
for tier,color in COLORS.items():
 block=tier+'_mana_crystal_cluster';raw='raw_'+tier+'_mana_crystal';cut='cut_'+tier+'_mana_crystal'
 texture(Path('assets/riftfall/textures/block')/(tier+'_mana_crystal.png'),color)
 texture(Path('assets/riftfall/textures/item')/(cut+'.png'),color,True)
 variants={}
 for layer in range(1,9):
  h=layer*2;base=max(.5,h-4);elements=[box([0,0,0],[16,base,16])]
  for a,b,f in [([2,0,2],[8,0,8],1),([9,0,9],[14,0,14],.9),([10,0,1],[15,0,5],.72),([1,0,10],[5,0,15],.8),([0,0,6],[3,0,10],.78),([13,0,6],[16,0,10],.78),([6,0,0],[10,0,3],.78),([6,0,13],[10,0,16],.78)]:
   top=max(base+.4,h*f);middle=(base+top)/2
   elements.append(box([a[0],base,a[2]],[b[0],middle,b[2]]))
   inset=.5 if b[0]-a[0]>3 else .25
   elements.append(box([a[0]+inset,middle,a[2]+.5],[b[0]-inset,top,b[2]-.5]))
  model=f'riftfall:block/mana_crystals/{tier}_{layer}'
  write(Path('assets/riftfall/models/block/mana_crystals')/f'{tier}_{layer}.json',{'parent':'minecraft:block/block','render_type':'minecraft:translucent','ambientocclusion':False,'textures':{'particle':f'riftfall:block/{tier}_mana_crystal','crystal':f'riftfall:block/{tier}_mana_crystal'},'elements':elements})
  for facing,rotation in rotations.items():variants[f'facing={facing},layers={layer}']={'model':model,**rotation}
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
