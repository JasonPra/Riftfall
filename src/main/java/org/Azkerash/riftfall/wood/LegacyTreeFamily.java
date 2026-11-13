package org.Azkerash.riftfall.wood;

import com.dtteam.dynamictrees.tree.family.UndergroundRootsFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private java.util.function.Supplier<com.dtteam.dynamictrees.block.branch.BranchBlock> woundedBranch;
    @Override public void setupBlocks(){
        super.setupBlocks();woundedBranch=setupBranch(createBranch(getBranchName("wounded_")),true);
    }
    public com.dtteam.dynamictrees.block.branch.BranchBlock getWoundedBranch(){
        // Preserve the existing raw/stripped/root branch indices used by DT's falling-tree data.
        getBranch();getStrippedBranch();getRoots();
        var branch=woundedBranch.get();
        getPrimitiveLog().ifPresent(log->branch.setPrimitiveLogDrops(new ItemStack(log)));
        return branch;
    }

    @Override protected com.dtteam.dynamictrees.block.branch.BranchBlock createBranchBlock(ResourceLocation name){
        LegacyBranchBlock branch=name.getPath().startsWith("wounded_")?new WoundedLegacyBranch(name,getProperties()):new LegacyBranchBlock(name,getProperties());
        if(isFireProof())branch.setFireSpreadSpeed(0).setFlammability(0);
        return branch;
    }

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
        return state.getBlock() instanceof LegacyBranchBlock&&LegacyBarkWound.faces(level,pos)!=63
            &&(player!=null&&player.isCreative()||LegacyLogBlock.canHarvest(tool,state))
            &&super.canStripBranch(state,level,pos,player,tool);
    }

    @Override public boolean onTreeActivated(TreeActivationContext context){
        if(context.hitState().getBlock() instanceof LegacyBranchBlock&&context.heldItem()!=null&&
            context.heldItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP))
            return stripFace(context.level(),context.hitPos(),context.hitResult().getDirection(),context.player(),context.heldItem());
        return super.onTreeActivated(context);
    }

    @Override public boolean stripBranch(BlockState state,Level level,BlockPos pos,Player player,ItemStack tool) {
        // Compatibility callers without a hit result use the player's viewing direction.
        if(player==null)return false;
        return stripFace(level,pos,Direction.getNearest(player.getLookAngle()).getOpposite(),player,tool);
    }

    public boolean stripFace(Level level,BlockPos pos,Direction face,Player player,ItemStack tool){
        var state=level.getBlockState(pos);
        if(!canStripBranch(state,level,pos,player,tool)||(LegacyBarkWound.faces(level,pos)&LegacyBranchBlock.faceBit(face))!=0)return false;
        if(level.isClientSide)return true;
        int faces=LegacyBarkWound.faces(level,pos)|LegacyBranchBlock.faceBit(face);
        var branch=(LegacyBranchBlock)state.getBlock();int radius=branch.getRadius(state);
        var replacement=(faces==63?getStrippedBranch().orElseThrow():getWoundedBranch()).getStateForRadius(radius);
        replacement=replacement.setValue(com.dtteam.dynamictrees.block.branch.BasicBranchBlock.WATERLOGGED,
            state.getValue(com.dtteam.dynamictrees.block.branch.BasicBranchBlock.WATERLOGGED));
        var previousMode=com.dtteam.dynamictrees.block.branch.BranchBlock.destroyMode;
        try{
            com.dtteam.dynamictrees.block.branch.BranchBlock.destroyMode=com.dtteam.dynamictrees.DynamicTrees.DestroyMode.SET_RADIUS;
            if(!state.equals(replacement)&&!level.setBlock(pos,replacement,3))return false;
            if(faces!=63){
                if(!(level.getBlockEntity(pos) instanceof LegacyBarkWound wound))return false;
                wound.setFaces(faces);
            }
        }finally{com.dtteam.dynamictrees.block.branch.BranchBlock.destroyMode=previousMode;}
        branch.damageAxe(player,tool,radius/2,new com.dtteam.dynamictrees.systems.nodemapper.NetVolumeNode.Volume((radius*radius*64)/2),false);
        Block.popResource(level,pos,new ItemStack(LegacyWood.BARK.get()));
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AXE_STRIP,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos);
        return true;
    }
}
