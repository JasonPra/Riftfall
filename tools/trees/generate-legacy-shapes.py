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

def crown(seed):
 rng=random.Random(seed);s=Shape();height=64+rng.randrange(6);trunk=s.path((0,0,0),[1]*height)
 # Diagonal boughs fill the spaces between the main limbs, avoiding a cross-shaped canopy.
 for level in [36,45,54]:
  for eastwest,northsouth in [(4,2),(4,3),(5,2),(5,3)]:
   moves=[]
   for i in range(rng.randrange(12,17)):
    moves.extend([eastwest,northsouth])
    if i>1 and i%4==0:moves.append(1)
   arm=s.path(trunk[level-1],moves)
   for j in range(7,len(arm)-2,6):
    for side in [eastwest,northsouth]:
     tip=s.path(arm[j],[side]*rng.randrange(3,6)+[1]*rng.randrange(2,5))
   if arm:s.path(arm[-1],[1]*3)
 for level in [32,41,50,59]:
  for direction in [2,3,4,5]:
   main=[]
   length=rng.randrange(17,23) if level<55 else rng.randrange(11,17)
   for i in range(length):
    main.append(direction)
    if i>2 and i%4==0:main.append(1)
   arm=s.path(trunk[level-1],main)
   perpendicular=[4,5] if direction in [2,3] else [2,3]
   for j in range(4,len(arm)-2,4):
    for side in perpendicular:
     moves=[]
     for k in range(rng.randrange(4,9)):
      moves.append(side)
      if k>=2 and k%3==0:moves.append(1)
     twig=s.path(arm[j],moves)
     if twig:s.path(twig[-1],[1]*rng.randrange(2,5))
   if arm:s.path(arm[-1],[1]*3)
 # A compact summit joins the crown visually without a flat umbrella top.
 for level in [height-5,height]:
  for direction in [2,3,4,5]:
   arm=s.path(trunk[level-1],[direction]*4+[1]*2+[direction]*3)
   if arm:s.path(arm[-1],[1]*2)
 return s

def roots(seed):
 s=Shape();axis=s.path((0,0,0),[0]*16)
 for depth in [3,8,13]:
  for d in [2,3,4,5]:
   moves=[]
   for i in range(7 if depth<10 else 4):
    moves.append(d)
    if i>1 and i%3==0:moves.append(0)
   branch=s.path(axis[depth-1],moves)
   if branch:s.path(branch[-1],[0]*3)
 return s

if __name__=='__main__':
 out=Path(__file__).resolve().parents[2]/'src/main/resources/trees/riftfall/jo_codes/vermillion_legacy.txt';out.parent.mkdir(parents=True,exist_ok=True)
 shapes=[crown(seed) for seed in [19,37,83]];root=roots(19)
 out.write_text('\n'.join([f'{radius}:{s.encode()}' for radius in range(2,9) for s in shapes]+[f'*{radius}:{root.encode()}' for radius in range(2,9)])+'\n')
 for s in shapes:
  points=list(s.nodes);print('Crown:',len(points),'branches; bounds',tuple(min(p[i] for p in points) for i in range(3)),tuple(max(p[i] for p in points) for i in range(3)))
