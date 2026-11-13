package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;

/** A receiving pool, swallow shaft and descending reach joining actual cave nodes and deep water. */
public final class GeyserUndergroundRiver {
    private record Key(GeyserRegion field,long seed){}
    private static final Map<Key,GeyserUndergroundRiver> CACHE=new LinkedHashMap<>(16,.75f,true);
    public record Point(double x,double z,int water){}
    public record Cut(int floor,int roof,int water,boolean falling,boolean wet,double flowX,double flowZ){}
    private final List<Point> points;
    public static synchronized GeyserUndergroundRiver of(long seed,GeyserRegion field){
        var key=new Key(field,seed);var result=CACHE.get(key);
        if(result==null){result=new GeyserUndergroundRiver(seed,field);CACHE.put(key,result);if(CACHE.size()>16)CACHE.remove(CACHE.keySet().iterator().next());}
        return result;
    }
    private GeyserUndergroundRiver(long seed,GeyserRegion field){
        var route=GeyserDrainage.of(field);
        if(!route.underground()){points=List.of();return;}
        var mouth=route.mouth();int cx=Math.floorDiv((int)mouth.x(),GeyserCaveShape.SPACING),cz=Math.floorDiv((int)mouth.z(),GeyserCaveShape.SPACING);
        var upper=GeyserCaveShape.node(seed,cx,0,cz);var lower=GeyserCaveShape.node(seed,cx+1,-1,cz+1);
        int upperWater=Math.min(mouth.water()-12,(int)upper.y());
        int lowerWater=Math.min(upperWater-4,(int)lower.y());
        var result=new ArrayList<Point>();result.add(new Point(mouth.x(),mouth.z(),mouth.water()));
        result.add(new Point(mouth.x(),mouth.z(),mouth.water()-8));result.add(new Point(upper.x(),upper.z(),upperWater));
        // Find an existing flooded cave column, without loading or writing another chunk.
        Point aquifer=null;
        search:for(int radius=0;radius<=192;radius+=8)for(int dx=-radius;dx<=radius;dx+=8)for(int dz=-radius;dz<=radius;dz+=8){
            if(radius>0&&Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            int x=(int)lower.x()+dx,z=(int)lower.z()+dz;
            int table=(int)(-43+4*Math.sin(x/173.0+(seed&31))+3*Math.cos(z/211.0));
            var network=GeyserCaveShape.network(seed,x,z,44);
            if(network.contains(table)&&network.contains(table+1)){aquifer=new Point(x,z,table);break search;}
        }
        if(aquifer!=null)lowerWater=Math.max(aquifer.water+3,lowerWater);
        result.add(new Point(lower.x(),lower.z(),lowerWater));if(aquifer!=null)result.add(aquifer);
        points=List.copyOf(result);
    }
    public List<Point> path(){return points;}
    public boolean active(){return !points.isEmpty();}
    public boolean mouth(int x,int z){return active()&&Math.hypot(x-points.getFirst().x,z-points.getFirst().z)<3.5;}
    public Cut column(int x,int z){
        Cut result=null;double closest=Double.POSITIVE_INFINITY;
        for(int i=0;i<points.size()-1;i++){
            var a=points.get(i);var b=points.get(i+1);double dx=b.x-a.x,dz=b.z-a.z,length=dx*dx+dz*dz;
            double t=length==0?0:Math.clamp(((x-a.x)*dx+(z-a.z)*dz)/length,0,1);
            double d=Math.hypot(x-a.x-dx*t,z-a.z-dz*t),radius=4+.6*Math.sin(t*Math.PI);
            if(d>=radius||d>=closest)continue;closest=d;
            boolean shaft=length==0;int water=(int)Math.floor(a.water+(b.water-a.water)*t);
            int floor=shaft?b.water-2:water-2+(d>radius*.7?1:0);
            int roof=shaft?a.water+1:water+Math.max(1,(int)Math.floor(4*Math.sqrt(1-d*d/(radius*radius))));
            double norm=Math.sqrt(length);
            result=new Cut(floor,roof,water,shaft,d<(shaft?1.7:radius*.72),norm==0?0:dx/norm,norm==0?0:dz/norm);
        }
        return result;
    }
}
