"""Author the orc mesh and clips against Epic Fight's standard biped bind pose.
Usage: python tools/generate_orc_warrior.py EPIC_FIGHT_JAR PLAYER_SYSTEM_JAR
NumPy is required. Only UV coordinates are read from Player System; its geometry and clips are not copied.
"""
from pathlib import Path
import copy,json,math,sys,zipfile
import numpy as np
BASE=Path(__file__).resolve().parents[1]/'src/main/resources/assets/riftfall/animmodels'
with zipfile.ZipFile(sys.argv[1]) as z:
    rig=copy.deepcopy(json.loads(z.read('assets/epicfight/animmodels/entity/biped.json'))['armature'])
with zipfile.ZipFile(sys.argv[2]) as z:
    source=json.loads(z.read('assets/player_system/geo/mob/orc_warrior.geo.json'))['minecraft:geometry'][0]
bones={b['name']:b for b in source['bones']}
# Full rest-pose frame reconstruction retains Epic Fight's local rotation conventions.
targets={'Root':(0,0,.96),'Thigh_R':(.15,0,.94),'Leg_R':(.15,0,.49),'Knee_R':(.15,0,.49),
'Thigh_L':(-.15,0,.94),'Leg_L':(-.15,0,.49),'Knee_L':(-.15,0,.49),'Torso':(0,0,1.0),'Chest':(0,0,1.38),
'Head':(0,0,1.80),'Shoulder_R':(0,0,1.76),'Shoulder_L':(0,0,1.76),
'Arm_R':(.43,0,1.70),'Arm_L':(-.43,0,1.70),'Hand_R':(.43,0,1.32),'Hand_L':(-.43,0,1.32),
'Elbow_R':(.43,0,1.32),'Elbow_L':(-.43,0,1.32),'Tool_R':(.43,-.03,.98),'Tool_L':(-.43,-.03,.98)}
def rebuild(node,old_parent,new_parent):
    old=old_parent@np.array(node['transform']).reshape(4,4)
    world=old.copy();world[:3,3]=targets[node['name']]
    node['transform']=[round(float(v),7) for v in (np.linalg.inv(new_parent)@world).flatten()]
    for child in node['children']:rebuild(child,old,world)
rebuild(rig['hierarchy'][0],np.eye(4),np.eye(4))
old_rig=copy.deepcopy(rig)
with zipfile.ZipFile(sys.argv[1]) as z:
    rig=copy.deepcopy(json.loads(z.read('assets/epicfight/animmodels/entity/biped.json'))['armature'])
def world_frames(armature):
    frames={}
    def visit(node,parent):
        world=parent@np.array(node['transform']).reshape(4,4);frames[node['name']]=world
        for child in node['children']:visit(child,world)
    visit(armature['hierarchy'][0],np.eye(4));return frames
old_frames=world_frames(old_rig);new_frames=world_frames(rig)
# Preserve the authored silhouette around each joint in the exact standard biped bind pose.
bind={name:new_frames[name]@np.linalg.inv(old_frames[name]) for name in new_frames}

positions=[];normals=[];uvs=[];counts=[];indices=[]
parts={k:[] for k in ['head','torso','rightArm','leftArm','rightLeg','leftLeg','hat','jacket','rightSleeve','leftSleeve','rightPants','leftPants']}
faces=['north','south','east','west','up','down'];corners=[[0,1,3,2],[5,4,6,7],[1,5,7,3],[4,0,2,6],[2,3,7,6],[4,5,1,0]]
joints={n:i for i,n in enumerate(rig['joints'])}
def cube(part,joint,origin,size,uvbone,uvindex=0):
    mapping=bones[uvbone]['cubes'][uvindex]['uv']
    vs=[np.array([origin[0]+(size[0] if i&1 else 0),origin[1]+(size[1] if i&2 else 0),origin[2]+(size[2] if i&4 else 0)]) for i in range(8)]
    for f,order in zip(faces,corners):
        if f not in mapping:continue
        uv=mapping[f];u,v=uv['uv'];du,dv=uv['uv_size'];tw=source['description']['texture_width'];th=source['description']['texture_height']
        tex=[(u,v+dv),(u+du,v+dv),(u+du,v),(u,v)] if f not in ('up','down') else [(u,v),(u+du,v),(u+du,v+dv),(u,v+dv)]
        base=len(positions)//3;converted=[]
        for i,corner in enumerate(order):
            x,y,z=vs[corner];vertex=(bind[joint]@np.array([-x,-z,y,1.]))[:3];converted.append(vertex);positions.extend(vertex.tolist());uvs.extend([tex[i][0]/tw,tex[i][1]/th]);counts.append(1);indices.extend([joints[joint],0])
        normal=np.cross(converted[1]-converted[0],converted[2]-converted[0]);normal/=np.linalg.norm(normal);ni=len(normals)//3;normals.extend(normal.tolist())
        for i in [0,1,2,0,2,3]:parts[part].extend([base+i,base+i,ni])
# Broad layered torso, independent upper/lower limbs and wrists; every cube is attached explicitly.
cube('torso','Torso',(-.29,.91,-.18),(.58,.39,.36),'body')
cube('torso','Chest',(-.34,1.30,-.20),(.68,.46,.40),'body')
cube('torso','Torso',(-.33,.92,-.205),(.66,.12,.41),'body',1)
cube('torso','Chest',(-.23,1.38,-.235),(.46,.28,.04),'body',2)
cube('head','Head',(-.20,1.78,-.20),(.40,.40,.39),'head')
cube('head','Head',(-.18,1.78,-.24),(.36,.11,.07),'head',1)
for side in [-1,1]:
    suffix='R' if side<0 else 'L';part='right' if side<0 else 'left';arm='arm_r' if side<0 else 'arm_l';leg='leg_r' if side<0 else 'leg_l';x=side*.43
    cube(part+'Arm','Arm_'+suffix,(x-.12,1.32,-.12),(.24,.38,.24),arm)
    cube(part+'Arm','Hand_'+suffix,(x-.13,1.04,-.13),(.26,.28,.26),arm,2)
    cube(part+'Arm','Hand_'+suffix,(x-.12,.96,-.14),(.24,.10,.27),arm)
    cube(part+'Arm','Arm_'+suffix,(x-.16,1.60,-.17),(.32,.15,.34),arm,1)
    x=side*.15
    cube(part+'Leg','Thigh_'+suffix,(x-.13,.49,-.15),(.26,.45,.30),leg)
    cube(part+'Leg','Leg_'+suffix,(x-.12,.12,-.14),(.24,.37,.28),leg)
    cube(part+'Leg','Leg_'+suffix,(x-.15,.03,-.23),(.30,.17,.40),leg,2)
    cube(part+'Leg','Leg_'+suffix,(x-.14,.43,-.18),(.28,.11,.08),leg,1)
    cube('head','Head',(side*.105-.025,1.78,-.265),(.05,.13,.05),'head',2 if side>0 else 3)
    cube('head','Head',(side*.10-.05,2.015,-.23),(.10,.055,.04),'head',4 if side>0 else 5)
cube('head','Head',(-.19,2.09,-.218),(.38,.055,.04),'head',6)
cube('head','Head',(-.035,2.15,-.12),(.07,.09,.27),'head',7)
# The axe is an actual equipped item rendered at the standard Tool_R attachment.
def buffer(a,stride):return {'array':a,'stride':stride,'count':len(a)//stride}
vertices={'positions':buffer(positions,3),'normals':buffer(normals,3),'uvs':buffer(uvs,2),'weights':buffer([1.],1),'vcounts':buffer(counts,1),'vindices':buffer(indices,1),'parts':{k:buffer(v,3) for k,v in parts.items()}}
(BASE/'entity').mkdir(parents=True,exist_ok=True)
(BASE/'entity/orc_warrior.json').write_text(json.dumps({'armature':rig,'vertices':vertices},separators=(',',':'))+'\n')
# Reuse the exporter, but author all poses against this rig and its actual segment lengths.
helper=(Path(__file__).parent/'generate_player_system_animations.py').read_text().split('# Species silhouettes')[0]
# Use the standard biped segment lengths for spell-pose IK.
ns={'__file__':str(Path(__file__).parent/'generate_player_system_animations.py')};exec(helper,ns)
ns['ROOT']=BASE/'animations/orc_warrior';ns['rig']=rig['hierarchy'][0];ns['nodes']={};ns['index'](ns['rig']);export=ns['export']
idle={'Chest':(8,0,0),'Head':(-6,0,0),'Arm_R':(-8,0,-8),'Arm_L':(-8,0,8)}
export('idle',[(0,idle),(1.5,{**idle,'Chest':(10,0,0),'_bob':.008}),(3,idle)])
walk=[]
for t,p in [(0,1),(.25,0),(.5,-1),(.75,0),(1,1)]:
    walk.append((t,{**idle,'Thigh_R':(22*p,0,0),'Thigh_L':(-22*p,0,0),'Leg_R':(-max(0,-p)*22,0,0),'Leg_L':(-max(0,p)*22,0,0),'Arm_R':(-8-12*p,0,-8),'Arm_L':(-8+12*p,0,8),'Torso':(0,0,p*2),'_bob':.015*(1-abs(p))}))
export('walk',walk)
ready={**idle,'_hands':{'Arm_R':(.43,1.28,.22),'Arm_L':(-.43,1.35,.20)}}
overhead={**idle,'Chest':(-10,0,-12),'_hands':{'Arm_R':(.46,2.05,-.08),'Arm_L':(-.4,1.5,.2)}}
strike={**idle,'Chest':(20,0,13),'_hands':{'Arm_R':(.15,1.12,.72),'Arm_L':(-.40,1.35,.2)}}
export('spell_windup',[(0,ready),(.85,overhead),(2,overhead)])
export('spell_slam',[(0,overhead),(.16,strike),(.35,{**strike,'Chest':(27,0,10)}),(.8,idle)])
export('hurt',[(0,idle),(.12,{**idle,'Chest':(-20,0,4),'Head':(15,0,0)}),(.45,idle)])
fallen={**idle,'Root':(88,0,5),'_bob':-.80,'Arm_R':(-30,0,-25),'Arm_L':(-35,0,30),'Thigh_R':(12,0,0),'Thigh_L':(-8,0,0)}
export('death',[(0,idle),(.25,{**idle,'Chest':(-18,0,0),'_bob':-.10}),(.7,{**fallen,'Root':(60,0,5),'_bob':-.45}),(1.15,fallen),(1.4,fallen)])
export('knockdown',[(0,idle),(.35,fallen),(.7,fallen),(1.4,idle)])
air={**idle,'Thigh_R':(-20,0,0),'Thigh_L':(-15,0,0),'Leg_R':(-25,0,0),'Leg_L':(-30,0,0)}
export('air',[(0,air),(1,air)])
print('Authored orc biped mesh and eight clips:',BASE)
