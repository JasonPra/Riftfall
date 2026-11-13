package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;

/** Immutable priority-flood drainage: route through low passes, fill depressions to their spillway. */
public final class GeyserDrainage {
    static final int STEP=16;
    private static final Map<GeyserRegion,GeyserDrainage> CACHE=new LinkedHashMap<>(16,.75f,true);
    public static synchronized GeyserDrainage of(GeyserRegion field){
        var result=CACHE.get(field);
        if(result==null){result=new GeyserDrainage(field);CACHE.put(field,result);if(CACHE.size()>8)CACHE.remove(CACHE.keySet().iterator().next());}
        return result;
    }
    public record Point(double x,double z,int water){}
    public record Reach(double distance,double x,double z,int water,double flowX,double flowZ,int segment){}
    private final GeyserRegion field;
    private final int size,minX,minZ;
    private final int[] parent,lake;
    private final List<Point> path;
    private final Map<Long,List<Integer>> segments;
    private final boolean north,external,swallow;
    public List<Point> path(){return path;}
    public Point source(){return path.getFirst();}
    public Point mouth(){return path.getLast();}
    public boolean north(){return north;}
    public boolean external(){return external&&!swallow;}
    public boolean underground(){return swallow;}
    private static long tile(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private double x(int index){return minX+(index%size)*STEP;}
    private double z(int index){return minZ+(index/size)*STEP;}
    private int index(double x,double z){return (int)Math.round((z-minZ)/STEP)*size+(int)Math.round((x-minX)/STEP);}
    private GeyserDrainage(GeyserRegion g){
        field=g;int half=(int)Math.ceil((GeyserShape.radius(g)+48)/STEP);size=half*2+1;
        minX=(int)g.centerX()-half*STEP;minZ=(int)g.centerZ()-half*STEP;
        long seed=GeyserLayout.mix(g.centerChunkX()*731L^g.centerChunkZ()*997L);
        double northHeight=GeyserTerrain.ground(g,g.centerX(),g.centerZ()-GeyserShape.radius(g)*.68);
        double southHeight=GeyserTerrain.ground(g,g.centerX(),g.centerZ()+GeyserShape.radius(g)*.68);
        boolean preferNorth=northHeight<southHeight;boolean preferExternal=(seed&(preferNorth?1:2))!=0;
        double end=g.centerZ()+(preferNorth?-1:1)*GeyserShape.radius(g)*.68;
        int goal=index(g.centerX(),end),count=size*size;
        var raw=new double[count];double[] spill;
        lake=new int[count];Arrays.fill(lake,Integer.MIN_VALUE);
        for(int i=0;i<count;i++)raw[i]=GeyserShape.distance(g,x(i),z(i))<=0?GeyserTerrain.ground(g,x(i),z(i))-1:Double.POSITIVE_INFINITY;
        while(!Double.isFinite(raw[goal]))goal+=preferNorth?size:-size;
        var outlets=new ArrayList<Integer>();
        for(int i=0;i<count;i++)if(Double.isFinite(raw[i])){
            int ix=i%size,iz=i/size;
            if(ix==0||iz==0||ix==size-1||iz==size-1||!Double.isFinite(raw[i-1])||!Double.isFinite(raw[i+1])||!Double.isFinite(raw[i-size])||!Double.isFinite(raw[i+size]))outlets.add(i);
        }
        var boundary=new HashSet<>(outlets);int receiver=-1;double lowestReceiver=Double.POSITIVE_INFINITY;
        if(!preferExternal)for(int i=size+1;i<count-size-1;i++){
            if(!Double.isFinite(raw[i])||Math.hypot(x(i)-g.centerX(),z(i)-g.centerZ())>GeyserShape.radius(g)*.75||raw[i]>=lowestReceiver)continue;
            boolean minimum=true;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)if(raw[i+dz*size+dx]<raw[i])minimum=false;
            if(minimum){receiver=i;lowestReceiver=raw[i];}
        }
        if(receiver>=0)outlets.add(receiver);
        var solved=DrainageGrid.solve(size,raw,outlets.stream().mapToInt(Integer::intValue).toArray(),62);spill=solved.spill();parent=solved.parent();
        int source=goal;double highest=-999;
        for(int i=0;i<count;i++)if(Math.hypot(x(i)-g.centerX(),z(i)-g.centerZ())<GeyserShape.radius(g)*.35&&Double.isFinite(spill[i])&&raw[i]>highest){highest=raw[i];source=i;}
        var ids=new ArrayList<Integer>();for(int i=source;i>=0;i=parent[i]){ids.add(i);if(ids.size()>count)throw new IllegalStateException("Cyclic drainage");}
        // Some courses encounter a swallow hole and continue through the cave network instead of the rim.
        boolean candidate=(seed&7)==0&&ids.size()>16;
        if(candidate){int stop=Math.max(8,ids.size()/2);ids.subList(stop+1,ids.size()).clear();}
        if(candidate){
            // Reroot the flat-water drainage tree at the swallow hole, including lake branches.
            int previous=-1;
            for(int i=ids.getLast();i>=0;){int next=parent[i];parent[i]=previous;previous=i;i=next;}
        }
        swallow=candidate;external=boundary.contains(ids.getLast())&&!swallow;north=z(ids.getLast())<g.centerZ();
        // Only flood actual depressions connected to this watercourse, rather than cutting ellipses into hills.
        var seen=new boolean[count];var pending=new ArrayDeque<Integer>();
        for(int id:ids){
            int level=(int)Math.floor(spill[id]);if(raw[id]+1>=level||seen[id])continue;
            pending.add(id);seen[id]=true;
            while(!pending.isEmpty()){
                int i=pending.removeFirst();lake[i]=level;int ix=i%size,iz=i/size;
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                    int nx=ix+d[0],nz=iz+d[1];if(nx<0||nx>=size||nz<0||nz>=size)continue;
                    int j=nz*size+nx;
                    if(!seen[j]&&raw[j]+1<level&&spill[j]<level+1){seen[j]=true;pending.add(j);}
                }
            }
        }
        var points=new ArrayList<Point>();
        for(int n=0;n<ids.size();n++){
            int id=ids.get(n);double px=x(id),pz=z(id);
            if(n>0&&n<ids.size()-1){px=(x(ids.get(n-1))+px*2+x(ids.get(n+1)))/4;pz=(z(ids.get(n-1))+pz*2+z(ids.get(n+1)))/4;}
            points.add(new Point(px,pz,(int)Math.floor(spill[id])));
        }
        if(external){
            var last=points.getLast();double dx=last.x-g.centerX(),dz=last.z-g.centerZ(),length=Math.hypot(dx,dz);
            double low=0,high=64;
            for(int i=0;i<24;i++){double mid=(low+high)/2;if(GeyserShape.distance(g,last.x+dx/length*mid,last.z+dz/length*mid)<0)low=mid;else high=mid;}
            points.add(new Point(last.x+dx/length*(low+high)/2,last.z+dz/length*(low+high)/2,last.water));
        }
        path=List.copyOf(points);var buckets=new HashMap<Long,List<Integer>>();
        for(int i=0;i<path.size()-1;i++){
            var a=path.get(i);var b=path.get(i+1);
            for(int tx=Math.floorDiv((int)Math.floor(Math.min(a.x,b.x)-64),64);tx<=Math.floorDiv((int)Math.ceil(Math.max(a.x,b.x)+64),64);tx++)
                for(int tz=Math.floorDiv((int)Math.floor(Math.min(a.z,b.z)-64),64);tz<=Math.floorDiv((int)Math.ceil(Math.max(a.z,b.z)+64),64);tz++)buckets.computeIfAbsent(tile(tx,tz),k->new ArrayList<>()).add(i);
        }
        var immutable=new HashMap<Long,List<Integer>>();buckets.forEach((k,v)->immutable.put(k,List.copyOf(v)));segments=Map.copyOf(immutable);
    }
    public Reach nearest(double wx,double wz){
        var candidates=segments.get(tile(Math.floorDiv((int)Math.floor(wx),64),Math.floorDiv((int)Math.floor(wz),64)));
        if(candidates==null)return new Reach(Double.POSITIVE_INFINITY,0,0,0,0,0,-1);
        Reach best=null;
        for(int i:candidates){
            var a=path.get(i);var b=path.get(i+1);double dx=b.x-a.x,dz=b.z-a.z,l=dx*dx+dz*dz;
            double t=l==0?0:Math.clamp(((wx-a.x)*dx+(wz-a.z)*dz)/l,0,1),px=a.x+dx*t,pz=a.z+dz*t,d=Math.hypot(wx-px,wz-pz);
            if(best==null||d<best.distance){double n=Math.sqrt(l);best=new Reach(d,px,pz,(int)Math.floor(a.water+(b.water-a.water)*t),n==0?0:dx/n,n==0?0:dz/n,i);}
        }
        return best;
    }
    /** Compatibility for outlet searches; the full sampler uses segments and supports turns/backtracking. */
    public double centerAt(double wz){
        Point closest=source();double distance=Math.abs(wz-closest.z);double result=closest.x;
        for(int i=0;i<path.size()-1;i++){
            var a=path.get(i);var b=path.get(i+1);
            if(wz>=Math.min(a.z,b.z)&&wz<=Math.max(a.z,b.z)&&a.z!=b.z)return a.x+(b.x-a.x)*(wz-a.z)/(b.z-a.z);
            if(Math.abs(wz-b.z)<distance){distance=Math.abs(wz-b.z);result=b.x;}
        }
        return result;
    }
    public int lakeAt(int wx,int wz){
        int gx=(int)Math.round((wx-minX)/(double)STEP),gz=(int)Math.round((wz-minZ)/(double)STEP),level=Integer.MIN_VALUE;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int a=gx+dx,b=gz+dz;if(a<0||a>=size||b<0||b>=size)continue;
            int candidate=lake[b*size+a];if(candidate>level&&GeyserTerrain.ground(field,wx,wz)<candidate+3)level=candidate;
        }
        return level;
    }
    public RiverCurrentField.Flow flow(int wx,int wz){
        var target=mouth();double tx=target.x-wx,tz=target.z-wz,distance=Math.hypot(tx,tz);
        if(!external()&&distance<48)return distance<1?new RiverCurrentField.Flow(0,0):new RiverCurrentField.Flow(tx/distance,tz/distance);
        var r=nearest(wx,wz);if(r.distance<20)return new RiverCurrentField.Flow(r.flowX,r.flowZ);
        int gx=(int)Math.round((wx-minX)/(double)STEP),gz=(int)Math.round((wz-minZ)/(double)STEP);
        if(gx<0||gx>=size||gz<0||gz>=size)return new RiverCurrentField.Flow(0,0);
        int id=gz*size+gx,next=parent[id];if(next<0)return new RiverCurrentField.Flow(0,0);
        double dx=x(next)-x(id),dz=z(next)-z(id),n=Math.hypot(dx,dz);
        return new RiverCurrentField.Flow(dx/n,dz/n);
    }
    private GeyserDrainage() {throw new AssertionError();}
}
