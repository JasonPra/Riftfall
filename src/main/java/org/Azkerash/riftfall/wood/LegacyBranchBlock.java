package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.block.branch.ThickBranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Radius changes resize the existing wood; they must not restore harvested bark. */
public final class LegacyBranchBlock extends ThickBranchBlock {
    public LegacyBranchBlock(ResourceLocation name,BlockBehaviour.Properties properties){super(name,properties);}

    @Override public int setRadius(LevelAccessor level,BlockPos pos,int radius,Direction originDir,int flags){
        var stripped=getFamily().getStrippedBranch().orElse(null);
        // BottomFlare and other DT features call family.getBranch().setRadius even when
        // this position is stripped. Dispatch to that variant before DT updates its shells.
        if(stripped!=null&&stripped!=this&&level.getBlockState(pos).is(stripped))
            return stripped.setRadius(level,pos,radius,originDir,flags);
        return super.setRadius(level,pos,radius,originDir,flags);
    }
}
