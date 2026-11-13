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
  added=[]
  for axis in sorted(range(3),key=lambda i:abs(delta[i]),reverse=True):
   if not delta[axis]:continue
   direction=({0:(4,5),1:(0,1),2:(2,3)}[axis])[delta[axis]>0]
   added=s.path(pos,[direction])
   if added:break
  if not added:break
  pos=added[-1];path.append(pos)
 return path

def crown(seed):
 import math
 rng=random.Random(seed);s=Shape();spine=[];pos=(0,0,0)
 # Four main limbs carry the crown. Fine forks stay in its upper leafy volume.
 for y in range(1,32):
  if y in [17,25]:
   step=s.path(pos,[5 if seed%2 else 4])
   if step:pos=step[-1];spine.extend(step)
  step=s.path(pos,[1])
  if not step:break
  pos=step[-1];spine.extend(step)
 by_height={p[1]:p for p in spine};phase=rng.uniform(0,math.tau)
 arms=[]
 for i in range(4):
  angle=phase+(i+rng.uniform(-.07,.07))*math.tau/4
  dx,dz=math.cos(angle),math.sin(angle);level=9+i
  pos=by_height[level];arm=[]
  for distance,height in [(4,level+4),(9,21),(14,25),(18,26)]:
   target=(round(dx*distance),height,round(dz*distance))
   segment=toward(s,pos,target);arm.extend(segment)
   if not segment:break
   pos=segment[-1]
   if pos!=target:break
  arms.append((arm,dx,dz))
 hubs=[]
 def dome(x,z):return round(32-9*(min(math.hypot(x,z),22)/22)**2)
 for arm,dx,dz in arms:
  if not arm:continue
  hubs.append(arm[-1])
  # Just two secondary forks per limb, alternating sides. No ladder of vertical
  # shoots or repeated fans along the exposed underside.
  for fraction,side in [(.58,-1),(.80,1)]:
   base=arm[min(len(arm)-1,round((len(arm)-1)*fraction))]
   tx=round(base[0]+dx*4-dz*side*7);tz=round(base[2]+dz*4+dx*side*7)
   tx=max(-16,min(16,tx));tz=max(-16,min(16,tz))
   twig=toward(s,base,(tx,max(base[1]+1,dome(tx,tz)),tz))
   if twig:hubs.append(twig[-1])
 # The central crown is higher than the skirt, producing a rounded roof rather
 # than a flat plane. These short upper forks are hidden by overlapping foliage.
 for level in [27,31]:
  base=by_height[level]
  for i in range(4):
   angle=phase+(i+.45)*math.tau/4;reach=7 if level==27 else 5
   tx=round(math.cos(angle)*reach);tz=round(math.sin(angle)*reach)
   twig=toward(s,base,(tx,dome(tx,tz),tz))
   if twig:hubs.append(twig[-1])
 # A small number of short, crown-level terminal forks supports broader native
 # Legacy leaf clusters. Foliage supplies fullness instead of extra scaffold.
 for base in hubs:
  for direction in rng.sample([2,3,4,5],3):
   length=rng.randrange(2,4);target=add(base,tuple(v*length for v in DIRECTIONS[direction]))
   if abs(target[0])>18 or abs(target[2])>18:continue
   twig=toward(s,base,(target[0],max(base[1],dome(target[0],target[2])),target[2]))
   if twig:s.path(twig[-1],[1])
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
