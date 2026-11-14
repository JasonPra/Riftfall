"""Original quadruped animation clips for Epic Fight and the Ry vanilla-model pose bridge."""
import json,math
from pathlib import Path
P=Path(__file__).resolve().parents[2]/'src/main/resources/assets/riftfall/animmodels'
# Binding positions are in metres; clip rotations are local, with no root-motion teleporting.
bones={'Root':(0,0,0),'Head':(0,.66,.44),'Jaw':(0,.55,.5),'Chest':(0,.63,.19),'Body':(0,.63,-.13),'FrontR':(.16,.5,.25),'FrontL':(-.03,.5,.25),'BackR':(.16,.5,-.44),'BackL':(-.03,.5,-.44),'Tail':(0,.75,-.5)}
def matrix(pos=(0,0,0),rot=(0,0,0)):
 x,y,z=map(math.radians,rot);cx,sx,cy,sy,cz,sz=math.cos(x),math.sin(x),math.cos(y),math.sin(y),math.cos(z),math.sin(z)
 return [cy*cz,-cy*sz,sy,pos[0],cx*sz+sx*sy*cz,cx*cz-sx*sy*sz,-sx*cy,pos[1],sx*sz-cx*sy*cz,sx*cz+cx*sy*sz,cx*cy,pos[2],0,0,0,1]
root={'name':'Root','transform':matrix(),'children':[{'name':n,'transform':matrix(v),'children':[]} for n,v in bones.items() if n!='Root']}
(P/'entity').mkdir(parents=True,exist_ok=True)
(P/'entity/ry.json').write_text(json.dumps({'armature':{'joints':list(bones),'hierarchy':[root]}},separators=(',',':'))+'\n')
def pose(kind,t,duration):
 p={};s=math.sin(t/duration*math.pi);wave=math.sin(t/duration*math.pi*2)
 if kind in ('idle','walk','run'):p={'Tail':(0,wave*(6 if kind=='idle' else 12),0),'Chest':(s*1.5,0,0)}
 if kind=='cast_charge':p={'Head':(-25*s,0,0),'Jaw':(32*s,0,0),'Chest':(-7*s,0,0),'FrontR':(-10*s,0,-5*s),'FrontL':(-10*s,0,5*s),'Tail':(-16*s,0,0)}
 if kind=='cast_release':p={'Head':(18*s,0,0),'Jaw':(24*s,0,0),'Chest':(8*s,0,0),'FrontR':(10*s,0,0),'FrontL':(10*s,0,0),'Tail':(14*s,0,0)}
 if kind=='bite':p={'Head':(14*s,12*wave,0),'Jaw':(48*s,0,0),'Chest':(8*s,0,0),'FrontR':(-14*s,0,0),'FrontL':(8*s,0,0)}
 if kind=='hurt':p={'Head':(-12*s,0,10*s),'Chest':(-8*s,0,5*s)}
 if kind=='death':p={'Head':(0,0,20*t/duration)}
 return p
(P/'animations/ry').mkdir(parents=True,exist_ok=True)
for kind,duration in [('idle',2),('walk',1),('run',.6),('sit',2),('death',1),('bite',.7),('cast_charge',1),('cast_release',.45),('hurt',.4)]:
 times=[round(i*duration/20,4) for i in range(21)];channels=[]
 for name,bind in bones.items():
  channels.append({'name':name,'time':times,'transform':[matrix(bind,pose(kind,t,duration).get(name,(0,0,0))) for t in times]})
 (P/f'animations/ry/{kind}.json').write_text(json.dumps({'animation':channels},separators=(',',':'))+'\n')
