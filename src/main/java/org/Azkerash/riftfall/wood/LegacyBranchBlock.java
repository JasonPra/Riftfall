package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.block.branch.ThickBranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Radius changes resize the existing wood; they must not restore harvested bark. */
public class LegacyBranchBlock extends ThickBranchBlock {
    public LegacyBranchBlock(ResourceLocation name,BlockBehaviour.Properties properties){super(name,properties);}
    public static int faceBit(Direction face){return 1<<face.get3DDataValue();}

    @Override public int setRadius(LevelAccessor level,BlockPos pos,int radius,Direction originDir,int flags){
        var existing=level.getBlockState(pos).getBlock();
        // DT's bottom flare uses the ordinary branch even at stripped/wounded positions.
        if(this==getFamily().getBranch().orElse(null)&&existing!=this&&existing instanceof LegacyBranchBlock branch&&branch.getFamily()==getFamily())
            return branch.setOwnRadius(level,pos,radius,originDir,flags);
        return super.setRadius(level,pos,radius,originDir,flags);
    }
    private int setOwnRadius(LevelAccessor level,BlockPos pos,int radius,Direction originDir,int flags){
        return super.setRadius(level,pos,radius,originDir,flags);
    }
}
