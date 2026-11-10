"""Generate a looping, bent-over pant with breathing shoulders and hands resting on thighs.
Usage: python tools/generate_goblin_pant.py EPIC_FIGHT_JAR (requires NumPy).
"""
from pathlib import Path
import json, math, sys, zipfile
import numpy as np
with zipfile.ZipFile(sys.argv[1]) as jar:
    rig=json.loads(jar.read('assets/epicfight/animmodels/entity/biped.json'))['armature']['hierarchy'][0]
nodes={}
def collect(node):
    nodes[node['name']]=node
    for child in node['children']: collect(child)
collect(rig)
def rotate(x,y,z):
    x,y,z=map(math.radians,(x,y,z)); c,s=math.cos,math.sin
    return np.array([[1,0,0,0],[0,c(x),-s(x),0],[0,s(x),c(x),0],[0,0,0,1]]) @ np.array([[c(y),0,s(y),0],[0,1,0,0],[-s(y),0,c(y),0],[0,0,0,1]]) @ np.array([[c(z),-s(z),0,0],[s(z),c(z),0,0],[0,0,1,0],[0,0,0,1]])
times=[i/20 for i in range(41)]
frames=[]
for t in times:
    breath=(1-math.cos(2*math.pi*t))/2
    pose={'Root':(-6,0,0),'Torso':(-18-3*breath,0,0),'Chest':(-10-3*breath,0,0),'Head':(18+2*breath,0,0),
          'Thigh_R':(14,0,-6),'Thigh_L':(14,0,6),'Leg_R':(-22,0,0),'Leg_L':(-22,0,0),
          'Arm_R':(12+2*breath,0,-12),'Arm_L':(12+2*breath,0,12),'Hand_R':(-28,0,0),'Hand_L':(-28,0,0)}
    frame={}
    for name,node in nodes.items():
        m=np.array(node['transform']).reshape(4,4) @ rotate(*pose.get(name,(0,0,0)))
        if name=='Root':m[2,3]-=.06+.006*breath
        frame[name]=[round(float(v),6) for v in m.flatten()]
    frames.append(frame)
channels=[{'name':name,'time':times,'transform':[f[name] for f in frames]} for name in nodes]
out=Path(__file__).resolve().parents[1]/'src/main/resources/assets/riftfall/animmodels/animations/biped/goblin_pant.json'
out.write_text(json.dumps({'animation':channels},separators=(',',':'))+'\n')
