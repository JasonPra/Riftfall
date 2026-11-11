"""Exterior shell/core surface extraction for intergrown, axis-aligned prisms."""
import copy

def exterior_faces(elements):
 # Remove faces buried in another prism of the same layer. Keep shell/core
 # envelopes separate so the glow remains inside the smoky outer mineral.
 result=[];eps=1e-7
 for source_index,source in enumerate(elements):
  a=source['from'];b=source['to']
  for side,face in source['faces'].items():
   axis,positive={'west':(0,False),'east':(0,True),'down':(1,False),'up':(1,True),'north':(2,False),'south':(2,True)}[side]
   u,v=([0,2] if axis==1 else [2,1] if axis==0 else [0,1]);plane=b[axis] if positive else a[axis]
   pieces=[(a[u],a[v],b[u],b[v])]
   for other_index,other in enumerate(elements):
    if other is source or other['shade']!=source['shade']:continue
    lo=other['from'];hi=other['to']
    covers=(lo[axis]<=plane+eps and hi[axis]>plane+eps) if positive else (lo[axis]<plane-eps and hi[axis]>=plane-eps)
    # Give coincident outside surfaces one deterministic owner as well.
    if other_index<source_index:
     covers|=(abs(hi[axis]-plane)<=eps and lo[axis]<plane-eps) if positive else (abs(lo[axis]-plane)<=eps and hi[axis]>plane+eps)
    if not covers:continue
    remaining=[]
    for x0,y0,x1,y1 in pieces:
     l=max(x0,lo[u]);r=min(x1,hi[u]);t=max(y0,lo[v]);d=min(y1,hi[v])
     if l>=r-eps or t>=d-eps:remaining.append((x0,y0,x1,y1));continue
     for rect in [(x0,y0,l,y1),(r,y0,x1,y1),(l,y0,r,t),(l,d,r,y1)]:
      if rect[2]-rect[0]>eps and rect[3]-rect[1]>eps:remaining.append(rect)
    pieces=remaining
    if not pieces:break
   for x0,y0,x1,y1 in pieces:
    e=copy.deepcopy(source);lo=a.copy();hi=b.copy();lo[axis]=hi[axis]=plane;lo[u]=x0;hi[u]=x1;lo[v]=y0;hi[v]=y1
    f=copy.deepcopy(face);f.pop('cullface',None)
    uv=face.get('uv',[0,0,16,16]);fu0=(x0-a[u])/(b[u]-a[u]);fu1=(x1-a[u])/(b[u]-a[u]);fv0=(y0-a[v])/(b[v]-a[v]);fv1=(y1-a[v])/(b[v]-a[v])
    if v==1:fv0,fv1=1-fv1,1-fv0
    f['uv']=[uv[0]+fu0*(uv[2]-uv[0]),uv[1]+fv0*(uv[3]-uv[1]),uv[0]+fu1*(uv[2]-uv[0]),uv[1]+fv1*(uv[3]-uv[1])]
    e['from']=lo;e['to']=hi;e['faces']={side:f};result.append(e)
 return result

