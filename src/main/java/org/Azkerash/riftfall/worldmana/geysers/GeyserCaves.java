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
            // Taper route openings at the field boundary.
            double fade=Math.clamp(Math.min(Math.min(x-left,right-1-x),Math.min(z-front,back-1-z))/32.0,0,1);
            var column=GeyserCaveShape.column(seed,x,z);
            int roof=world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-24;
            int bottom=Math.max(-59,world.getMinBuildHeight()+5);
            for(int y=bottom;y<=roof;y++){
                pos.set(x,y,z);var state=world.getBlockState(pos);
                boolean natural=state.is(BlockTags.BASE_STONE_OVERWORLD)||state.isAir()||state.is(Blocks.WATER)||state.is(Blocks.LAVA);
                if(!natural)continue;
                boolean carve=Math.abs(y+8)<=52*fade&&column.contains(y);
                boolean stream=column.streamLevel()!=Integer.MIN_VALUE&&fade>=1&&y>=column.streamLevel()-1&&y<=column.streamLevel()+2;
                var replacement=(carve||stream)?Blocks.CAVE_AIR.defaultBlockState():(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
                if(carve&&y<=column.floodLevel())replacement=Blocks.WATER.defaultBlockState();
                if(stream&&(y==column.streamLevel()-1||column.streamBank()&&y<=column.streamLevel()+1))replacement=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
                if(stream&&!column.streamBank()&&y==column.streamLevel()){
                    replacement=Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,column.streamSource()?0:Math.min(7,1+Math.floorMod(x-58,128)));
                    world.scheduleTick(pos.immutable(),net.minecraft.world.level.material.Fluids.WATER,1);
                }
                if(!replacement.equals(state)){world.setBlock(pos,replacement,2);changed=true;}
            }
            // Sparse accents, after excavation, only on genuine dry floor/ceiling contacts.
            long hash=GeyserLayout.mix(seed^x*731L^z*997L);
            if(fade<1)continue;
            for(int y=bottom+1;y<roof;y++){
                pos.set(x,y,z);if(!world.getBlockState(pos).is(Blocks.CAVE_AIR))continue;
                var below=pos.below();var above=pos.above();
                boolean floor=world.getBlockState(below).is(BlockTags.BASE_STONE_OVERWORLD);
                boolean ceiling=world.getBlockState(above).is(BlockTags.BASE_STONE_OVERWORLD);
                if(column.lush()){
                    if(floor&&(hash&7)!=7){world.setBlock(below,Blocks.MOSS_BLOCK.defaultBlockState(),2);
                        if((hash&15)==0)world.setBlock(pos,Blocks.AZALEA.defaultBlockState(),2);
                        else if((hash&7)==0)world.setBlock(pos,Blocks.MOSS_CARPET.defaultBlockState(),2);
                    }
                    if(ceiling&&(hash&31)==0)world.setBlock(pos,Blocks.CAVE_VINES.defaultBlockState().setValue(net.minecraft.world.level.block.CaveVines.BERRIES,true),2);
                }else if((hash&31)==0&&(floor||ceiling)){
                    var direction=ceiling?net.minecraft.core.Direction.DOWN:net.minecraft.core.Direction.UP;
                    var support=ceiling?above:below;
                    world.setBlock(support,Blocks.DRIPSTONE_BLOCK.defaultBlockState(),2);
                    world.setBlock(pos,Blocks.POINTED_DRIPSTONE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION,direction),2);
                }
            }
        }
        return changed;
    }
}
