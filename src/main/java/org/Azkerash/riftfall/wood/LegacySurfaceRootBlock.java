package org.Azkerash.riftfall.wood;
import com.dtteam.dynamictrees.block.branch.SurfaceRootBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.tree.family.Family;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

/** Roots can overlap thick-trunk shells at ground level; retain access to that trunk face. */
public final class LegacySurfaceRootBlock extends SurfaceRootBlock {
    public LegacySurfaceRootBlock(Family family,BlockBehaviour.Properties properties){super(family,properties);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            var core=pos.offset(dx,0,dz);var trunk=level.getBlockState(core);
            if(trunk.getBlock() instanceof BranchBlock branch&&branch.getFamily()==getFamily()&&branch.getRadius(trunk)>8){
                double radius=branch.getRadius(trunk)/16.0+.001;
                if(Math.abs(hit.getLocation().x-core.getX()-.5)<=radius&&Math.abs(hit.getLocation().z-core.getZ()-.5)<=radius)
                    return trunk.useItemOn(stack,level,player,hand,hit.withPosition(core));
            }
        }
        return super.useItemOn(stack,state,level,pos,player,hand,hit);
    }
}
