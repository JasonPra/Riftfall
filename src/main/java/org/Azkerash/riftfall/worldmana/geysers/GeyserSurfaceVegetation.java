package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Non-tree surface vegetation; all trees are placed by normal biome decoration. */
public final class GeyserSurfaceVegetation extends Feature<NoneFeatureConfiguration> {
    public GeyserSurfaceVegetation(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level();int cx=context.origin().getX()>>4,cz=context.origin().getZ()>>4;
        var field=GeyserBiomes.field(world.getLevel(),cx,cz);if(field==null)return false;
        long seed=GeyserLayout.mix(world.getSeed()^0x534544494d454e54L);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            if(GeyserShape.zone(field,x,z)==GeyserZone.CORE)continue;
            int floor=world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
            var pos=new BlockPos(x,floor+1,z);
            if(!world.getFluidState(pos).is(FluidTags.WATER)||!world.getFluidState(pos.above()).is(FluidTags.WATER))continue;
            if(!world.getBlockState(pos.below()).isFaceSturdy(world,pos.below(),net.minecraft.core.Direction.UP))continue;
            long hash=GeyserLayout.mix(seed^x*731L^z*997L);
            if((hash&7)==0){
                var grass=Blocks.SEAGRASS.defaultBlockState();
                if(grass.canSurvive(world,pos))world.setBlock(pos,grass,2);
            }
        }
        return true;
    }
}
