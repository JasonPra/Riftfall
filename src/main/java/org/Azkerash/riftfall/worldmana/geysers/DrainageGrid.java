package org.Azkerash.riftfall.worldmana.geysers;

import java.util.*;

/** Lowest-spill drainage tree. Parents point downstream; depressions inherit their spill elevation. */
final class DrainageGrid {
    record Result(double[] spill,int[] parent){}
    private record Visit(int index,double spill,double length){}
    static Result solve(int size,double[] raw,int goal){
        return solve(size,raw,new int[]{goal},Double.NEGATIVE_INFINITY);
    }
    static Result solve(int size,double[] raw,int[] goals,double minimumOutlet){
        if(size<1||raw.length!=size*size||goals.length==0)throw new IllegalArgumentException("Invalid drainage grid");
        for(int goal:goals)if(goal<0||goal>=raw.length||!Double.isFinite(raw[goal]))throw new IllegalArgumentException("Invalid drainage outlet");
        var spill=new double[raw.length];var length=new double[raw.length];var parent=new int[raw.length];
        Arrays.fill(spill,Double.POSITIVE_INFINITY);Arrays.fill(length,Double.POSITIVE_INFINITY);Arrays.fill(parent,-1);
        var queue=new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::spill).thenComparingDouble(Visit::length).thenComparingInt(Visit::index));
        for(int goal:goals){spill[goal]=Math.max(minimumOutlet,raw[goal]);length[goal]=0;queue.add(new Visit(goal,spill[goal],0));}
        while(!queue.isEmpty()){
            var current=queue.remove();int i=current.index;
            if(current.spill!=spill[i]||current.length!=length[i])continue;
            int ix=i%size,iz=i/size;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                if(dx==0&&dz==0||ix+dx<0||ix+dx>=size||iz+dz<0||iz+dz>=size)continue;
                int j=i+dz*size+dx;if(!Double.isFinite(raw[j]))continue;
                double s=Math.max(raw[j],spill[i]),d=length[i]+Math.hypot(dx,dz);
                if(s<spill[j]||s==spill[j]&&d<length[j]){spill[j]=s;length[j]=d;parent[j]=i;queue.add(new Visit(j,s,d));}
            }
        }
        return new Result(spill,parent);
    }
}
