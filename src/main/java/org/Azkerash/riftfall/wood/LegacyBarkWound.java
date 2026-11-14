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

/** Sparse surface-patch wound data, saved/synchronized only on wounded segments. */
public final class LegacyBarkWound extends BlockEntity {
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"riftfall");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<LegacyBarkWound>> TYPE=TYPES.register("legacy_bark_wound",()->
        BlockEntityType.Builder.of(LegacyBarkWound::new,((LegacyTreeFamily)com.dtteam.dynamictrees.tree.family.Family.REGISTRY.get(LegacyTrees.ID)).getWoundedBranch()).build(null));
    private int faces;
    private long patches;
    private int sections;
    private int loadedTicks;
    private final int[] healingTicks=new int[54];
    /** Stage masks are immutable model data; unchanged stages do not rebuild chunk meshes. */
    public record Visual(long sap,long coating,long resin,long closingEarly,long closingMiddle,long closingLate){
        public static final Visual EMPTY=new Visual(0,0,0,0,0,0);
        public int stage(long patch){
            if((closingLate&patch)!=0)return 6;if((closingMiddle&patch)!=0)return 5;
            if((closingEarly&patch)!=0)return 4;if((resin&patch)!=0)return 3;
            if((coating&patch)!=0)return 2;return (sap&patch)!=0?1:0;
        }
    }
    public static Visual visual(BlockGetter world,BlockPos pos){return world.getBlockEntity(pos) instanceof LegacyBarkWound wound?wound.visual():Visual.EMPTY;}
    public Visual visual(){
        long[] masks=new long[7];long active=combinedPatches();
        for(int bit=0;bit<54;bit++)if((active&(1L<<bit))!=0){int stage=HeartwoodWoundHealing.stage(healingTicks[bit]);if(stage<7)masks[stage]|=1L<<bit;}
        return new Visual(masks[1],masks[2],masks[3],masks[4],masks[5],masks[6]);
    }
    private long combinedPatches(){long active=patches;for(Direction face:Direction.values())if((faces&(1<<face.get3DDataValue()))!=0)active|=facePatches(face);return active;}
    public static void tick(net.minecraft.world.level.Level world,BlockPos pos,BlockState state,LegacyBarkWound wound){
        if(world instanceof net.minecraft.server.level.ServerLevel server&&++wound.loadedTicks>=200){wound.loadedTicks=0;wound.advanceLoaded(server,200);}
    }
    public boolean advanceLoaded(net.minecraft.server.level.ServerLevel server,int ticks){
        if(ticks<=0||!HeartwoodWoundHealing.hasLivingRoot(server,worldPosition))return false;
        var previous=visual();long active=combinedPatches();faces=0;patches=active;
        for(int bit=0;bit<54;bit++)if((active&(1L<<bit))!=0){
            healingTicks[bit]=(int)Math.min(HeartwoodWoundHealing.COMPLETE_TICKS,(long)healingTicks[bit]+ticks);
            if(healingTicks[bit]>=HeartwoodWoundHealing.COMPLETE_TICKS){patches&=~(1L<<bit);healingTicks[bit]=0;}
        }
        setChanged();
        if(patches==0){restoreBark(server);return true;}
        if(active!=patches||!previous.equals(visual()))setFaces(0);
        return true;
    }
    private boolean wet(int bit){int stage=HeartwoodWoundHealing.stage(healingTicks[bit]);return stage==1||stage==2;}
    public boolean hasLiquidSap(long patch){
        long selected=combinedPatches()&patch;for(int bit=0;bit<54;bit++)if((selected&(1L<<bit))!=0&&wet(bit))return true;return false;
    }
    public boolean collectSap(long patch){
        long selected=combinedPatches()&patch;
        for(int bit=0;bit<54;bit++)if((selected&(1L<<bit))!=0&&wet(bit)){
            healingTicks[bit]=0;setFaces(faces);return true;
        }
        return false;
    }
    public String healingReport(){
        var counts=new int[7];int least=HeartwoodWoundHealing.COMPLETE_TICKS,most=0;long active=combinedPatches();
        for(int bit=0;bit<54;bit++)if((active&(1L<<bit))!=0){int age=healingTicks[bit];counts[Math.min(6,HeartwoodWoundHealing.stage(age))]++;least=Math.min(least,age);most=Math.max(most,age);}
        if(active==0)return "Heartwood segment has no open bark patches.";
        var text=new StringBuilder("Heartwood wound: ");
        for(int stage=0;stage<7;stage++)if(counts[stage]>0){int[] times={0,6000,12000,24000,48000,56400,64800};text.append(counts[stage]).append(" patch(es) ").append(HeartwoodWoundHealing.name(times[stage])).append("; ");}
        return text.append("loaded living-tree age ").append(least/1200).append("–").append(most/1200).append(" minutes. Full cycle: 60 minutes.").toString();
    }
    private void restoreBark(net.minecraft.server.level.ServerLevel server){
        var state=getBlockState();var old=(LegacyBranchBlock)state.getBlock();
        var normal=old.getFamily().getBranch().orElseThrow().getStateForRadius(old.getRadius(state))
            .setValue(com.dtteam.dynamictrees.block.branch.BasicBranchBlock.WATERLOGGED,state.getValue(com.dtteam.dynamictrees.block.branch.BasicBranchBlock.WATERLOGGED));
        var mode=com.dtteam.dynamictrees.block.branch.BranchBlock.destroyMode;
        try{com.dtteam.dynamictrees.block.branch.BranchBlock.destroyMode=com.dtteam.dynamictrees.DynamicTrees.DestroyMode.SET_RADIUS;server.setBlock(worldPosition,normal,3);}
        finally{com.dtteam.dynamictrees.block.branch.BranchBlock.destroyMode=mode;}
    }
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
    public void setPatches(long value){
        for(int bit=0;bit<54;bit++)if((value&(1L<<bit))==0||(patches&(1L<<bit))==0){healingTicks[bit]=0;}
        patches=value;setFaces(faces);
    }
    public LegacyBarkWound(BlockPos pos,BlockState state){super(TYPE.get(),pos,state);}
    public static void register(IEventBus bus){TYPES.register(bus);}
    public static int faces(BlockGetter world,BlockPos pos){
        if(world.getBlockState(pos).getBlock() instanceof LegacyBranchBlock branch&&branch.isStrippedBranch())return 63;
        return world.getBlockEntity(pos) instanceof LegacyBarkWound wound?wound.faces:0;
    }
    public int faces(){return faces;}
    public void setFaces(int value){
        int added=(value&63)&~faces;
        for(Direction face:Direction.values())if((added&(1<<face.get3DDataValue()))!=0)
            for(int bit=face.get3DDataValue()*9;bit<face.get3DDataValue()*9+9;bit++){healingTicks[bit]=0;}
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
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("StrippedFaces",faces);tag.putLong("StrippedPatches",patches);tag.putInt("StrippingSections",sections);tag.putIntArray("HealingTicks",healingTicks);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);faces=tag.getInt("StrippedFaces")&63;patches=tag.getLong("StrippedPatches");sections=Math.clamp(tag.getInt("StrippingSections"),0,3);migrateGridPatches();java.util.Arrays.fill(healingTicks,0);var saved=tag.getIntArray("HealingTicks");
        for(int bit=0;bit<Math.min(saved.length,healingTicks.length);bit++)healingTicks[bit]=Math.clamp(saved[bit],0,HeartwoodWoundHealing.COMPLETE_TICKS);
        requestModelDataUpdate();
        if(level!=null&&level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
