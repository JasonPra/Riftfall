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
    static {FEATURES.register("geyser_caves",GeyserCaves::new);FEATURES.register("geyser_surface",GeyserSurfaceVegetation::new);}
    public static void register(IEventBus bus){FEATURES.register(bus);GeyserSurfacePlan.register(bus);}
    private GeyserCaves(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level();var level=world.getLevel();
        if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return false;
        int cx=context.origin().getX()>>4,cz=context.origin().getZ()>>4;
        var field=GeyserBiomes.field(level,cx,cz);if(field==null)return false;
        long seed=GeyserLayout.mix(world.getSeed()^0x474559534552L);
        var pos=new BlockPos.MutableBlockPos();boolean changed=false;
        var river=GeyserUndergroundRiver.of(seed,field);
        var plan=world.getChunk(cx,cz).getExistingDataOrNull(GeyserSurfacePlan.PLAN);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
            // Taper route openings at the field boundary.
            double fade=Math.clamp(-GeyserTransition.distance(field,x,z)/32.0,0,1);
            if(GeyserTransition.distance(field,x,z)>0)continue;
            int planned=plan!=null&&plan.length>=256?plan[(x&15)*16+(z&15)]:Integer.MIN_VALUE;
            int surface=planned==Integer.MIN_VALUE?world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z):(planned>>1)+1;
            int roof=surface-24;
            var network=GeyserCaveShape.network(seed,x,z,roof);
            int bottom=Math.max(-59,world.getMinBuildHeight()+5);
            var entry=fade>=1?GeyserTerrain.entranceColumn(seed,field,x,z):null;
            var hydro=river.column(x,z);
            int entranceTop=Math.max(entry!=null?surface+3:roof,hydro!=null?hydro.roof():roof);
            for(int y=bottom;y<=entranceTop;y++){
                pos.set(x,y,z);var state=world.getBlockState(pos);
                boolean natural=state.is(BlockTags.BASE_STONE_OVERWORLD)||state.isAir()||state.is(Blocks.WATER)||state.is(Blocks.LAVA)
                    ||hydro!=null&&(state.is(BlockTags.DIRT)||state.is(BlockTags.SAND)||state.is(Blocks.GRAVEL)||state.is(Blocks.CLAY)||state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES));
                if(!natural)continue;
                boolean entrance=entry!=null&&entry.contains(y);
                boolean channel=hydro!=null&&y>hydro.floor()&&y<=hydro.roof();
                boolean bed=hydro!=null&&y>=hydro.floor()-3&&y<=hydro.floor();
                if(y>roof&&!entrance&&!channel&&!bed)continue;
                boolean carve=(fade>=1&&network.contains(y))||entrance||channel;
                boolean wet=channel?hydro.wet()&&y<=hydro.water():carve&&network.water(y);
                var replacement=bed?GeyserTerrain.sediment(seed,x,z,hydro.floor()-y):carve?(wet?Blocks.WATER:Blocks.CAVE_AIR).defaultBlockState():(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
                if(channel&&wet&&hydro.falling())replacement=Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,8);
                if(wet&&(y&7)==0)world.scheduleTick(pos.immutable(),net.minecraft.world.level.material.Fluids.WATER,1);
                if(!replacement.equals(state)){world.setBlock(pos,replacement,2);changed=true;}
            }
            // Sparse accents, after excavation, only on genuine dry floor/ceiling contacts.
            long hash=GeyserLayout.mix(seed^x*731L^z*997L);
            boolean lush=GeyserCaveShape.column(seed,x,z).lush();
            if(fade<1)continue;
            for(int y=bottom+1;y<roof;y++){
                pos.set(x,y,z);if(!world.getBlockState(pos).is(Blocks.CAVE_AIR))continue;
                var below=pos.below();var above=pos.above();
                boolean floor=world.getBlockState(below).is(BlockTags.BASE_STONE_OVERWORLD);
                boolean ceiling=world.getBlockState(above).is(BlockTags.BASE_STONE_OVERWORLD);
                if(lush){
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
