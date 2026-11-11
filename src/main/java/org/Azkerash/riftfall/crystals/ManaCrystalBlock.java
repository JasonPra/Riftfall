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
    private final CrystalTier tier;
    private final VoxelShape[][] shapes=new VoxelShape[6][8];
    public ManaCrystalBlock(CrystalTier tier,Properties properties){
        super(properties);this.tier=tier;registerDefaultState(stateDefinition.any().setValue(LAYERS,1).setValue(FACING,Direction.UP).setValue(BRANCH_NEXT,true));
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
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LAYERS,FACING,BRANCH_NEXT);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shapes[state.getValue(FACING).ordinal()][state.getValue(LAYERS)-1];}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var facing=state.getValue(FACING);var host=pos.relative(facing.getOpposite());var stone=level.getBlockState(host);
        return stone.is(ManaCrystals.SUPPORTS)&&stone.isFaceSturdy(level,host,facing);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var old=c.getLevel().getBlockState(c.getClickedPos());
        if(old.is(this)&&old.getValue(LAYERS)<8)return old.setValue(LAYERS,old.getValue(LAYERS)+1).setValue(BRANCH_NEXT,true);
        for(var direction:c.getNearestLookingDirections()){
            var state=defaultBlockState().setValue(FACING,direction.getOpposite());if(state.canSurvive(c.getLevel(),c.getClickedPos()))return state;
        }return null;
    }
    @Override protected boolean canBeReplaced(BlockState state,BlockPlaceContext c){return c.getItemInHand().is(asItem())&&state.getValue(LAYERS)<8;}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){
        return direction==state.getValue(FACING).getOpposite()&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():state;
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);if(level instanceof ServerLevel server){CrystalRefinement.changed(server,pos);if(!state.canSurvive(level,pos))level.scheduleTick(pos,this,1);}
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(level instanceof ServerLevel server&&state.getBlock()!=next.getBlock())CrystalRefinement.changing(server,pos);
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
            if(state.getValue(BRANCH_NEXT))level.setBlock(pos,state.setValue(BRANCH_NEXT,false),Block.UPDATE_CLIENTS);
            return;
        }
        if(random.nextInt(6)==0){level.setBlock(pos,state.setValue(LAYERS,state.getValue(LAYERS)+1).setValue(BRANCH_NEXT,true),Block.UPDATE_ALL);CrystalRefinement.changed(level,pos);}
    }
    @Override protected boolean skipRendering(BlockState state,BlockState adjacent,Direction side){
        if(adjacent.getBlock() instanceof ManaCrystalBlock&&state.getValue(FACING)==adjacent.getValue(FACING)
                &&side.getAxis()!=state.getValue(FACING).getAxis()&&adjacent.getValue(LAYERS)>=state.getValue(LAYERS))return true;
        return super.skipRendering(state,adjacent,side);
    }
    @Override protected boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override protected int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 0;}
}
