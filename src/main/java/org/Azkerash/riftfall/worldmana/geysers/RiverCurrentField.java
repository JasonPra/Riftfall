package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;

/** Follow connected water, preferring a lower outlet, otherwise moving away from the broadest body. */
public final class RiverCurrentField {
    @FunctionalInterface public interface Water {int height(int x,int z);}
    public record Flow(double x,double z){}
    private record Point(int x,int z,int firstX,int firstZ,int distance){}
    public static final int DRY=Integer.MIN_VALUE;
    public static Flow solve(int x,int z,Water water){
        int start=water.height(x,z);if(start==DRY)return new Flow(0,0);
        var queue=new ArrayDeque<Point>();var seen=new HashSet<Long>();
        queue.add(new Point(x,z,0,0,0));Point body=null,downhill=null;int bodyScore=0,lowest=start;
        int[][] directions={{8,0},{-8,0},{0,8},{0,-8},{8,8},{8,-8},{-8,8},{-8,-8}};
        while(!queue.isEmpty()&&seen.size()<4096){
            var p=queue.removeFirst();long key=((long)p.x<<32)^(p.z&0xffffffffL);if(!seen.add(key))continue;
            int height=water.height(p.x,p.z);if(height==DRY||p.distance>128)continue;
            if(height<lowest){lowest=height;downhill=p;}
            if(p.distance>0&&((p.x>>3^p.z>>3)&3)==0){
                int score=0;for(int radius:new int[]{24,48,96})for(int[] d:directions){if(water.height(p.x+d[0]*radius/8,p.z+d[1]*radius/8)!=DRY)score++;}
                if(score>bodyScore){bodyScore=score;body=p;}
            }
            for(var d:directions)if(water.height(p.x+d[0]/2,p.z+d[1]/2)!=DRY)queue.addLast(new Point(p.x+d[0],p.z+d[1],p.distance==0?d[0]:p.firstX,p.distance==0?d[1]:p.firstZ,p.distance+1));
        }
        var target=downhill!=null?downhill:body;
        if(target==null||target.firstX==0&&target.firstZ==0)return new Flow(0,0);
        double length=Math.hypot(target.firstX,target.firstZ),sign=downhill!=null?1:-1;
        double wantedX=sign*target.firstX/length,wantedZ=sign*target.firstZ/length,best=-2;int bx=0,bz=0;
        for(var d:directions){
            if(water.height(x+d[0],z+d[1])==DRY||water.height(x+d[0]/2,z+d[1]/2)==DRY)continue;
            double dot=(d[0]*wantedX+d[1]*wantedZ)/Math.hypot(d[0],d[1]);
            if(dot>best){best=dot;bx=d[0];bz=d[1];}
        }
        if(best<0)return new Flow(0,0);
        return new Flow(bx/Math.hypot(bx,bz),bz/Math.hypot(bx,bz));
    }
    private RiverCurrentField(){}
}
