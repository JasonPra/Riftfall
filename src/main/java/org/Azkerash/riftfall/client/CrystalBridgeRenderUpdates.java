package org.Azkerash.riftfall.client;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.Azkerash.riftfall.crystals.ManaCrystalBlock;
/** Rebuild the far endpoint too when a bridge changes across a section boundary. Never mutates blocks. */
public final class CrystalBridgeRenderUpdates {
    public static void changed(Level level,BlockPos pos,BlockState oldState,BlockState newState){
        if(!level.isClientSide)return;
        if(!(oldState.getBlock() instanceof ManaCrystalBlock)&&!(newState.getBlock() instanceof ManaCrystalBlock)&&oldState.isAir()==newState.isAir()&&oldState.getFluidState().isEmpty()==newState.getFluidState().isEmpty())return;
        for(var direction:Direction.values())for(int distance=1;distance<=2;distance++){
            var endpoint=pos.relative(direction,distance);if(!level.hasChunkAt(endpoint))continue;var state=level.getBlockState(endpoint);
            if(state.getBlock() instanceof ManaCrystalBlock)level.sendBlockUpdated(endpoint,state,state,Block.UPDATE_CLIENTS);
        }
    }
    private CrystalBridgeRenderUpdates(){}
}
