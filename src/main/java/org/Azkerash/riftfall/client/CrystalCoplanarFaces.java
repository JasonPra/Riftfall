package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;

/** Remove only duplicate, same-facing, coplanar prism coverage. Never moves a crystal. */
final class CrystalCoplanarFaces {
    private static final double EPS=1e-6,AREA_EPS=1e-12;
    private record V(double x,double y,double z){
        V add(V b){return new V(x+b.x,y+b.y,z+b.z);}V sub(V b){return new V(x-b.x,y-b.y,z-b.z);}V mul(double s){return new V(x*s,y*s,z*s);}
        double dot(V b){return x*b.x+y*b.y+z*b.z;}V cross(V b){return new V(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        V unit(){return mul(1/Math.sqrt(dot(this)));}
    }
    private record P(double x,double y){}
    private static final class Plane {
        final V origin,normal,u,v;final List<List<P>> coverage=new ArrayList<>();
        Plane(V origin,V normal,V edge){this.origin=origin;this.normal=normal;u=edge.unit();v=normal.cross(u);}
        P project(V point){var delta=point.sub(origin);return new P(delta.dot(u),delta.dot(v));}
        V expand(P point){return origin.add(u.mul(point.x)).add(v.mul(point.y));}
        boolean matches(V n,V[] points){if(normal.dot(n)<1-1e-10)return false;for(var point:points)if(Math.abs(point.sub(origin).dot(normal))>EPS)return false;return true;}
    }
    static void clean(List<List<List<BakedQuad>>> models){
        var planes=new ArrayList<Plane>();
        for(var model:models)for(int bucket=0;bucket<7;bucket++){
            var output=new ArrayList<BakedQuad>();
            for(var quad:model.get(bucket)){
                if(CrystalBedModel.isBed(quad)){output.add(quad);continue;}
                V[] points=new V[4];for(int i=0;i<4;i++)points[i]=vertex(quad,i);
                var cross=points[1].sub(points[0]).cross(points[3].sub(points[0]));
                if(cross.dot(cross)<AREA_EPS*AREA_EPS){output.add(quad);continue;}
                var normal=cross.unit();Plane plane=null;
                for(var candidate:planes)if(candidate.matches(normal,points)){plane=candidate;break;}
                if(plane==null){plane=new Plane(points[0],normal,points[1].sub(points[0]));planes.add(plane);}
                var polygon=new ArrayList<P>();for(var point:points)polygon.add(plane.project(point));
                List<List<P>> remaining=new ArrayList<>(List.of(polygon));
                for(var cover:plane.coverage){
                    var next=new ArrayList<List<P>>();for(var piece:remaining)next.addAll(subtract(piece,cover));remaining=next;
                    if(remaining.isEmpty())break;
                }
                if(remaining.size()==1&&remaining.getFirst().equals(polygon))output.add(quad);
                else for(var piece:remaining)emit(quad,plane,piece,output);
                plane.coverage.addAll(remaining);
            }
            model.set(bucket,List.copyOf(output));
        }
    }
    /** Convex polygon difference, retaining every uncovered portion on its original plane. */
    private static List<List<P>> subtract(List<P> polygon,List<P> cover){
        List<P> inside=polygon;var outside=new ArrayList<List<P>>();
        for(int edge=0;edge<cover.size();edge++){
            var a=cover.get(edge);var b=cover.get((edge+1)%cover.size());
            var keep=new ArrayList<P>();var cut=new ArrayList<P>();
            P previous=inside.getLast();double before=distance(a,b,previous);
            for(var current:inside){
                double after=distance(a,b,current);
                if((before<0)!=(after<0)){
                    double t=before/(before-after);var hit=new P(previous.x+(current.x-previous.x)*t,previous.y+(current.y-previous.y)*t);keep.add(hit);cut.add(hit);
                }
                if(after<0)cut.add(current);else keep.add(current);
                previous=current;before=after;
            }
            if(area(cut)>AREA_EPS)outside.add(List.copyOf(cut));
            if(area(keep)<=AREA_EPS)return List.of(polygon); // No area was covered: preserve the original quad.
            inside=keep;
        }
        return outside;
    }
    private static double distance(P a,P b,P point){double value=(b.x-a.x)*(point.y-a.y)-(b.y-a.y)*(point.x-a.x);return Math.abs(value)<1e-12?0:value;}
    private static double area(List<P> points){if(points.size()<3)return 0;double sum=0;for(int i=0;i<points.size();i++){var a=points.get(i);var b=points.get((i+1)%points.size());sum+=a.x*b.y-a.y*b.x;}return Math.abs(sum)*.5;}
    private static V vertex(BakedQuad quad,int i){var data=quad.getVertices();int stride=data.length/4;return new V(Float.intBitsToFloat(data[i*stride]),Float.intBitsToFloat(data[i*stride+1]),Float.intBitsToFloat(data[i*stride+2]));}
    private static void emit(BakedQuad source,Plane plane,List<P> points,List<BakedQuad> output){
        if(points.size()==4){output.add(fragment(source,plane,points));return;}
        for(int i=1;i+1<points.size();i++){
            var triangle=List.of(points.getFirst(),points.get(i),points.get(i+1));
            if(area(triangle)>AREA_EPS)output.add(fragment(source,plane,List.of(triangle.get(0),triangle.get(1),triangle.get(2),triangle.get(2))));
        }
    }
    private static BakedQuad fragment(BakedQuad source,Plane plane,List<P> points){
        var original=source.getVertices();var data=original.clone();int stride=data.length/4;
        var origin=vertex(source,0);var a=vertex(source,1).sub(origin);var b=vertex(source,3).sub(origin);
        double aa=a.dot(a),ab=a.dot(b),bb=b.dot(b),det=aa*bb-ab*ab;
        for(int i=0;i<4;i++){
            var p=plane.expand(points.get(i));var delta=p.sub(origin);
            double s=(delta.dot(a)*bb-delta.dot(b)*ab)/det,t=(delta.dot(b)*aa-delta.dot(a)*ab)/det;
            data[i*stride]=Float.floatToRawIntBits((float)p.x);data[i*stride+1]=Float.floatToRawIntBits((float)p.y);data[i*stride+2]=Float.floatToRawIntBits((float)p.z);
            for(int uv=4;uv<=5;uv++){
                double base=Float.intBitsToFloat(original[uv]);double value=base+s*(Float.intBitsToFloat(original[stride+uv])-base)+t*(Float.intBitsToFloat(original[3*stride+uv])-base);
                data[i*stride+uv]=Float.floatToRawIntBits((float)value);
            }
        }
        return new BakedQuad(data,source.getTintIndex(),source.getDirection(),source.getSprite(),source.isShade(),source.hasAmbientOcclusion());
    }
    private CrystalCoplanarFaces(){}
}
