package org.Azkerash.riftfall.client;

import java.util.*;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.block.model.BakedQuad;

/** One visible polygon per coplanar region, including tilted stems. Only runs during chunk meshing. */
public final class CrystalQuadUnion {
    private static final double EPS=1e-6;
    private record Point(double u,double v){}
    private record Plane(int facing,int axis,long nx,long ny,long nz,long distance){}
    private record Face(Plane key,int u,int v,List<Point> polygon,boolean reverse){}
    private record Ranked(BakedQuad quad,int priority){}
    public static List<BakedQuad> clean(List<BakedQuad> input,ToIntFunction<BakedQuad> priority){
        if(input.size()<2)return input;
        var ranked=new ArrayList<Ranked>(input.size());for(var quad:input)ranked.add(new Ranked(quad,priority.applyAsInt(quad)));
        ranked.sort(Comparator.comparingInt(Ranked::priority).reversed()); // stable authored order for equal tiers
        var covered=new HashMap<Plane,List<List<Point>>>();var output=new ArrayList<BakedQuad>();
        for(var rankedQuad:ranked){
            var quad=rankedQuad.quad();var face=face(quad);if(face==null){output.add(quad);continue;}
            var prior=covered.computeIfAbsent(face.key(),k->new ArrayList<>());var pieces=new ArrayList<List<Point>>();pieces.add(face.polygon());
            for(var old:prior){var next=new ArrayList<List<Point>>();for(var piece:pieces)subtract(piece,old,next);pieces=next;if(pieces.isEmpty())break;}
            for(var piece:pieces){
                if(piece==face.polygon())output.add(quad);else emit(quad,face,piece,output);
                prior.add(piece);
            }
        }return output;
    }
    private static double area(List<Point> polygon){double a=0;for(int i=0;i<polygon.size();i++){var p=polygon.get(i);var q=polygon.get((i+1)%polygon.size());a+=p.u()*q.v()-p.v()*q.u();}return a*.5;}
    private static double cross(Point a,Point b,Point p){return (b.u()-a.u())*(p.v()-a.v())-(b.v()-a.v())*(p.u()-a.u());}
    private static List<Point> half(List<Point> polygon,Point a,Point b,boolean inside){
        if(polygon.isEmpty())return List.of();var result=new ArrayList<Point>();var previous=polygon.getLast();double pd=cross(a,b,previous);boolean was=inside?pd>=0:pd<=0;
        for(var current:polygon){
            double cd=cross(a,b,current);boolean is=inside?cd>=0:cd<=0;
            if(was!=is){double t=Math.clamp(pd/(pd-cd),0,1);add(result,new Point(previous.u()+(current.u()-previous.u())*t,previous.v()+(current.v()-previous.v())*t));}
            if(is)add(result,current);previous=current;pd=cd;was=is;
        }
        if(result.size()>1&&near(result.getFirst(),result.getLast()))result.removeLast();
        return result.size()<3||Math.abs(area(result))<=EPS*EPS?List.of():result;
    }
    private static boolean near(Point a,Point b){return Math.abs(a.u()-b.u())<EPS&&Math.abs(a.v()-b.v())<EPS;}
    private static void add(List<Point> list,Point p){if(list.isEmpty()||!near(list.getLast(),p))list.add(p);}
    private static void subtract(List<Point> a,List<Point> b,List<List<Point>> output){
        // Test the actual intersection first, so disjoint slanted polygons do not fragment each other.
        List<Point> intersection=a;
        for(int i=0;i<b.size()&&!intersection.isEmpty();i++)intersection=half(intersection,b.get(i),b.get((i+1)%b.size()),true);
        if(intersection.isEmpty()){output.add(a);return;}
        List<Point> remaining=a;
        for(int i=0;i<b.size()&&!remaining.isEmpty();i++){
            var from=b.get(i);var to=b.get((i+1)%b.size());var outside=half(remaining,from,to,false);if(!outside.isEmpty())output.add(outside);remaining=half(remaining,from,to,true);
        }
    }
    private static Face face(BakedQuad quad){
        var data=quad.getVertices();if(data.length!=32)return null;double[] a=new double[3],b=new double[3],p=new double[3];
        for(int axis=0;axis<3;axis++){p[axis]=value(data,0,axis);a[axis]=value(data,1,axis)-p[axis];b[axis]=value(data,3,axis)-p[axis];if(Math.abs(value(data,2,axis)-(p[axis]+a[axis]+b[axis]))>EPS)return null;}
        double[] n={a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};double length=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);if(length<=EPS*EPS)return null;
        for(int axis=0;axis<3;axis++)n[axis]/=length;
        double largest=Math.max(Math.abs(n[0]),Math.max(Math.abs(n[1]),Math.abs(n[2])));int axis=Math.abs(n[0])>=largest-EPS?0:Math.abs(n[1])>=largest-EPS?1:2,u=(axis+1)%3,v=(axis+2)%3;
        var polygon=new ArrayList<Point>();for(int i=0;i<4;i++)polygon.add(new Point(value(data,i,u),value(data,i,v)));boolean reverse=area(polygon)<0;if(reverse)Collections.reverse(polygon);
        var key=new Plane(quad.getDirection().ordinal(),axis,Math.round(n[0]/EPS),Math.round(n[1]/EPS),Math.round(n[2]/EPS),Math.round((n[0]*p[0]+n[1]*p[1]+n[2]*p[2])/EPS));
        return new Face(key,u,v,polygon,reverse);
    }
    private static double value(int[] data,int vertex,int component){return Float.intBitsToFloat(data[vertex*8+component]);}
    private static void emit(BakedQuad source,Face face,List<Point> piece,List<BakedQuad> output){
        var polygon=new ArrayList<>(piece);if(face.reverse())Collections.reverse(polygon);
        for(int i=1;i<polygon.size()-1;i+=2){int last=Math.min(i+2,polygon.size()-1);output.add(clip(source,face,List.of(polygon.getFirst(),polygon.get(i),polygon.get(i+1),polygon.get(last))));}
    }
    private static BakedQuad clip(BakedQuad quad,Face face,List<Point> points){
        var original=quad.getVertices();var data=original.clone();double au=value(original,1,face.u())-value(original,0,face.u()),av=value(original,1,face.v())-value(original,0,face.v()),bu=value(original,3,face.u())-value(original,0,face.u()),bv=value(original,3,face.v())-value(original,0,face.v()),det=au*bv-av*bu;
        for(int vertex=0;vertex<4;vertex++){
            var point=points.get(vertex);double du=point.u()-value(original,0,face.u()),dv=point.v()-value(original,0,face.v()),a=(du*bv-dv*bu)/det,b=(au*dv-av*du)/det;
            for(int component:new int[]{0,1,2,4,5})data[vertex*8+component]=Float.floatToRawIntBits((float)(value(original,0,component)+a*(value(original,1,component)-value(original,0,component))+b*(value(original,3,component)-value(original,0,component))));
        }
        return new BakedQuad(data,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),quad.isShade(),quad.hasAmbientOcclusion());
    }
    private CrystalQuadUnion(){}
}
