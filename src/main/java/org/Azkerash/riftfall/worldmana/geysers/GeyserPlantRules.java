package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Generation-only placement checks; normal player planting remains vanilla behavior. */
public final class GeyserPlantRules {
    public static boolean plant(BlockState state){return state.getBlock() instanceof BushBlock||state.is(Blocks.MELON)||state.is(Blocks.PUMPKIN);}
    public static boolean valid(LevelReader world,BlockPos pos,BlockState state){
        boolean aquatic=state.is(Blocks.SEAGRASS)||state.is(Blocks.TALL_SEAGRASS);
        if(!aquatic&&world.getFluidState(pos).is(FluidTags.WATER))return false;
        if(state.is(Blocks.MELON)||state.is(Blocks.PUMPKIN))return world.getFluidState(pos.below()).isEmpty()&&world.getBlockState(pos.below()).isFaceSturdy(world,pos.below(),Direction.UP);
        return state.canSurvive(world,pos);
    }
    private GeyserPlantRules(){}
}
