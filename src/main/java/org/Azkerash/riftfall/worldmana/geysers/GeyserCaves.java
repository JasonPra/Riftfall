package org.Azkerash.riftfall.worldmana.geysers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.Azkerash.riftfall.Riftfall;

/** Generation-only excavation, before crystal surveys; writes only the current chunk. */
public final class GeyserCaves extends Feature<NoneFeatureConfiguration> {
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Riftfall.MODID);
    static {FEATURES.register("geyser_caves",GeyserCaves::new);}
    public static void register(IEventBus bus){FEATURES.register(bus);}
    private GeyserCaves(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level();var level=world.getLevel();
        if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return false;
        int cx=context.origin().getX()>>4,cz=context.origin().getZ()>>4;
        GeyserRegion field;
        // Saved legacy exclusions must also apply to generation workers.
        synchronized(level.getDataStorage()){
            var fields=GeyserRegions.get(level);
            field=fields.candidate(Math.floorDiv(cx,GeyserLayout.CELL_CHUNKS),Math.floorDiv(cz,GeyserLayout.CELL_CHUNKS)).filter(g->g.contains(cx,cz)).orElse(null);
        }
        if(field==null||field.legacy())return false;
        long seed=GeyserLayout.mix(world.getSeed()^0x474559534552L);
        var pos=new BlockPos.MutableBlockPos();boolean changed=false;
        int left=(field.centerChunkX()-field.halfWidth())*16,right=(field.centerChunkX()+field.halfWidth()+1)*16;
        int front=(field.centerChunkZ()-field.halfWidth())*16,back=(field.centerChunkZ()+field.halfWidth()+1)*16;
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            // Fade the roof/floor together at the field boundary rather than making a cut wall.
            double fade=Math.clamp(Math.min(Math.min(x-left,right-1-x),Math.min(z-front,back-1-z))/32.0,0,1);
            var column=GeyserCaveShape.column(seed,x,z);
            int roof=Math.min(44,world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-24);
            for(int y=Math.max(-52,world.getMinBuildHeight()+5);y<=roof;y++){
                if(Math.abs(y+8)>52*fade||!column.contains(y))continue;
                pos.set(x,y,z);var state=world.getBlockState(pos);
                if(!state.is(BlockTags.BASE_STONE_OVERWORLD))continue;
                // Keep water/lava barriers intact. Existing aquifers remain vanilla terrain.
                boolean fluid=false;for(var d:net.minecraft.core.Direction.values())if(!world.getFluidState(pos.relative(d)).isEmpty()){fluid=true;break;}
                if(!fluid){world.setBlock(pos,Blocks.CAVE_AIR.defaultBlockState(),2);changed=true;}
            }
        }
        return changed;
    }
}
