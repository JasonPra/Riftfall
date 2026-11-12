package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Jittered 3D cave graph: curved passages, narrow fractures, irregular chambers and drops. */
public final class GeyserCaveShape {
    public static final int SPACING=112,VERTICAL_SPACING=52;
    record Node(double x,double y,double z,long hash){}
    static Node node(long seed,int x,int y,int z){
        long h=GeyserLayout.mix(seed^x*0x632be59bd9b4e019L^y*0x94d049bb133111ebL^z*0x9e3779b97f4a7c15L);
        return new Node(x*112.0+56+Math.floorMod(h,53)-26,y*52.0+26+Math.floorMod(h>>>12,35)-17,z*112.0+56+Math.floorMod(h>>>24,53)-26,h);
    }
    private static double sq(double v){return v*v;}
    private interface Cut {boolean contains(int y);int water();}
    private interface Shape {Cut column(int x,int z);}
    private record Oval(double x,double y,double z,double rx,double ry,double rz,int water) implements Shape {
        public Cut column(int wx,int wz){
            double h=sq((wx-x)/rx)+sq((wz-z)/rz);if(h>1.12)return null;
            return new Cut(){public boolean contains(int wy){return h+sq((wy-y)/ry)<1+.07*Math.sin(wx/7.0+wz/9.0+wy/4.0);}public int water(){return water;}};
        }
    }
    private record Tube(Node a,Node b,double radius,double vertical,int water) implements Shape {
        public Cut column(int x,int z){
            if(x<Math.min(a.x,b.x)-radius||x>Math.max(a.x,b.x)+radius||z<Math.min(a.z,b.z)-radius||z>Math.max(a.z,b.z)+radius)return null;
            double ux=(x-a.x)/radius,uz=(z-a.z)/radius,dx=(b.x-a.x)/radius,dy=(b.y-a.y)/vertical,dz=(b.z-a.z)/radius;
            double length=dx*dx+dy*dy+dz*dz,base=ux*dx+uz*dz,horizontal=ux*ux+uz*uz;
            return new Cut(){public boolean contains(int y){double uy=(y-a.y)/vertical,dot=base+uy*dy,t=Math.clamp(dot/length,0,1);return horizontal+uy*uy-2*t*dot+t*t*length<1;}public int water(){return water;}};
        }
    }
    private record Cell(long seed,int x,int y,int z){}
    private record Chunk(long seed,int x,int z,int top){}
    private static final Map<Cell,List<Shape>> CELLS=new ConcurrentHashMap<>();
    private static final ThreadLocal<LinkedHashMap<Chunk,List<Shape>>> CHUNKS=ThreadLocal.withInitial(LinkedHashMap::new);
    private static List<Shape> cell(Cell c){
        return CELLS.computeIfAbsent(c,key->{
            var shapes=new ArrayList<Shape>();var n=node(c.seed,c.x,c.y,c.z);long h=n.hash;
            int kind=(int)(h&31);boolean chamber=kind<7;
            double rx=chamber?10+Math.floorMod(h>>>7,15):3+Math.floorMod(h>>>7,4),rz=chamber?8+Math.floorMod(h>>>15,14):3+Math.floorMod(h>>>15,4);
            double ry=chamber?6+Math.floorMod(h>>>21,11):3+Math.floorMod(h>>>21,5);
            if(kind==0){rx+=8;rz+=7;ry+=4;}
            int pool=(h&63)==4?(int)(n.y-ry*.25):Integer.MIN_VALUE;
            shapes.add(new Oval(n.x,n.y,n.z,rx,ry,rz,pool));
            route(shapes,n,node(c.seed,c.x+1,c.y,c.z),h+1,false,false);
            if(Math.floorMod(c.x,2)==0||(h&128)==0)route(shapes,n,node(c.seed,c.x,c.y,c.z+1),h+2,false,false);
            if((h&768)==256)route(shapes,n,node(c.seed,c.x+1,c.y,c.z+1),h+3,false,false);
            if((h&3072)==0)route(shapes,n,node(c.seed,c.x,c.y+1,c.z),h+4,true,(h&16384)==0);
            if((h&7)==3){var end=new Node(n.x+20+Math.floorMod(h>>>8,28),n.y-12+Math.floorMod(h>>>15,25),n.z-22+Math.floorMod(h>>>23,45),h);route(shapes,n,end,h+5,false,false);}
            return List.copyOf(shapes);
        });
    }
    private static void route(List<Shape> shapes,Node a,Node b,long seed,boolean shaft,boolean flooded){
        long h=GeyserLayout.mix(seed);boolean fracture=(h&7)==0;
        double dx=b.x-a.x,dz=b.z-a.z,length=Math.max(1,Math.hypot(dx,dz));
        double bend=10+Math.floorMod(h>>>6,20),rise=-16+Math.floorMod(h>>>14,33);
        Node previous=a;
        for(int i=1;i<=10;i++){
            double t=i/10.0,s=Math.sin(Math.PI*t),side=bend*s*Math.sin(Math.PI*t+(h&7));
            var next=new Node(a.x+(b.x-a.x)*t-dz/length*side,a.y+(b.y-a.y)*t+rise*s,a.z+(b.z-a.z)*t+dx/length*side,h);
            double radius=(fracture?1.05:2.2)+Math.floorMod(h>>>24,18)/10.0+(fracture?.3:1.9)*sq(Math.sin(t*7+(h&31)));
            double vertical=fracture?5+Math.floorMod(h>>>34,7):radius*(.65+.65*sq(Math.sin(t*5+(h&15))));
            if(shaft){radius=2+Math.floorMod(h>>>8,3);vertical=radius;}
            int water=flooded?(int)Math.max(a.y,b.y)+5:Integer.MIN_VALUE;
            shapes.add(new Tube(previous,next,radius,vertical,water));previous=next;
        }
    }
    public record Column(boolean lush){}
    public static Column column(long seed,int x,int z){return new Column((GeyserLayout.mix(seed^Math.floorDiv(x,48)*731L^Math.floorDiv(z,48)*997L)&3)==0);}
    public static final class Network {
        private final List<Cut> cuts;private final int roof,table;
        private Network(List<Cut> cuts,int roof,int table){this.cuts=cuts;this.roof=roof;this.table=table;}
        public boolean contains(int y){if(y< -59||y>roof)return false;for(var cut:cuts)if(cut.contains(y))return true;return false;}
        public boolean water(int y){if(!contains(y))return false;if(y<=table)return true;for(var cut:cuts)if(y<=cut.water()&&cut.contains(y))return true;return false;}
    }
    public static Network network(long seed,int x,int z,int roof){
        var key=new Chunk(seed,x>>4,z>>4,Math.floorDiv(roof,VERTICAL_SPACING));var cache=CHUNKS.get();var shapes=cache.get(key);
        if(shapes==null){
            if(CELLS.size()>2048)CELLS.clear();
            var list=new ArrayList<Shape>();
            int minX=Math.floorDiv((x>>4)*16,SPACING)-1,maxX=Math.floorDiv((x>>4)*16+15,SPACING)+1;
            int minZ=Math.floorDiv((z>>4)*16,SPACING)-1,maxZ=Math.floorDiv((z>>4)*16+15,SPACING)+1;
            for(int a=minX;a<=maxX;a++)for(int b=-2;b<=key.top+1;b++)for(int c=minZ;c<=maxZ;c++)list.addAll(cell(new Cell(seed,a,b,c)));
            shapes=List.copyOf(list);cache.put(key,shapes);if(cache.size()>4)cache.remove(cache.keySet().iterator().next());
        }
        var cuts=new ArrayList<Cut>();for(var shape:shapes){var cut=shape.column(x,z);if(cut!=null)cuts.add(cut);}
        int table=(int)(-43+4*Math.sin(x/173.0+(seed&31))+3*Math.cos(z/211.0));
        return new Network(List.copyOf(cuts),roof,table);
    }
    public static boolean contains(long seed,int x,int y,int z){return network(seed,x,z,44).contains(y);}
    private GeyserCaveShape(){}
}
