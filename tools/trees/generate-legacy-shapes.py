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
 rng=random.Random(seed);s=Shape()
 # A short, massive bole divides into a low bowl of limbs. There is no tall leader
 # above the crown: the upper leader becomes one of the supporting boughs.
 spine=[];pos=(0,0,0)
 for y in range(1,27):
  if y in [15,21]:
   added=s.path(pos,[5 if seed%2 else 4])
   if added:pos=added[-1];spine.extend(added)
  added=s.path(pos,[1])
  if not added:break
  pos=added[-1];spine.extend(added)
 by_height={p[1]:p for p in spine}
 phase=rng.uniform(0,math.tau);arms=[]
 for i in range(7):
  angle=phase+(i+rng.uniform(-.12,.12))*math.tau/7
  dx,dz=math.cos(angle),math.sin(angle)
  level=8+i+rng.randrange(2);pos=by_height[level];arm=[]
  for distance,rise in [(4,3),(9,8),(14,12),(19,14)]:
   target=(round(dx*distance),level+rise,round(dz*distance))
   segment=toward(s,pos,target)
   arm.extend(segment)
   if not segment:break
   pos=segment[-1]
   if pos!=target:break
  arms.append((arm,dx,dz))
 # Build the scaffold first. Each limb supports several overlapping pads rather
 # than sending thin shoots up to isolated, high foliage balls.
 supports=[]
 for arm,dx,dz in arms:
  for index in range(8,len(arm),4):
   base=arm[index]
   for side in [-1,1]:
    spread=rng.randrange(4,8)
    tx=max(-20,min(20,round(base[0]-dz*side*spread+dx*2)))
    tz=max(-20,min(20,round(base[2]+dx*side*spread+dz*2)))
    radial=math.hypot(tx,tz)
    ty=round(29-4*(min(radial,23)/23)**2)+rng.randrange(-1,2)
    ty=max(base[1],ty)
    twig=toward(s,base,(tx,ty,tz))
    if twig:supports.append(twig)
 # Fill the central roof without prolonging the trunk into a separate peak.
 for level in [20,23,26]:
  base=by_height[level]
  for i in range(6):
   angle=phase+(i+rng.uniform(-.15,.15))*math.tau/6
   reach=rng.randrange(8,15)
   twig=toward(s,base,(round(math.cos(angle)*reach),29+rng.randrange(-1,2),round(math.sin(angle)*reach)))
   if twig:supports.append(twig)
 # Short lateral fans make wider, flatter leaf pads. Their tips share a shallow
 # irregular dome, leaving gaps underneath to expose the heavy branch structure.
 for twig in supports:
  for index in range(max(0,len(twig)-7),len(twig),3):
   base=twig[index]
   for direction in rng.sample([2,3,4,5],3):
    moves=[];tip=base
    for _ in range(rng.randrange(2,5)):
     tip=add(tip,DIRECTIONS[direction])
     if abs(tip[0])>21 or abs(tip[2])>21:break
     moves.append(direction)
    sprig=s.path(base,moves)
    if sprig:s.path(sprig[-1],[1]*rng.randrange(1,3))
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
