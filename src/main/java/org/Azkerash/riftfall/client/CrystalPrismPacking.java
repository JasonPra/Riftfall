package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.Azkerash.riftfall.crystals.CrystalSurfaceConnections;

/** Fit closed prisms into free space once when a chunk mesh is built. Never removes textured faces. */
final class CrystalPrismPacking {
    private static final double EPS=1e-7,GAP=1.0/2048;
    record V(double x,double y,double z){
        V add(V b){return new V(x+b.x,y+b.y,z+b.z);}V sub(V b){return new V(x-b.x,y-b.y,z-b.z);}V mul(double s){return new V(x*s,y*s,z*s);}
        double dot(V b){return x*b.x+y*b.y+z*b.z;}V cross(V b){return new V(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        double length(){return Math.sqrt(dot(this));}
    }
    record Box(V center,List<V> edges){
        double radius(V axis){double result=0;for(var edge:edges)result+=Math.abs(edge.dot(axis));return result;}
        double volume(){return 8*Math.abs(edges.get(0).dot(edges.get(1).cross(edges.get(2))));}
        boolean contained(){for(var axis:AXES){double c=center.dot(axis),r=radius(axis);if(c-r< -EPS||c+r>1+EPS)return false;}return true;}
    }
    private static final List<V> AXES=List.of(new V(1,0,0),new V(0,1,0),new V(0,0,1));
    private static final List<int[]> OFFSETS=offsets();
    private static List<int[]> offsets(){
        var result=new ArrayList<int[]>();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)result.add(new int[]{x,z});
        result.sort(Comparator.<int[]>comparingInt(p->p[0]*p[0]+p[1]*p[1]).thenComparingInt(p->p[0]).thenComparingInt(p->p[1]));return List.copyOf(result);
    }
    static boolean overlaps(Box a,Box b){return intersects(a,b,GAP);}
    static boolean penetrates(Box a,Box b){return intersects(a,b,-EPS*2);}
    private static boolean intersects(Box a,Box b,double clearance){
        var delta=b.center.sub(a.center);
        for(var axis:AXES)if(separated(a,b,delta,axis,clearance))return false;
        for(int i=0;i<3;i++){
            if(separated(a,b,delta,a.edges.get(i).cross(a.edges.get((i+1)%3)),clearance))return false;
            if(separated(a,b,delta,b.edges.get(i).cross(b.edges.get((i+1)%3)),clearance))return false;
            for(int j=0;j<3;j++)if(separated(a,b,delta,a.edges.get(i).cross(b.edges.get(j)),clearance))return false;
        }return true;
    }
    private static boolean separated(Box a,Box b,V delta,V axis,double clearance){double length=axis.length();return length>EPS&&Math.abs(delta.dot(axis))>=a.radius(axis)+b.radius(axis)+clearance*length;}
    private static boolean available(Box candidate,List<Box> placed){if(!candidate.contained())return false;for(var box:placed)if(overlaps(candidate,box))return false;return true;}
    static Box fit(Box original,Direction growth,List<Box> placed){
        if(available(original,placed))return original;
        var normal=direction(growth);int height=0;double alignment=-1;
        for(int i=0;i<3;i++){var edge=original.edges.get(i);double score=Math.abs(edge.dot(normal))/edge.length();if(score>alignment){alignment=score;height=i;}}
        V heightEdge=original.edges.get(height);if(heightEdge.dot(normal)<0)heightEdge=heightEdge.mul(-1);
        var root=original.center.sub(heightEdge);var u=direction(CrystalSurfaceConnections.tangent(growth,1));var v=direction(CrystalSurfaceConnections.tangent(growth,2));
        double[] widths={1,.85,.7,.55,.4,.25,.15};
        // Prefer the existing root and full height; reduce width before searching nearby gaps.
        for(double width:widths){var candidate=candidate(original,root,heightEdge,height,1,width,new V(0,0,0));if(available(candidate,placed))return candidate;}
        for(var offset:OFFSETS){
            if(Math.abs(offset[0])>4||Math.abs(offset[1])>4)continue;
            var shift=u.mul(offset[0]/16.0).add(v.mul(offset[1]/16.0));
            for(double width:widths){var candidate=candidate(original,root,heightEdge,height,1,width,shift);if(available(candidate,placed))return candidate;}
        }
        // Crowded corners can also limit length. Even these small prisms keep all six faces.
        for(double length:new double[]{.75,.5,.25,.125,.05,.02,.005})for(var offset:OFFSETS){
            var shift=u.mul(offset[0]/16.0).add(v.mul(offset[1]/16.0));
            for(double width:new double[]{.4,.2,.1,.05,.02,.01}){var candidate=candidate(original,root,heightEdge,height,length,width,shift);if(available(candidate,placed))return candidate;}
        }
        throw new IllegalStateException("No free crystal prism slot within block");
    }
    private static Box candidate(Box original,V root,V heightEdge,int height,double length,double width,V shift){
        var edges=new ArrayList<V>();for(int i=0;i<3;i++)edges.add(original.edges.get(i).mul(i==height?length:width));
        return new Box(root.add(heightEdge.mul(length)).add(shift),List.copyOf(edges));
    }
    private static V direction(Direction d){return new V(d.getStepX(),d.getStepY(),d.getStepZ());}
    private static V vertex(BakedQuad quad,int index){var data=quad.getVertices();int stride=data.length/4;return new V(Float.intBitsToFloat(data[index*stride]),Float.intBitsToFloat(data[index*stride+1]),Float.intBitsToFloat(data[index*stride+2]));}
    static Box bounds(List<BakedQuad> quads){
        if(quads.size()!=6)return null;var corner=vertex(quads.getFirst(),0);var directions=new ArrayList<V>();var center=new V(0,0,0);
        for(var quad:quads)for(int i=0;i<4;i++){
            var p=vertex(quad,i);center=center.add(p);
            if(p.sub(corner).length()>EPS)continue;
            for(int adjacent:new int[]{(i+1)%4,(i+3)%4}){
                var edge=vertex(quad,adjacent).sub(corner);if(edge.length()<EPS)continue;
                boolean known=false;for(var old:directions)if(Math.abs(old.dot(edge))/(old.length()*edge.length())>1-EPS){known=true;break;}
                if(!known)directions.add(edge);
            }
        }
        if(directions.size()!=3)return null;
        return new Box(center.mul(1.0/24),directions.stream().map(e->e.mul(.5)).toList());
    }
    private record Piece(int model,int tint,Box box){}
    private record Layout(List<Box> boxes,List<Direction> growth){}
    private static final Map<Layout,List<Box>> CACHE=Collections.synchronizedMap(new LinkedHashMap<>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Layout,List<Box>> entry){return size()>512;}
    });
    static void pack(List<List<List<BakedQuad>>> models,List<Direction> growth){
        var pieces=new ArrayList<Piece>();
        for(int model=0;model<models.size();model++){
            var map=new LinkedHashMap<Integer,List<BakedQuad>>();
            for(var bucket:models.get(model))for(var quad:bucket)if(!CrystalBedModel.isBed(quad))map.computeIfAbsent(quad.getTintIndex(),id->new ArrayList<>()).add(quad);
            for(var entry:map.entrySet()){var box=bounds(entry.getValue());if(box!=null)pieces.add(new Piece(model,entry.getKey(),box));}
        }
        pieces.sort(Comparator.<Piece>comparingDouble(p->-p.box.volume()).thenComparingInt(Piece::model).thenComparingInt(Piece::tint));
        var key=new Layout(pieces.stream().map(Piece::box).toList(),pieces.stream().map(p->growth.get(p.model)).toList());
        var layout=CACHE.get(key);
        if(layout==null){
            var placed=new ArrayList<Box>();for(var piece:pieces)placed.add(fit(piece.box,growth.get(piece.model),placed));
            layout=List.copyOf(placed);CACHE.put(key,layout);
        }
        var fitted=new HashMap<Piece,Box>();for(int i=0;i<pieces.size();i++)if(!pieces.get(i).box.equals(layout.get(i)))fitted.put(pieces.get(i),layout.get(i));
        for(var entry:fitted.entrySet()){
            var piece=entry.getKey();var target=entry.getValue();
            for(int bucket=0;bucket<7;bucket++){
                var output=new ArrayList<BakedQuad>();for(var quad:models.get(piece.model).get(bucket))output.add(quad.getTintIndex()==piece.tint?transform(quad,piece.box,target):quad);
                models.get(piece.model).set(bucket,List.copyOf(output));
            }
        }
        // Fitted caps can move away from their original cell boundary. Keep closed prism faces
        // in the general bucket so a neighboring stone block cannot incorrectly suppress them.
        for(var model:models){
            var general=new ArrayList<>(model.get(6));
            for(int bucket=0;bucket<6;bucket++){
                var bed=new ArrayList<BakedQuad>();for(var quad:model.get(bucket)){if(CrystalBedModel.isBed(quad))bed.add(quad);else general.add(quad);}
                model.set(bucket,List.copyOf(bed));
            }
            model.set(6,List.copyOf(general));
        }
    }
    private static BakedQuad transform(BakedQuad quad,Box source,Box target){
        var data=quad.getVertices().clone();int stride=data.length/4;var a=source.edges.get(0);var b=source.edges.get(1);var c=source.edges.get(2);double det=a.dot(b.cross(c));
        for(int i=0;i<4;i++){
            var delta=vertex(quad,i).sub(source.center);double x=delta.dot(b.cross(c))/det,y=delta.dot(c.cross(a))/det,z=delta.dot(a.cross(b))/det;
            var p=target.center.add(target.edges.get(0).mul(x)).add(target.edges.get(1).mul(y)).add(target.edges.get(2).mul(z));
            data[i*stride]=Float.floatToRawIntBits((float)p.x);data[i*stride+1]=Float.floatToRawIntBits((float)p.y);data[i*stride+2]=Float.floatToRawIntBits((float)p.z);
        }
        return new BakedQuad(data,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion());
    }
    private CrystalPrismPacking(){}
}
