package org.Azkerash.riftfall.crystals;

import java.util.*;

/** Percentage speed bonuses, indexed by recipient tier then highest adjacent mother tier. */
public record RefinementBoostTable(List<Double> percentages) {
    public RefinementBoostTable {
        percentages=List.copyOf(percentages);
        if(percentages.size()!=64)throw new IllegalArgumentException("An 8 by 8 boost table is required");
        for(int target=0;target<8;target++)for(int source=0;source<8;source++){
            double value=percentages.get(target*8+source);
            if(!Double.isFinite(value)||value<0||value>10000||(source<=target&&value!=0))throw new IllegalArgumentException("Invalid refinement bonus");
        }
    }
    public double percent(CrystalTier target,CrystalTier source){return source==null?0:percentages.get(target.ordinal()*8+source.ordinal());}
    public double speed(CrystalTier target,CrystalTier source){return 1+percent(target,source)/100;}
    public static RefinementBoostTable none(){return new RefinementBoostTable(Collections.nCopies(64,0.0));}
    public static RefinementBoostTable defaults(){
        double[][] rows={
            {0,25,50,75,100,125,150,200},
            {0,0,20,40,60,80,100,150},
            {0,0,0,15,30,45,60,100},
            {0,0,0,0,10,20,30,60},
            {0,0,0,0,0,5,15,35},
            {0,0,0,0,0,0,5,15},
            {0,0,0,0,0,0,0,5},
            {0,0,0,0,0,0,0,0}
        };
        var values=new ArrayList<Double>();for(var row:rows)for(double value:row)values.add(value);
        return new RefinementBoostTable(values);
    }
}
