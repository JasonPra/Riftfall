#!/usr/bin/env python3
"""Compile three bespoke, loop-free Legacy crowns and taproot systems to DT JoCodes.

A connected voxel tree (never a solid 3x3 column) lets Dynamic Trees calculate branch
thickness, leaf support, growth, stripping and felling. No copied jungle JoCodes.
"""
from pathlib import Path
import random
DIRECTIONS=[(0,-1,0),(0,1,0),(0,0,-1),(0,0,1),(-1,0,0),(1,0,0)]
ALPHABET='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

def add(a,b):return tuple(x+y for x,y in zip(a,b))
class Shape:
 def __init__(self):self.nodes={(0,0,0):[]}
 def path(self,start,moves):
  pos=start;result=[]
  for direction in moves:
   nxt=add(pos,DIRECTIONS[direction])
   if nxt in self.nodes or any(add(nxt,d) in self.nodes and add(nxt,d)!=pos for d in DIRECTIONS):break
   self.nodes[pos].append((direction,nxt));self.nodes[nxt]=[];pos=nxt;result.append(pos)
  return result
 def encode(self):
  def walk(pos):
   code=[];children=self.nodes[pos]
   for i,(d,nxt) in enumerate(children):
    if i<len(children)-1:code.append(6)
    code.append(d);code.extend(walk(nxt))
    if i<len(children)-1:code.append(7)
   return code
  code=walk((0,0,0))
  if len(code)%2:code.append(7)
  return ''.join(ALPHABET[(a<<3)|b] for a,b in zip(code[::2],code[1::2]))

def toward(s,start,target):
 """An orthogonal, gently rising limb; adjacency checks keep the live graph acyclic."""
 pos=start;path=[]
 while pos!=target:
  delta=[target[i]-pos[i] for i in range(3)]
  # Prefer the longest remaining component, rather than a stair every fixed N blocks.
  axis=max(range(3),key=lambda i:abs(delta[i]))
  if not delta[axis]:break
  direction=({0:(4,5),1:(0,1),2:(2,3)}[axis])[delta[axis]>0]
  added=s.path(pos,[direction])
  if not added:break
  pos=added[-1];path.append(pos)
 return path

def crown(seed):
 import math
 rng=random.Random(seed);s=Shape();height=35+rng.randrange(3)
 # Broad lower bole; above the early forks the leader bends and tapers into the crown.
 spine=[];pos=(0,0,0)
 for y in range(1,height+1):
  if y>17 and y%7==0:
   direction=[5,2,4,3][(y//7+seed)%4]
   added=s.path(pos,[direction])
   if added:pos=added[-1];spine.extend(added)
  added=s.path(pos,[1])
  if not added:break
  pos=added[-1];spine.extend(added)
 by_height={p[1]:p for p in spine}
 arms=[]
 # Six uneven primary boughs, arching upwards before opening into a wide dome.
 for i in range(6):
  angle=(i+rng.uniform(-.16,.16))*math.tau/6
  dx,dz=math.cos(angle),math.sin(angle)
  level=11+i+rng.randrange(3)
  base=by_height[level];arm=[];pos=base
  for distance in [4,9,15,20]:
   rise={4:2,9:7,15:12,20:15}[distance]+rng.randrange(3)
   target=(round(dx*distance),level+rise,round(dz*distance))
   segment=toward(s,pos,target)
   if not segment:break
   arm.extend(segment);pos=segment[-1]
   if pos!=target:break
  arms.append((arm,dx,dz))
 # Build all main limbs before the finer branches, so twigs cannot block a major fork.
 for arm,dx,dz in arms:
  for index in range(7,len(arm)-1,5):
   base=arm[index]
   for side in [-1,1]:
    spread=rng.randrange(4,8)
    tx=max(-21,min(21,round(base[0]-dz*side*spread+dx*2)))
    tz=max(-21,min(21,round(base[2]+dx*side*spread+dz*2)))
    radius=math.hypot(tx,tz)
    ty=round(35-6*(min(radius,23)/23)**2)+rng.randrange(-3,3)
    ty=max(base[1]+2,min(height+1,ty))
    twig=toward(s,base,(tx,ty,tz))
    if not twig:continue
    # Short offshoots give rounded, overlapping foliage pads and visible gaps underneath.
    for k in range(3,len(twig)-1,4):
     tip=twig[k];horizontal=rng.choice([2,3,4,5])
     sprig=s.path(tip,[horizontal]*rng.randrange(2,4))
     if sprig:s.path(sprig[-1],[1]*rng.randrange(1,4))
    s.path(twig[-1],[1]*rng.randrange(1,4))
 # Uneven upper crown: lateral pads at varied heights, rather than stacked radial tiers.
 for level in [25,29,33,height-2]:
  base=by_height.get(level)
  if base is None:continue
  for i in range(5):
   angle=(i+rng.random()*.5)*math.tau/5
   reach=rng.randrange(7,15) if level<31 else rng.randrange(5,10)
   target=(max(-20,min(20,base[0]+round(math.cos(angle)*reach))),min(height+2,level+rng.randrange(2,6)),max(-20,min(20,base[2]+round(math.sin(angle)*reach))))
   twig=toward(s,base,target)
   if twig:
    for side in [2,3,4,5]:s.path(twig[-1],[side]*rng.randrange(2,4))
 return s

def roots(seed):
 rng=random.Random(seed);s=Shape();axis=s.path((0,0,0),[0]*16)
 for depth in [3,7,12]:
  for d in rng.sample([2,3,4,5],3):
   moves=[]
   for i in range(rng.randrange(4,8)):
    moves.append(d)
    if i>1 and rng.random()<.45:moves.append(0)
   branch=s.path(axis[depth-1],moves)
   if branch:s.path(branch[-1],[0]*rng.randrange(2,5))
 return s

if __name__=='__main__':
 out=Path(__file__).resolve().parents[2]/'src/main/resources/trees/riftfall/jo_codes/vermillion_legacy.txt';out.parent.mkdir(parents=True,exist_ok=True)
 shapes=[crown(seed) for seed in [19,37,83]];root_shapes=[roots(seed) for seed in [19,37,83]]
 out.write_text('\n'.join([f'{radius}:{s.encode()}' for radius in range(2,9) for s in shapes]+[f'*{radius}:{root.encode()}' for radius in range(2,9) for root in root_shapes])+'\n')
 for s in shapes:
  points=list(s.nodes);print('Crown:',len(points),'branches; bounds',tuple(min(p[i] for p in points) for i in range(3)),tuple(max(p[i] for p in points) for i in range(3)))
