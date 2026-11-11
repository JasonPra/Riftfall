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
    public static final IntegerProperty LAYERS=BlockStateProperties.LAYERS;
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty BRANCH_NEXT=BooleanProperty.create("branch_next");
    // Names are model-local directions; they rotate with the attachment face.
    public static final BooleanProperty JOIN_TIP=BooleanProperty.create("join_tip");
    public static final BooleanProperty JOIN_NORTH=BooleanProperty.create("join_north"),JOIN_EAST=BooleanProperty.create("join_east"),JOIN_SOUTH=BooleanProperty.create("join_south"),JOIN_WEST=BooleanProperty.create("join_west");
    public static final java.util.List<BooleanProperty> JOINS=java.util.List.of(JOIN_NORTH,JOIN_EAST,JOIN_SOUTH,JOIN_WEST);
    private final CrystalTier tier;
    private final VoxelShape[][] shapes=new VoxelShape[6][8];
    public ManaCrystalBlock(CrystalTier tier,Properties properties){
        super(properties);this.tier=tier;registerDefaultState(stateDefinition.any().setValue(LAYERS,1).setValue(FACING,Direction.UP).setValue(BRANCH_NEXT,true).setValue(JOIN_NORTH,false).setValue(JOIN_EAST,false).setValue(JOIN_SOUTH,false).setValue(JOIN_WEST,false).setValue(JOIN_TIP,false));
        for(var d:Direction.values())for(int n=1;n<=8;n++){
            double h=n*2;shapes[d.ordinal()][n-1]=switch(d){
                case UP->box(0,0,0,16,h,16);case DOWN->box(0,16-h,0,16,16,16);
                case NORTH->box(0,0,16-h,16,16,16);case SOUTH->box(0,0,0,16,16,h);
                case WEST->box(16-h,0,0,16,16,16);case EAST->box(0,0,0,h,16,16);
            };
        }
    }
    @Override protected MapCodec<? extends Block> codec(){return Block.CODEC;}
    public CrystalTier tier(){return tier;}
    public static boolean adult(BlockState state){return state.getBlock() instanceof ManaCrystalBlock&&state.getValue(LAYERS)==8;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LAYERS,FACING,BRANCH_NEXT,JOIN_NORTH,JOIN_EAST,JOIN_SOUTH,JOIN_WEST,JOIN_TIP);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shapes[state.getValue(FACING).ordinal()][state.getValue(LAYERS)-1];}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var facing=state.getValue(FACING);var host=pos.relative(facing.getOpposite());var stone=level.getBlockState(host);
        return stone.is(ManaCrystals.SUPPORTS)&&stone.isFaceSturdy(level,host,facing);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var old=c.getLevel().getBlockState(c.getClickedPos());
        if(old.is(this)&&old.getValue(LAYERS)<8)return connections(old.setValue(LAYERS,old.getValue(LAYERS)+1).setValue(BRANCH_NEXT,true),c.getLevel(),c.getClickedPos());
        for(var direction:c.getNearestLookingDirections()){
            var state=defaultBlockState().setValue(FACING,direction.getOpposite());if(state.canSurvive(c.getLevel(),c.getClickedPos()))return connections(state,c.getLevel(),c.getClickedPos());
        }return null;
    }
    /** Tangents match the X/Y rotations used by the multipart models. */
    public static Direction tangent(Direction facing,int index){
        if(index<0||index>3)throw new IllegalArgumentException("Invalid crystal tangent");
        if(facing==Direction.UP)return new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[index];
        if(facing==Direction.DOWN)return new Direction[]{Direction.SOUTH,Direction.EAST,Direction.NORTH,Direction.WEST}[index];
        if(index==0)return Direction.DOWN;if(index==2)return Direction.UP;
        Direction right=switch(facing){case NORTH->Direction.EAST;case EAST->Direction.SOUTH;case SOUTH->Direction.WEST;case WEST->Direction.NORTH;default->throw new IllegalStateException();};
        return index==1?right:right.getOpposite();
    }
    private static BlockState neighbor(BlockGetter level,BlockPos pos){
        if(level instanceof ServerLevel server){var chunk=server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);}
        if(level instanceof LevelReader reader&&!reader.hasChunk(pos.getX()>>4,pos.getZ()>>4))return Blocks.AIR.defaultBlockState();
        return level.getBlockState(pos);
    }
    public static BlockState connections(BlockState state,BlockGetter level,BlockPos pos){
        var facing=state.getValue(FACING);
        for(int i=0;i<4;i++){
            var direction=tangent(facing,i);var adjacent=pos.relative(direction);var other=neighbor(level,adjacent);
            boolean joined=false;
            if(other.getBlock() instanceof ManaCrystalBlock){
                var mount=other.getValue(FACING);
                // Parallel surfaces or perpendicular mounts whose actual geometry reaches this seam.
                joined=CrystalSurfaceConnections.edge(facing,direction,mount,adult(other));
            }
            // Convex corner: two clusters on perpendicular faces of the same stone block.
            var around=neighbor(level,adjacent.relative(facing.getOpposite()));
            joined|=around.getBlock() instanceof ManaCrystalBlock&&around.getValue(FACING)==direction;
            state=state.setValue(JOINS.get(i),joined);
        }
        var front=neighbor(level,pos.relative(facing));
        boolean tip=front.getBlock() instanceof ManaCrystalBlock&&CrystalSurfaceConnections.tip(facing,adult(state),front.getValue(FACING));
        return state.setValue(JOIN_TIP,tip);
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
        return direction==state.getValue(FACING).getOpposite()&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():connections(state,level,pos);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);if(level instanceof ServerLevel server){var joined=connections(state,level,pos);if(joined!=state)level.setBlock(pos,joined,Block.UPDATE_ALL);cornerNeighbors(server,pos,state);CrystalRefinement.changed(server,pos);if(!state.canSurvive(level,pos))level.scheduleTick(pos,this,1);}
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
        if(random.nextInt(6)==0){level.setBlock(pos,connections(state.setValue(LAYERS,state.getValue(LAYERS)+1).setValue(BRANCH_NEXT,true),level,pos),Block.UPDATE_ALL);CrystalRefinement.changed(level,pos);}
    }
    @Override protected boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override protected int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 0;}
}
