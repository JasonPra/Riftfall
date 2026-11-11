package org.Azkerash.riftfall.client;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.Azkerash.riftfall.crystals.*;

/** Refresh group eligibility across chunk-section borders without loading terrain or ticking groups. */
public final class CrystalGroupRenderUpdates {
    public static void changed(Level level,BlockPos pos,BlockState old,BlockState next){
        if(!level.isClientSide)return;
        var faces=EnumSet.noneOf(Direction.class);
        if(old.getBlock() instanceof ManaCrystalBlock)faces.add(old.getValue(ManaCrystalBlock.FACING));
        if(next.getBlock() instanceof ManaCrystalBlock)faces.add(next.getValue(ManaCrystalBlock.FACING));
        for(var face:faces){var x=CrystalSurfaceConnections.tangent(face,1);var z=CrystalSurfaceConnections.tangent(face,2);
            for(int u=-2;u<=2;u++)for(int v=-2;v<=2;v++){
                if(u==0&&v==0)continue;var member=pos.relative(x,u).relative(z,v);if(!level.hasChunkAt(member))continue;
                var state=level.getBlockState(member);if(state.getBlock() instanceof ManaCrystalBlock&&state.getValue(ManaCrystalBlock.FACING)==face)level.sendBlockUpdated(member,state,state,Block.UPDATE_CLIENTS);
            }
        }
    }
    private CrystalGroupRenderUpdates(){}
}
