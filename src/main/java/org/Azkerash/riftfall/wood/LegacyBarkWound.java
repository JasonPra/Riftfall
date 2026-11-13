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

/** Sparse surface-patch wound data, saved/synchronized only on partially stripped segments. */
public final class LegacyBarkWound extends BlockEntity {
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"riftfall");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<LegacyBarkWound>> TYPE=TYPES.register("legacy_bark_wound",()->
        BlockEntityType.Builder.of(LegacyBarkWound::new,((LegacyTreeFamily)com.dtteam.dynamictrees.tree.family.Family.REGISTRY.get(LegacyTrees.ID)).getWoundedBranch()).build(null));
    private int faces;
    private long patches;
    private int sections;
    public static int sectionCount(int radius){return radius<=8?1:radius<24?2:3;}
    public static int sections(BlockGetter world,BlockPos pos){return world.getBlockEntity(pos) instanceof LegacyBarkWound wound?wound.sections:0;}
    public void setSections(int value){sections=Math.clamp(value,0,3);}
    public static long patch(Direction face,double x,double y,double z,int radius,int sections){
        if(sections==0)return patch(face,x,y,z);
        double u=face.getAxis()==Direction.Axis.X?z:x;
        double minimum=.5-radius/16.0,width=radius/8.0;
        int column=Math.clamp((int)Math.floor((u-minimum)*sections/width),0,sections-1);
        return 1L<<(face.get3DDataValue()*9+3+column);
    }
    public static long facePatches(Direction face){return 511L<<(face.get3DDataValue()*9);}
    public static long patch(Direction face,double x,double y,double z){
        double u=face.getAxis()==Direction.Axis.X?z:x;
        double v=face.getAxis()==Direction.Axis.Y?z:y;
        int a=Math.clamp((int)Math.floor(u)+1,0,2),b=Math.clamp((int)Math.floor(v)+1,0,2);
        return 1L<<(face.get3DDataValue()*9+b*3+a);
    }
    public static long patches(BlockGetter world,BlockPos pos){
        long result=world.getBlockEntity(pos) instanceof LegacyBarkWound wound?wound.patches:0;
        int faces=faces(world,pos);
        for(Direction face:Direction.values())if((faces&(1<<face.get3DDataValue()))!=0)result|=facePatches(face);
        return result;
    }
    public void setPatches(long value){patches=value;setFaces(faces);}
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
    private void migrateGridPatches(){
        if(sections!=0||!(getBlockState().getBlock() instanceof LegacyBranchBlock branch))return;
        int radius=branch.getRadius(getBlockState());sections=sectionCount(radius);
        long converted=patches;double low=.5-radius/16.0,width=radius/8.0;
        for(Direction face:Direction.Plane.HORIZONTAL){
            converted&=~facePatches(face);
            for(int old=0;old<3;old++)if((patches&(1L<<(face.get3DDataValue()*9+3+old)))!=0)
                for(int next=0;next<sections;next++){
                    double a=low+width*next/sections,b=low+width*(next+1)/sections;
                    if(Math.min(b,old)-Math.max(a,old-1)>1e-6)converted|=1L<<(face.get3DDataValue()*9+3+next);
                }
        }
        patches=converted;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("StrippedFaces",faces);tag.putLong("StrippedPatches",patches);tag.putInt("StrippingSections",sections);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);faces=tag.getInt("StrippedFaces")&63;patches=tag.getLong("StrippedPatches");sections=Math.clamp(tag.getInt("StrippingSections"),0,3);migrateGridPatches();requestModelDataUpdate();
        if(level!=null&&level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
