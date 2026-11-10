"""Author the paired backstab clips. Usage: python tools/generate_backstab_animations.py EPIC_FIGHT_JAR
Requires numpy. Writes only the three backstab clips; existing combat animations are untouched.
"""
from pathlib import Path
import json,zipfile,math,sys,numpy as np
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/riftfall/animmodels/animations/biped'
with zipfile.ZipFile(sys.argv[1]) as z:rig=json.loads(z.read('assets/epicfight/animmodels/entity/biped.json'))['armature']['hierarchy'][0]
nodes={}
def index(n):
 nodes[n['name']]=n
 for c in n['children']:index(c)
index(rig)
def rotation(angles):
 x,y,z=map(math.radians,angles);c,s=math.cos,math.sin
 return np.array([[1,0,0,0],[0,c(x),-s(x),0],[0,s(x),c(x),0],[0,0,0,1]])@np.array([[c(y),0,s(y),0],[0,1,0,0],[-s(y),0,c(y),0],[0,0,0,1]])@np.array([[c(z),-s(z),0,0],[s(z),c(z),0,0],[0,0,1,0],[0,0,0,1]])
def frame(direction,position):
 y=direction/np.linalg.norm(direction);x=np.array([1.,0,0]);x-=y*np.dot(x,y)
 if np.linalg.norm(x)<.01:x=np.cross(y,[0,1,0])
 x/=np.linalg.norm(x);z=np.cross(x,y)
 m=np.eye(4);m[:3,:3]=np.column_stack([x,y,z]);m[:3,3]=position;return m
# Arm IK places the thrusting hand and the victim's floor-contacting palms.
# Inputs are unscaled body coordinates: right, height, forward.
def matrices(pose):
 result={};overrides={};globals={}
 def visit(n,parent):
  name=n['name'];bind=np.array(n['transform']).reshape(4,4)
  local=bind@rotation(pose.get(name,(0,0,0)))
  if name=='Root':
   local[2,3]+=pose.get('_sink',0);local[1,3]+=pose.get('_forward',0)
  if name in overrides:local=np.linalg.inv(parent)@overrides[name]
  g=parent@local
  if name in ['Arm_R','Arm_L'] and name in pose.get('_hands',{}):
   tx,ty,tz=pose['_hands'][name];target=np.array([tx,tz,ty]);origin=g[:3,3]
   delta=target-origin;dist=np.linalg.norm(delta);direction=delta/dist
   dist=min(dist,.3+.272858-.001);target=origin+direction*dist
   along=(.3**2-.272858**2+dist**2)/(2*dist);height=math.sqrt(max(0,.3**2-along**2))
   outward=np.array([1. if name.endswith('R') else -1.,0,0]);outward-=direction*np.dot(outward,direction)
   if np.linalg.norm(outward)<.01:outward=np.array([0.,-1,0])
   outward/=np.linalg.norm(outward);elbow=origin+direction*along+outward*height
   g=frame(elbow-origin,origin);local=np.linalg.inv(parent)@g
   overrides['Hand_'+name[-1]]=frame(target-elbow,elbow)
  if name=='Tool_R' and '_blade' in pose:
   # Orient the wrist so the vanilla sword's diagonal blade follows a safe world-space path.
   # Includes Epic Fight's -90 degree item correction and vanilla handheld display transform.
   bx,by,bz=pose['_blade'];direction=np.array([bx,bz,by]);direction/=np.linalg.norm(direction)
   x=np.array(pose.get('_cut_normal',(1.,0,0)));x-=direction*np.dot(x,direction);x/=np.linalg.norm(x)
   perpendicular=np.cross(x,direction)
   bladeY=-math.sin(math.radians(10));bladeZ=-math.cos(math.radians(10))
   y=bladeY*direction-bladeZ*perpendicular;z=bladeZ*direction+bladeY*perpendicular
   orientation=np.column_stack([x,y,z])
   if '_cut_normal' in pose:
    # Roll around the actual grip, not the item origin, so the hilt stays in the fist.
    oldX=np.array([1.,0,0]);oldX-=direction*np.dot(oldX,direction);oldX/=np.linalg.norm(oldX)
    oldPerp=np.cross(oldX,direction)
    oldOrientation=np.column_stack([oldX,bladeY*direction-bladeZ*oldPerp,bladeZ*direction+bladeY*oldPerp])
    display=rotation((-90,0,0));display[:3,3]=[0,0,-.13]
    offset=np.eye(4);offset[:3,3]=[0,.25,.03125]
    grip=(display@offset@rotation((0,-90,55))@np.diag([.85,.85,.85,1])@np.array([3.5/16-.5,3.5/16-.5,0,1]))[:3]
    g[:3,3]+=oldOrientation@grip-orientation@grip
   g[:3,:3]=orientation;local=np.linalg.inv(parent)@g
  result[name]=local;globals[name]=g
  for child in n['children']:visit(child,g)
 visit(rig,np.eye(4));return result,globals

def interpolate(a,b,u):
 result={}
 for key in a.keys()|b.keys():
  if key=='_hands':
   result[key]={joint:tuple(np.array(a.get(key,{}).get(joint,(.3,.95,0)))*(1-u)+np.array(b.get(key,{}).get(joint,(.3,.95,0)))*u) for joint in a.get(key,{}).keys()|b.get(key,{}).keys()}
  elif key.startswith('_') and key not in ('_blade','_cut_normal'):
   result[key]=a.get(key,0)*(1-u)+b.get(key,0)*u
  else:
   default=(0,0,1) if key=='_blade' else (1,0,0) if key=='_cut_normal' else (0,0,0)
   av=np.array(a.get(key,default));bv=np.array(b.get(key,default));result[key]=tuple(av*(1-u)+bv*u)
 return result

def export(name,keys):
 times=sorted(set([round(i*.05,4) for i in range(round(keys[-1][0]/.05)+1)]+[t for t,p in keys]));frames=[]
 for t in times:
  i=next((i for i in range(len(keys)-1) if keys[i][0]<=t<keys[i+1][0]),len(keys)-2)
  a,pa=keys[i];b,pb=keys[i+1];u=min(1,max(0,(t-a)/(b-a)));u=u*u*(3-2*u)
  frames.append(matrices(interpolate(pa,pb,u))[0])
 channels=[{'name':joint,'time':times,'transform':[[round(float(v),6) for v in frame[joint].flatten()] for frame in frames]} for joint in nodes]
 (root/(name+'.json')).write_text(json.dumps({'animation':channels},separators=(',',':'))+'\n')

caught={'Chest':(9,0,0),'Head':(-9,0,0),'Arm_R':(-12,0,-12),'Arm_L':(-12,0,12),'Hand_R':(-25,0,0),'Hand_L':(-25,0,0)}
export('backstab_caught',[(0,{}),(.18,caught),(1.8,caught)])
knees={'Root':(-12,0,0),'_sink':-.43,'Torso':(-16,0,0),'Chest':(-12,0,0),'Head':(15,0,0),
 'Thigh_R':(65,0,-4),'Leg_R':(-145,0,0),'Thigh_L':(65,0,4),'Leg_L':(-145,0,0),
 '_hands':{'Arm_R':(.45,.5,.55),'Arm_L':(-.45,.5,.55)}}
prone={'Root':(-90,0,0),'_sink':-.46,'_forward':.25,'Head':(0,0,-4),
 '_hands':{'Arm_R':(.5,.13,1.25),'Arm_L':(-.5,.13,1.25)},
 'Thigh_R':(20,0,-6),'Thigh_L':(20,0,6),'Leg_R':(-20,0,0),'Leg_L':(-20,0,0)}
push={**prone,'Root':(-72,0,0),'_sink':-.38,'_hands':{'Arm_R':(.5,.13,.85),'Arm_L':(-.5,.13,.85)},'Head':(-12,0,0)}
allfours={**knees,'Root':(-60,0,0),'_sink':-.33,'_forward':.16,'Torso':(-10,0,0),'Chest':(0,0,0),'Head':(16,0,0),
 'Thigh_R':(90,0,-4),'Thigh_L':(90,0,4),'Leg_R':(-120,0,0),'Leg_L':(-120,0,0),
 '_hands':{'Arm_R':(.4,.13,.75),'Arm_L':(-.4,.13,.75)}}
rise={**knees,'Root':(-5,0,0),'_sink':-.08,'_forward':.06,'Torso':(-8,0,0),'Chest':(0,0,0),'Head':(4,0,0),
 'Thigh_R':(30,0,0),'Leg_R':(-60,0,0),'Thigh_L':(65,0,0),'Leg_L':(-105,0,0),
 '_hands':{'Arm_R':(.35,.8,.3),'Arm_L':(-.35,.7,.25)}}
export('backstab_fall',[(0,caught),(.12,{**caught,'Chest':(14,0,0)}),(.3,{**caught,'Chest':(-22,0,0),'Head':(16,0,0)}),
 (.68,knees),(.86,{**knees,'Root':(-38,0,0),'_sink':-.35}),
 (1.14,prone),(1.26,{**prone,'Root':(-85,0,0),'_sink':-.43}),(1.45,prone),(2.0,prone),
 (2.25,{**prone,'_hands':{'Arm_R':(.5,.13,.85),'Arm_L':(-.5,.13,.85)}}),(2.65,push),(3.0,allfours),(3.35,knees),(3.8,rise),(4.12,{'Root':(-3,0,0),'_sink':-.025,'Chest':(-5,0,0),'Arm_R':(-8,0,-7),'Arm_L':(-8,0,7)}),(4.6,{})])
ready={'_blade':(.05,.05,1),'_hands':{'Arm_R':(.34,1.24,.15)},'Chest':(-5,-12,0),'Arm_L':(38,0,22),'Hand_L':(62,0,0)}
windup={**ready,'_hands':{'Arm_R':(.36,1.27,-.04)},'Chest':(-8,-24,0),'_sink':-.035,'Thigh_R':(12,0,0),'Leg_R':(-22,0,0)}
thrust={**ready,'_hands':{'Arm_R':(.18,1.4,.58)},'Chest':(-17,16,0),'_sink':-.06,'Thigh_L':(18,0,0),'Leg_L':(-18,0,0),'Head':(9,0,0)}
export('goblin_shield_backstab',[(0,ready),(.2,windup),(.46,windup),(.62,thrust),(.8,thrust),(.98,ready),(1.3,ready),(1.8,{})])
print('Generated backstab strike, caught, and 4.6-second fall/recovery clips')
