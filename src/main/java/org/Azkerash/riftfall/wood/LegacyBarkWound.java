package org.Azkerash.riftfall.wood;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Sparse per-face wound data, saved/synchronized only on partially stripped segments. */
public final class LegacyBarkWound extends BlockEntity {
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"riftfall");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<LegacyBarkWound>> TYPE=TYPES.register("legacy_bark_wound",()->
        BlockEntityType.Builder.of(LegacyBarkWound::new,((LegacyTreeFamily)com.dtteam.dynamictrees.tree.family.Family.REGISTRY.get(LegacyTrees.ID)).getWoundedBranch()).build(null));
    private int faces;
    public LegacyBarkWound(BlockPos pos,BlockState state){super(TYPE.get(),pos,state);}
    public static void register(IEventBus bus){TYPES.register(bus);}
    public static int faces(BlockGetter world,BlockPos pos){
        if(world.getBlockState(pos).getBlock() instanceof LegacyBranchBlock branch&&branch.isStrippedBranch())return 63;
        return world.getBlockEntity(pos) instanceof LegacyBarkWound wound?wound.faces:0;
    }
    public int faces(){return faces;}
    public void setFaces(int value){
        faces=value&63;setChanged();requestModelDataUpdate();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("StrippedFaces",faces);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);faces=tag.getInt("StrippedFaces")&63;requestModelDataUpdate();
        if(level!=null&&level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
