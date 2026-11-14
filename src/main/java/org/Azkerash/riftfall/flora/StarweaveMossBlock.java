package org.Azkerash.riftfall.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MossBlock;
import net.minecraft.world.level.block.state.*;

/** Bone meal grows the magical variety rather than reverting it to vanilla moss. */
public final class StarweaveMossBlock extends MossBlock {
    public static final MapCodec<MossBlock> CODEC=simpleCodec(StarweaveMossBlock::new);
    public StarweaveMossBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override public MapCodec<MossBlock> codec(){return CODEC;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state){
        level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
            .getHolder(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("riftfall","starweave_moss_bonemeal")))
            .ifPresent(feature->feature.value().place(level,level.getChunkSource().getGenerator(),random,pos.above()));
    }
}
