package org.Azkerash.riftfall.worldmana.geysers;

/** Deterministic, chunk-independent routes. Most junctions are tunnels, not chambers. */
public final class GeyserCaveShape {
    public static final int SPACING=128;
    private record Node(double x,double y,double z,double radius,double height,long hash){}
    private static Node node(long seed,int x,int z){
        long h=GeyserLayout.mix(seed^x*0x632be59bd9b4e019L^z*0x9e3779b97f4a7c15L);
        boolean cavern=(h&3)==0;
        return new Node(x*128.0+64,-18+Math.floorMod(h>>>4,13),z*128.0+64,
            cavern?13+Math.floorMod(h>>>8,8):6,cavern?8+Math.floorMod(h>>>16,5):5,h);
    }
    private static double square(double v){return v*v;}
    public record Column(double[][] terms,boolean lush,int floodLevel,int streamLevel,boolean streamSource,boolean streamBank){
        public boolean contains(int y){
            if(y< -55||y>30)return false;
            for(var term:terms)if(term[0]+square((y-term[1])/term[2])<1)return true;
            return false;
        }
    }
    private static void tunnel(java.util.List<double[]> terms,Node a,Node b,double x,double z){
        double dx=b.x-a.x,dz=b.z-a.z,t=Math.clamp(((x-a.x)*dx+(z-a.z)*dz)/(dx*dx+dz*dz),0,1);
        double bend=12*Math.sin(t*Math.PI*2),length=Math.sqrt(dx*dx+dz*dz);
        double cx=a.x+t*dx-dz/length*bend,cz=a.z+t*dz+dx/length*bend,cy=a.y+(b.y-a.y)*t+3*Math.sin(t*Math.PI);
        double radius=4+1.5*Math.sin(t*Math.PI),horizontal=(square(x-cx)+square(z-cz))/square(radius);
        if(horizontal<1)terms.add(new double[]{horizontal,cy,radius*.85});
    }
    public static Column column(long seed,int x,int z){
        var terms=new java.util.ArrayList<double[]>();
        int cellX=Math.floorDiv(x-64,SPACING),cellZ=Math.floorDiv(z-64,SPACING);
        int flood=Integer.MIN_VALUE,stream=Integer.MIN_VALUE;boolean source=false,bank=false;
        for(int a=cellX;a<=cellX+1;a++)for(int b=cellZ;b<=cellZ+1;b++){
            Node n=node(seed,a,b);double horizontal=(square(x-n.x)+square(z-n.z))/square(n.radius);
            if(horizontal<1)terms.add(new double[]{horizontal,n.y,n.height});
            tunnel(terms,n,node(seed,a+1,b),x,z);tunnel(terms,n,node(seed,a,b+1),x,z);
            // Rare drop shafts, connected to a junction but never opening onto the surface.
            if((n.hash&31)==3&&(square(x-n.x)+square(z-n.z))<9)terms.add(new double[]{0,-29,22});
            if((n.hash&15)==0&&horizontal<1)flood=(int)n.y-1;
            // Short stepped rivulets through occasional chambers: one upstream source, an open downhill run.
            if((n.hash&15)==4&&Math.abs(z-n.z)<=2&&x>=n.x-6&&x<=n.x+6){
                int along=x-(int)n.x+6;
                stream=(int)(n.y-n.height)+4-along/4;source=along==0;bank=Math.abs(z-n.z)==2;
            }
        }
        long habitat=GeyserLayout.mix(seed^Math.floorDiv(x,64)*731L^Math.floorDiv(z,64)*997L);
        return new Column(terms.toArray(double[][]::new),(habitat&3)==0,flood,stream,source,bank);
    }
    public record Layer(Column shape,int offset){}
    public record Network(java.util.List<Layer> layers,double[][] shafts){
        public boolean contains(int y){
            for(var layer:layers)if(layer.shape.contains(y-layer.offset))return true;
            for(var shaft:shafts)if(y>=shaft[0]&&y<=shaft[1])return true;
            return false;
        }
        public Layer stream(int y){
            for(var layer:layers)if(layer.shape.streamLevel!=Integer.MIN_VALUE&&y>=layer.shape.streamLevel+layer.offset-1&&y<=layer.shape.streamLevel+layer.offset+2)return layer;
            return null;
        }
        public boolean water(int y){
            for(var layer:layers)if(layer.shape.contains(y-layer.offset)&&y-layer.offset<=layer.shape.floodLevel)return true;
            return false;
        }
    }
    /** Fill the available depth with separate networks, linked at selected junctions. */
    public static Network network(long seed,int x,int z,int roof){
        var layers=new java.util.ArrayList<Layer>();
        for(int offset=-28;offset-18<=roof-8;offset+=28)layers.add(new Layer(column(seed^GeyserLayout.mix(offset+101L),x,z),offset));
        var shafts=new java.util.ArrayList<double[]>();
        int nx=Math.floorDiv(x-64+64,128),nz=Math.floorDiv(z-64+64,128);
        long h=GeyserLayout.mix(seed^nx*731L^nz*997L);
        if((h&3)==0&&square(x-(nx*128+64))+square(z-(nz*128+64))<12)
            for(int i=1;i<layers.size();i++)shafts.add(new double[]{-24+layers.get(i-1).offset,-4+layers.get(i).offset});
        return new Network(java.util.List.copyOf(layers),shafts.toArray(double[][]::new));
    }
    public static boolean contains(long seed,int x,int y,int z){return column(seed,x,z).contains(y);}
    private GeyserCaveShape(){}
}
