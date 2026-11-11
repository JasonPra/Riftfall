package org.Azkerash.riftfall.crystals;

import java.util.*;
import net.minecraft.core.*;

/** Six face neighbors only. Saved contacts cover mature neighbors outside the indexed geyser. */
public final class RefinementNeighbors {
    public static long position(long position,int side){return BlockPos.of(position).relative(Direction.values()[side]).asLong();}
    public static int with(int contacts,int side,CrystalTier tier){return (contacts&~(15<<(side*4)))|((tier==null?0:tier.ordinal()+1)<<(side*4));}
    public static CrystalTier tier(int contacts,int side){int value=(contacts>>>(side*4))&15;return value==0?null:CrystalTier.values()[value-1];}
    public static Map<Long,Double> speeds(List<RefinementBatch.Crystal> crystals,RefinementBoostTable table){
        var tiers=new HashMap<Long,CrystalTier>();for(var crystal:crystals)tiers.put(crystal.position(),crystal.tier());
        var result=new HashMap<Long,Double>();
        for(var crystal:crystals){
            CrystalTier highest=crystal.tier();
            for(int side=0;side<6;side++){
                var other=tiers.getOrDefault(position(crystal.position(),side),tier(crystal.contacts(),side));
                if(other!=null&&other.ordinal()>highest.ordinal())highest=other;
            }
            result.put(crystal.position(),table.speed(crystal.tier(),highest));
        }return result;
    }
    private RefinementNeighbors(){}
}
