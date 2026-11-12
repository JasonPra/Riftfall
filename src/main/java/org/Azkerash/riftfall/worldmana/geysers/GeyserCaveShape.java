package org.Azkerash.riftfall.worldmana.geysers;

/** World-coordinate chambers joined by a continuous, branching grid of curved tunnels. */
public final class GeyserCaveShape {
    public static final int SPACING=128;
    private record Node(double x,double y,double z,double radius,double height){}
    private static Node node(long seed,int x,int z){
        long h=GeyserLayout.mix(seed^x*0x632be59bd9b4e019L^z*0x9e3779b97f4a7c15L);
        return new Node(x*128.0+64, -12+Math.floorMod(h,25),z*128.0+64,38+Math.floorMod(h>>>8,23),21+Math.floorMod(h>>>16,10));
    }
    private static double square(double v){return v*v;}
    public record Column(double[][] terms){
        public boolean contains(int y){
            if(y< -52||y>44)return false;
            for(var term:terms)if(term[0]+square((y-term[1])/term[2])<1)return true;
            return false;
        }
    }
    private static void tunnel(java.util.List<double[]> terms,Node a,Node b,double x,double z){
        double dx=b.x-a.x,dz=b.z-a.z,t=Math.clamp(((x-a.x)*dx+(z-a.z)*dz)/(dx*dx+dz*dz),0,1);
        double bend=9*Math.sin(t*Math.PI*2),length=Math.sqrt(dx*dx+dz*dz);
        double cx=a.x+t*dx-dz/length*bend,cz=a.z+t*dz+dx/length*bend,cy=a.y+(b.y-a.y)*t+4*Math.sin(t*Math.PI);
        double radius=11+3*Math.sin(t*Math.PI),horizontal=(square(x-cx)+square(z-cz))/square(radius);
        if(horizontal<1)terms.add(new double[]{horizontal,cy,radius*.8});
    }
    /** Precompute the expensive horizontal shape once per column, not once per voxel. */
    public static Column column(long seed,int x,int z){
        var terms=new java.util.ArrayList<double[]>();
        int cellX=Math.floorDiv(x-64,SPACING),cellZ=Math.floorDiv(z-64,SPACING);
        for(int a=cellX;a<=cellX+1;a++)for(int b=cellZ;b<=cellZ+1;b++){
            Node n=node(seed,a,b);double horizontal=(square(x-n.x)+square(z-n.z))/square(n.radius);
            if(horizontal<1)terms.add(new double[]{horizontal,n.y,n.height});
            tunnel(terms,n,node(seed,a+1,b),x,z);tunnel(terms,n,node(seed,a,b+1),x,z);
        }
        return new Column(terms.toArray(double[][]::new));
    }
    public static boolean contains(long seed,int x,int y,int z){return column(seed,x,z).contains(y);}
    private GeyserCaveShape(){}
}
