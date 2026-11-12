package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Decorations only: terrain and rivers already exist before this feature can run. */
public final class GeyserSurfaceVegetation extends Feature<NoneFeatureConfiguration> {
    public GeyserSurfaceVegetation(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level();int cx=context.origin().getX()>>4,cz=context.origin().getZ()>>4;
        var field=GeyserBiomes.field(world.getLevel(),cx,cz);if(field==null)return false;
        // Jungle trees are temporary flora. The fringe gets only a rare tree; middle-ring forest is more open.
        var zone=field.zone(cx,cz);
        int extraTrees=zone==GeyserZone.MIDDLE?2:zone==GeyserZone.OUTER&&context.random().nextInt(20)==0?1:0;
        if(extraTrees>0){
            var trees=world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("trees_jungle"));
            for(int i=0;i<extraTrees&&trees!=null;i++){
                int x=cx*16+context.random().nextInt(16),z=cz*16+context.random().nextInt(16);
                var root=world.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG,new BlockPos(x,0,z));
                if(world.isEmptyBlock(root)&&world.getFluidState(root).isEmpty()&&world.getBlockState(root.below()).is(BlockTags.DIRT))trees.place(world,context.chunkGenerator(),context.random(),root);
            }
        }

        long seed=GeyserLayout.mix(world.getSeed()^0x534544494d454e54L);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
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
