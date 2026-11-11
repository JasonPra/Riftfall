package org.Azkerash.riftfall.crystals;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import org.Azkerash.riftfall.worldmana.WorldMana;

/** Raw crystal layers attach to stone, including walls/ceilings; no crystal-supported towers. */
public final class ManaCrystalBlock extends Block {
    public static final IntegerProperty VARIANT=IntegerProperty.create("variant",0,5);
    public static final IntegerProperty LAYERS=BlockStateProperties.LAYERS;
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty BRANCH_NEXT=BooleanProperty.create("branch_next");
    // Names are model-local directions; they rotate with the attachment face.
    public static final BooleanProperty JOIN_NORTH=BooleanProperty.create("join_north"),JOIN_EAST=BooleanProperty.create("join_east"),JOIN_SOUTH=BooleanProperty.create("join_south"),JOIN_WEST=BooleanProperty.create("join_west");
    public static final java.util.List<BooleanProperty> JOINS=java.util.List.of(JOIN_NORTH,JOIN_EAST,JOIN_SOUTH,JOIN_WEST);
    private final CrystalTier tier;
    private final VoxelShape[][] shapes=new VoxelShape[6][8];
    public ManaCrystalBlock(CrystalTier tier,Properties properties){
        super(properties);this.tier=tier;registerDefaultState(stateDefinition.any().setValue(VARIANT,0).setValue(LAYERS,1).setValue(FACING,Direction.UP).setValue(BRANCH_NEXT,true).setValue(JOIN_NORTH,false).setValue(JOIN_EAST,false).setValue(JOIN_SOUTH,false).setValue(JOIN_WEST,false));
        for(var d:Direction.values())for(int n=1;n<=8;n++){
            double h=n*2;shapes[d.ordinal()][n-1]=switch(d){
                case UP->box(0,0,0,16,h,16);case DOWN->box(0,16-h,0,16,16,16);
                case NORTH->box(0,0,16-h,16,16,16);case SOUTH->box(0,0,0,16,16,h);
                case WEST->box(16-h,0,0,16,16,16);case EAST->box(0,0,0,h,16,16);
            };
        }
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        // Vanilla invokes this only near the client camera; no server ticking or packets.
        if(random.nextInt(18)!=0)return;
        int mask=surfaceMask(level,pos,state);int selected=random.nextInt(Integer.bitCount(mask));Direction face=state.getValue(FACING);
        for(var candidate:Direction.values())if(CrystalSurfaceConnections.includes(mask,candidate)&&selected--==0){face=candidate;break;}
        int layers=state.getValue(LAYERS);double height=(face==state.getValue(FACING)?layers:(layers+1)/2)/8.0;
        double u=.15+random.nextDouble()*.7,v=.15+random.nextDouble()*.7,w=.08+random.nextDouble()*height*.85;
        double x=u,y=w,z=v;
        switch(face){
            case DOWN->y=1-w;
            case NORTH->{y=v;z=1-w;}
            case SOUTH->{y=v;z=w;}
            case WEST->{x=1-w;y=u;z=v;}
            case EAST->{x=w;y=u;z=v;}
            default->{}
        }
        int color=tier.glowColor();
        level.addParticle(CrystalParticles.SPARKLE.get(),pos.getX()+x,pos.getY()+y,pos.getZ()+z,
            .3+.7*((color>>16)&255)/255.0,.3+.7*((color>>8)&255)/255.0,.3+.7*(color&255)/255.0);
    }
    @Override protected MapCodec<? extends Block> codec(){return Block.CODEC;}
    public CrystalTier tier(){return tier;}
    public static boolean adult(BlockState state){return state.getBlock() instanceof ManaCrystalBlock&&state.getValue(LAYERS)==8;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(VARIANT,LAYERS,FACING,BRANCH_NEXT,JOIN_NORTH,JOIN_EAST,JOIN_SOUTH,JOIN_WEST);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){var primary=state.getValue(FACING);int layers=state.getValue(LAYERS),mask=surfaceMask(level,pos,state);var shape=shapes[primary.ordinal()][layers-1];
        for(var face:Direction.values())if(face!=primary&&CrystalSurfaceConnections.includes(mask,face))shape=Shapes.or(shape,shapes[face.ordinal()][(layers-1)/2]);
        return shape;}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var facing=state.getValue(FACING);var host=pos.relative(facing.getOpposite());var stone=level.getBlockState(host);
        return stone.is(ManaCrystals.SUPPORTS)&&stone.isFaceSturdy(level,host,facing);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var old=c.getLevel().getBlockState(c.getClickedPos());
        if(old.is(this)&&old.getValue(LAYERS)<8)return connections(old.setValue(VARIANT,c.getLevel().random.nextInt(6)).setValue(LAYERS,old.getValue(LAYERS)+1).setValue(BRANCH_NEXT,true),c.getLevel(),c.getClickedPos());
        for(var direction:c.getNearestLookingDirections()){
            var state=defaultBlockState().setValue(FACING,direction.getOpposite());if(state.canSurvive(c.getLevel(),c.getClickedPos()))return connections(state,c.getLevel(),c.getClickedPos());
        }return null;
    }
    public static Direction tangent(Direction facing,int index){return CrystalSurfaceConnections.tangent(facing,index);}
    private static BlockState neighbor(BlockGetter level,BlockPos pos){
        if(level instanceof ServerLevel server){var chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);}
        if(level instanceof LevelReader reader&&!reader.hasChunk(pos.getX()>>4,pos.getZ()>>4))return Blocks.AIR.defaultBlockState();
        return level.getBlockState(pos);
    }
    /** Every sturdy solid contact contributes a surface; the primary face retains its larger stems. */
    public static int surfaceMask(BlockGetter level,BlockPos pos,BlockState state){
        int mask=CrystalSurfaceConnections.bit(state.getValue(FACING));
        for(var face:Direction.values()){
            var host=pos.relative(face.getOpposite());var solid=neighbor(level,host);
            if(!(solid.getBlock() instanceof ManaCrystalBlock)&&solid.isFaceSturdy(level,host,face))mask|=CrystalSurfaceConnections.bit(face);
        }return mask;
    }
    public static int surfaceJoins(BlockGetter level,BlockPos pos,Direction face){
        var own=neighbor(level,pos);int surfaces=own.getBlock() instanceof ManaCrystalBlock?surfaceMask(level,pos,own):0;
        return CrystalSurfaceConnections.joins(face,surfaces,offset->{
            var adjacent=pos.offset(offset.x(),offset.y(),offset.z());var other=neighbor(level,adjacent);
            return other.getBlock() instanceof ManaCrystalBlock?surfaceMask(level,adjacent,other):0;
        });
    }
    public static BlockState connections(BlockState state,BlockGetter level,BlockPos pos){
        int joins=surfaceJoins(level,pos,state.getValue(FACING));
        for(int i=0;i<4;i++)state=state.setValue(JOINS.get(i),(joins&(1<<i))!=0);
        return state;
    }
    private static void cornerNeighbors(ServerLevel level,BlockPos pos,BlockState state){
        for(int i=0;i<4;i++){
            var adjacent=pos.relative(tangent(state.getValue(FACING),i)).relative(state.getValue(FACING).getOpposite());
            if(level.getChunkSource().getChunkNow(adjacent.getX()>>4,adjacent.getZ()>>4)==null)continue;
            var old=level.getBlockState(adjacent);if(old.getBlock() instanceof ManaCrystalBlock){var next=connections(old,level,adjacent);if(old!=next)level.setBlock(adjacent,next,Block.UPDATE_CLIENTS);}
        }
    }
    /** Restores connections for pre-existing saves and both sides of newly loaded chunk seams. */
    public static void refreshConnections(net.minecraft.world.level.chunk.LevelChunk chunk){
        if(!(chunk.getLevel() instanceof ServerLevel level))return;
        var cp=chunk.getPos();var sections=chunk.getSections();var positions=new java.util.ArrayList<BlockPos>();
        for(int section=0;section<sections.length;section++){
            var data=sections[section];if(!data.maybeHas(s->s.getBlock() instanceof ManaCrystalBlock))continue;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++)if(data.getBlockState(x,y,z).getBlock() instanceof ManaCrystalBlock)positions.add(new BlockPos(cp.getMinBlockX()+x,chunk.getMinBuildHeight()+section*16+y,cp.getMinBlockZ()+z));
        }
        for(var pos:positions){
            var state=level.getBlockState(pos);var joined=connections(state,level,pos);if(joined!=state)level.setBlock(pos,joined,Block.UPDATE_CLIENTS);
            cornerNeighbors(level,pos,state);
            for(var direction:Direction.values()){
                var adjacent=pos.relative(direction);if(level.getChunkSource().getChunkNow(adjacent.getX()>>4,adjacent.getZ()>>4)==null)continue;
                var old=level.getBlockState(adjacent);if(old.getBlock() instanceof ManaCrystalBlock){var next=connections(old,level,adjacent);if(old!=next)level.setBlock(adjacent,next,Block.UPDATE_CLIENTS);}
            }
        }
    }
    @Override protected boolean canBeReplaced(BlockState state,BlockPlaceContext c){return c.getItemInHand().is(asItem())&&state.getValue(LAYERS)<8;}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){
        if(!state.canSurvive(level,pos)){
            for(var face:Direction.values()){var candidate=state.setValue(FACING,face);if(candidate.canSurvive(level,pos))return connections(candidate,level,pos);}
            return Blocks.AIR.defaultBlockState();
        }return connections(state,level,pos);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);if(level instanceof ServerLevel server){if(old.getBlock()!=this)state=state.setValue(VARIANT,server.random.nextInt(6));var joined=connections(state,level,pos);if(joined!=level.getBlockState(pos))level.setBlock(pos,joined,Block.UPDATE_ALL);cornerNeighbors(server,pos,state);CrystalRefinement.changed(server,pos);if(!state.canSurvive(level,pos))level.scheduleTick(pos,this,1);}
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(level instanceof ServerLevel server&&state.getBlock()!=next.getBlock()){CrystalRefinement.changing(server,pos);cornerNeighbors(server,pos,state);}
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);if(level instanceof ServerLevel server)CrystalRefinement.changed(server,pos);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!state.canSurvive(level,pos))level.destroyBlock(pos,true);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null||!CrystalRules.growth(WorldMana.inspect(chunk).amount()))return;
        if(state.getValue(BRANCH_NEXT)||state.getValue(LAYERS)==8){
            CrystalGrowth.spread(level,pos,state,random);
            if(state.getValue(BRANCH_NEXT)){var current=level.getBlockState(pos);if(current.is(this))level.setBlock(pos,current.setValue(BRANCH_NEXT,false),Block.UPDATE_CLIENTS);}
            return;
        }
        if(random.nextInt(6)==0){level.setBlock(pos,connections(state.setValue(VARIANT,random.nextInt(6)).setValue(LAYERS,state.getValue(LAYERS)+1).setValue(BRANCH_NEXT,true),level,pos),Block.UPDATE_ALL);CrystalRefinement.changed(level,pos);}
    }
    @Override protected boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override protected int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 0;}
}
