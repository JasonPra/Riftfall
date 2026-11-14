package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.*;
import com.dtteam.dynamictrees.utility.CoordUtils;
import net.minecraft.core.Direction;

/** Early major forks, arching boughs and a spreading crown instead of a long bare pole. */
public final class LegacyGrowthLogic extends GrowthLogicKit {
    public static final int MAX_TRUNK_HEIGHT=24,MAX_BRANCH_HEIGHT=29,MAX_HORIZONTAL_REACH=23,BRANCH_ENERGY=24;
    public LegacyGrowthLogic(){super(LegacyTrees.ID);}

    @Override public Direction selectNewDirection(GrowthLogicKitConfiguration configuration,DirectionSelectionContext context) {
        Direction next=super.selectNewDirection(configuration,context);
        if(next==Direction.UP&&context.signal().delta.getY()>=(context.signal().isInTrunk()?MAX_TRUNK_HEIGHT:MAX_BRANCH_HEIGHT))
            context.signal().energy=0;
        if(context.signal().isInTrunk()&&next!=Direction.UP)
            context.signal().energy=BRANCH_ENERGY+Math.floorMod(CoordUtils.coordHashCode(context.pos(),2),6);
        return next;
    }

    @Override public int[] populateDirectionProbabilityMap(GrowthLogicKitConfiguration configuration,DirectionManipulationContext context) {
        int[] map=super.populateDirectionProbabilityMap(configuration,context);
        var signal=context.signal();
        map[Direction.DOWN.ordinal()]=0;
        if(signal.isInTrunk()) {
            int hash=CoordUtils.coordHashCode(signal.rootPos,2);
            int firstFork=9+Math.floorMod(hash,3);
            int offset=signal.delta.getY()-firstFork;
            boolean bough=offset>=0&&offset<=6&&offset%2==0&&context.radius()>1;
            Direction fork=Direction.from2DDataValue((offset/2+Math.floorMod(hash/3,4))&3);
            for(Direction direction:Direction.Plane.HORIZONTAL){
                var neighbor=context.level().getBlockState(context.pos().relative(direction));
                boolean connected=neighbor.getBlock() instanceof com.dtteam.dynamictrees.block.branch.BranchBlock branch
                    &&branch.getFamily()==context.species().getFamily();
                // Existing limbs keep DT's native routing weight. Only new fork sites are gated.
                if(!connected)map[direction.ordinal()]=bough&&direction==fork?2:0;
            }
            map[Direction.UP.ordinal()]=signal.delta.getY()<10?8:signal.delta.getY()<MAX_TRUNK_HEIGHT?2:0;
        }else{
            map[Direction.UP.ordinal()]=signal.delta.getY()<22&&signal.numTurns<2?3:signal.delta.getY()<MAX_BRANCH_HEIGHT?1:0;
            if(signal.dir!=Direction.UP||signal.delta.getY()<26)map[signal.dir.ordinal()]+=3;
            if(signal.delta.getY()>=MAX_BRANCH_HEIGHT)map[Direction.UP.ordinal()]=0;
            for(Direction direction:Direction.Plane.HORIZONTAL){
                var next=context.pos().relative(direction);
                if(Math.abs(next.getX()-signal.rootPos.getX())>MAX_HORIZONTAL_REACH||Math.abs(next.getZ()-signal.rootPos.getZ())>MAX_HORIZONTAL_REACH)
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
