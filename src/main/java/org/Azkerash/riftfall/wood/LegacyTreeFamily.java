package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.tree.family.UndergroundRootsFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class LegacyTreeFamily extends UndergroundRootsFamily {
    public LegacyTreeFamily(ResourceLocation name) {super(name);}

    @Override public BlockBehaviour.Properties getDefaultBranchProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).sound(SoundType.WOOD)
            .strength(8,1200).requiresCorrectToolForDrops().lightLevel(state->3).noLootTable().ignitedByLava();
    }
    @Override public Tier getDefaultBranchHarvestTier(){return Tiers.DIAMOND;}
    @Override public Tier getDefaultStrippedBranchHarvestTier(){return Tiers.DIAMOND;}
    @Override public Tier getDefaultRootsHarvestTier(){return Tiers.DIAMOND;}

    @Override public boolean isAcceptableSoilForRootSystem(BlockState state) {
        return super.isAcceptableSoilForRootSystem(state)||state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    @Override public boolean canStripBranch(BlockState state,Level level,BlockPos pos,Player player,ItemStack tool) {
        return (player!=null&&player.isCreative()||LegacyLogBlock.canHarvest(tool,state))
            &&super.canStripBranch(state,level,pos,player,tool);
    }

    @Override public boolean stripBranch(BlockState state,Level level,BlockPos pos,Player player,ItemStack tool) {
        if(!canStripBranch(state,level,pos,player,tool))return false;
        boolean stripped=super.stripBranch(state,level,pos,player,tool);
        if(stripped&&!level.isClientSide&&level.getBlockState(pos).getBlock()!=state.getBlock())
            Block.popResource(level,pos,new ItemStack(LegacyWood.BARK.get()));
        return stripped;
    }
}
