"""Author Epic Fight humanoid clips. Usage: python tools/generate_player_system_animations.py EPIC_FIGHT_JAR
Requires numpy. The original GeckoLib orc spell animation is deliberately not replaced.
"""
from pathlib import Path
import json, math, sys, zipfile
import numpy as np
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/riftfall/animmodels/animations/player_system'
with zipfile.ZipFile(sys.argv[1]) as archive:
    rig = json.loads(archive.read('assets/epicfight/animmodels/entity/biped.json'))['armature']['hierarchy'][0]
nodes = {}
def index(node):
    nodes[node['name']] = node
    for child in node['children']:
        index(child)
index(rig)
def rotation(angles):
    x,y,z = map(math.radians, angles); c,s=math.cos,math.sin
    return np.array([[1,0,0,0],[0,c(x),-s(x),0],[0,s(x),c(x),0],[0,0,0,1]]) @ np.array([[c(y),0,s(y),0],[0,1,0,0],[-s(y),0,c(y),0],[0,0,0,1]]) @ np.array([[c(z),-s(z),0,0],[s(z),c(z),0,0],[0,0,1,0],[0,0,0,1]])
def frame(direction, position):
    y=direction/np.linalg.norm(direction); x=np.array([1.,0,0]); x-=y*np.dot(x,y)
    if np.linalg.norm(x)<.01: x=np.cross(y,[0,1,0])
    x/=np.linalg.norm(x); z=np.cross(x,y)
    m=np.eye(4); m[:3,:3]=np.column_stack([x,y,z]); m[:3,3]=position
    return m
def matrices(pose):
    result={}; overrides={}
    def visit(node,parent):
        name=node['name']; local=np.array(node['transform']).reshape(4,4) @ rotation(pose.get(name,(0,0,0)))
        if name=='Root': local[2,3]+=pose.get('_bob',0)
        if name in overrides: local=np.linalg.inv(parent) @ overrides[name]
        world=parent @ local
        if name in pose.get('_hands',{}):
            x,y,z=pose['_hands'][name]; target=np.array([x,z,y]); origin=world[:3,3]
            delta=target-origin; distance=np.linalg.norm(delta); direction=delta/max(.001,distance)
            distance=min(distance,.3+.272858-.001); target=origin+direction*distance
            along=(.3**2-.272858**2+distance**2)/(2*max(.001,distance)); height=math.sqrt(max(0,.3**2-along**2))
            outward=np.array([1. if name.endswith('R') else -1.,0,0]); outward-=direction*np.dot(outward,direction)
            if np.linalg.norm(outward)<.01: outward=np.array([0.,-1,0])
            outward/=np.linalg.norm(outward); elbow=origin+direction*along+outward*height
            world=frame(elbow-origin,origin); local=np.linalg.inv(parent) @ world
            overrides['Hand_'+name[-1]]=frame(target-elbow,elbow)
        result[name]=local
        for child in node['children']: visit(child,world)
    visit(rig,np.eye(4)); return result

def blend(a,b,u):
    out={}
    for key in a.keys() | b.keys():
        if key=='_hands':
            out[key]={j:tuple(np.array(a.get(key,{}).get(j,(.35 if j.endswith('R') else -.35,1.05,.2)))*(1-u)+np.array(b.get(key,{}).get(j,(.35 if j.endswith('R') else -.35,1.05,.2)))*u) for j in a.get(key,{}) | b.get(key,{})}
        elif key=='_bob': out[key]=a.get(key,0)*(1-u)+b.get(key,0)*u
        else: out[key]=tuple(np.array(a.get(key,(0,0,0)))*(1-u)+np.array(b.get(key,(0,0,0)))*u)
    return out

def export(name, keys):
    keys=[(round(t,4),pose) for t,pose in keys]
    end=keys[-1][0]; times=sorted({round(i*.05,4) for i in range(int(round(end/.05))+1)} | {t for t,_ in keys})
    frames=[]
    for t in times:
        i=next((i for i in range(len(keys)-1) if keys[i][0]<=t<keys[i+1][0]),len(keys)-2)
        a,pa=keys[i];b,pb=keys[i+1];u=min(1,max(0,(t-a)/(b-a)));frames.append(matrices(blend(pa,pb,u*u*(3-2*u))))
    channels=[{'name':j,'time':times,'transform':[[round(float(v),6) for v in f[j].flatten()] for f in frames]} for j in nodes]
    ROOT.mkdir(parents=True,exist_ok=True);(ROOT/(name+'.json')).write_text(json.dumps({'animation':channels},separators=(',',':'))+'\n')

# Species silhouettes and cadence, with planted feet during attacks. No root-motion lunge.
profiles = {
    'orc_warrior': (7, 1.0, 20), 'rotting_zombie': (18, 1.25, 16),
    'gargoyle_striker': (12, .8, 23), 'decayed_skeleton': (5, 1.1, 18),
    'kobold_scout': (8, .7, 24), 'death_knight': (2, 1.15, 15),
}
for name,(hunch,period,stride) in profiles.items():
    idle={'Chest':(hunch,0,0),'Head':(-hunch*.7,0,0),'Arm_R':(-8,0,-5),'Arm_L':(-8,0,5)}
    breathe={**idle,'Chest':(hunch+1.5,0,0),'_bob':.008}
    export(name+'_idle',[(0,idle),(1.5,breathe),(3,idle)])
    poses=[]
    for t,phase in [(0,1),(period*.25,0),(period*.5,-1),(period*.75,0),(period,1)]:
        p={**idle,'Thigh_R':(stride*phase,0,0),'Thigh_L':(-stride*phase,0,0),
           'Leg_R':(-max(0,-phase)*stride*.7,0,0),'Leg_L':(-max(0,phase)*stride*.7,0,0),
           'Arm_R':(-8-stride*.55*phase,0,-5),'Arm_L':(-8+stride*.55*phase,0,5),
           'Torso':(0,0,phase*2),'_bob':.018*(1-abs(phase))}
        poses.append((t,p))
    export(name+'_walk',poses)
    # One readable wind-up, one fast contact stroke, then a visible recovery.
    duration,windup,hit=(1.15,.5,.65) if name=='orc_warrior' else (1.1,.45,.6) if name=='death_knight' else (.9,.35,.5) if name=='rotting_zombie' else (.75,.25,.4)
    ready={**idle,'_hands':{'Arm_R':(.38,1.15,.22),'Arm_L':(-.38,1.2,.2)}}
    if name=='orc_warrior':
        load={**idle,'Chest':(-8,0,-12),'_hands':{'Arm_R':(.48,1.92,-.1),'Arm_L':(-.38,1.4,.3)}}
        strike={**idle,'Chest':(16,0,12),'_hands':{'Arm_R':(.12,1.08,.7),'Arm_L':(-.4,1.25,.2)}}
    elif name=='death_knight':
        load={**idle,'Chest':(2,0,-20),'_hands':{'Arm_R':(.55,1.5,-.1),'Arm_L':(-.4,1.3,.3)}}
        strike={**idle,'Chest':(6,0,18),'_hands':{'Arm_R':(-.05,1.3,.7),'Arm_L':(-.4,1.3,.3)}}
    elif name=='gargoyle_striker':
        load={**idle,'Chest':(2,0,-18),'_hands':{'Arm_R':(.65,1.5,.05),'Arm_L':(-.6,1.4,.05)}}
        strike={**idle,'Chest':(20,0,15),'_hands':{'Arm_R':(-.1,1.2,.65),'Arm_L':(-.4,1.25,.3)}}
    else:
        load={**idle,'Chest':(hunch-4,0,-8),'_hands':{'Arm_R':(.48,1.25,.05),'Arm_L':(-.4,1.2,.1)}}
        strike={**idle,'Chest':(hunch+8,0,8),'_hands':{'Arm_R':(.2,1.25,.7),'Arm_L':(-.4,1.2,.1)}}
    export(name+'_attack',[(0,ready),(windup,load),(hit,strike),(hit+.1,strike),(duration,ready)])
