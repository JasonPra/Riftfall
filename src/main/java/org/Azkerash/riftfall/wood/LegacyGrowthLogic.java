package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.*;
import com.dtteam.dynamictrees.utility.CoordUtils;
import net.minecraft.core.Direction;

/** Long-lived central leader with tiered, outward-reaching boughs and an irregular summit. */
public final class LegacyGrowthLogic extends GrowthLogicKit {
    public LegacyGrowthLogic(){super(LegacyTrees.ID);}

    @Override public Direction selectNewDirection(GrowthLogicKitConfiguration configuration,DirectionSelectionContext context) {
        Direction next=super.selectNewDirection(configuration,context);
        if(context.signal().isInTrunk()&&next!=Direction.UP)
            context.signal().energy=20+Math.floorMod(CoordUtils.coordHashCode(context.pos(),2),6);
        return next;
    }

    @Override public int[] populateDirectionProbabilityMap(GrowthLogicKitConfiguration configuration,DirectionManipulationContext context) {
        int[] map=super.populateDirectionProbabilityMap(configuration,context);
        var signal=context.signal();
        map[Direction.DOWN.ordinal()]=0;
        if(signal.isInTrunk()) {
            int tier=Math.floorMod(signal.delta.getY()+CoordUtils.coordHashCode(signal.rootPos,2),9);
            boolean bough=tier<2&&context.radius()>1;
            for(Direction direction:Direction.Plane.HORIZONTAL)map[direction.ordinal()]=bough?2:0;
            map[Direction.UP.ordinal()]=signal.delta.getY()<60?8:1;
        }else{
            map[Direction.UP.ordinal()]=1;
            map[signal.dir.ordinal()]+=3;
            for(Direction direction:Direction.Plane.HORIZONTAL){
                var next=context.pos().relative(direction);
                if(Math.abs(next.getX()-signal.rootPos.getX())>23||Math.abs(next.getZ()-signal.rootPos.getZ())>23)
                    map[direction.ordinal()]=0;
            }
        }
        map[signal.dir.getOpposite().ordinal()]=0;
        return map;
    }

    @Override public float getEnergy(GrowthLogicKitConfiguration configuration,PositionalSpeciesContext context) {
        return context.species().getSignalEnergy()+Math.floorMod(CoordUtils.coordHashCode(context.pos(),2),7);
    }
}
