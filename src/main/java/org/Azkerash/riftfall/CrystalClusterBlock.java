package org.Azkerash.riftfall;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CrystalClusterBlock extends BaseEntityBlock {
    public static final int MAX_CRYSTALS = 5;
    public static final MapCodec<CrystalClusterBlock> CODEC = simpleCodec(CrystalClusterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty CRYSTALS = IntegerProperty.create("crystals", 1, MAX_CRYSTALS);
    // Local surface directions, rotated with FACING by the multipart models.
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    private static final BooleanProperty[] CONNECTIONS = {NORTH, EAST, SOUTH, WEST};
    private static final Direction[] TANGENTS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final Map<Direction, VoxelShape[]> SHAPES = shapes();

    public CrystalClusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(CRYSTALS, 1)
            .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override protected MapCodec<CrystalClusterBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING, CRYSTALS, NORTH, EAST, SOUTH, WEST); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrystalClusterBlockEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING))[state.getValue(CRYSTALS) - 1];
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState().setValue(FACING, context.getClickedFace()), context.getLevel(), context.getClickedPos());
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return connections(state, level, pos);
    }

    static Direction tangent(Direction facing, Direction local) {
        if (facing == Direction.UP) return local;
        if (facing == Direction.DOWN) return local.getAxis() == Direction.Axis.Z ? local.getOpposite() : local;
        if (local == Direction.NORTH) return facing == Direction.SOUTH ? Direction.UP : Direction.DOWN;
        if (local == Direction.SOUTH) return facing == Direction.SOUTH ? Direction.DOWN : Direction.UP;
        Direction east = switch (facing) {
            case WEST -> Direction.NORTH;
            case EAST -> Direction.SOUTH;
            default -> Direction.EAST;
        };
        return local == Direction.EAST ? east : east.getOpposite();
    }

    static BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < TANGENTS.length; i++) {
            BlockState neighbor = level.getBlockState(pos.relative(tangent(facing, TANGENTS[i])));
            state = state.setValue(CONNECTIONS[i], neighbor.is(state.getBlock()) && neighbor.getValue(FACING) == facing);
        }
        return state;
    }

    private BlockState transform(BlockState state, java.util.function.UnaryOperator<Direction> transform) {
        Direction oldFacing = state.getValue(FACING), facing = transform.apply(oldFacing);
        BlockState result = state.setValue(FACING, facing);
        for (int i = 0; i < TANGENTS.length; i++) {
            Direction target = transform.apply(tangent(oldFacing, TANGENTS[i]));
            for (int j = 0; j < TANGENTS.length; j++)
                if (tangent(facing, TANGENTS[j]) == target) result = result.setValue(CONNECTIONS[j], state.getValue(CONNECTIONS[i]));
        }
        return result;
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return transform(state, rotation::rotate); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return transform(state, mirror::mirror); }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1F; }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, RiftBlocks.CRYSTAL_CLUSTER_ENTITY.get(), CrystalClusterBlockEntity::tick);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        int count=state.getValue(CRYSTALS);
        // Client-only atmosphere; dense colonies should never become particle storms.
        if (random.nextInt(100)>=count*2) return;
        var local=new net.minecraft.world.phys.Vec3(6+random.nextDouble()*4,2+count*1.1,6+random.nextDouble()*4);
        var point=CrystalFormation.orient(local,state.getValue(FACING)).scale(1.0/16);
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL,
            pos.getX()+point.x,pos.getY()+point.y,pos.getZ()+point.z,
            (random.nextDouble()-.5)*.008,.012,(random.nextDouble()-.5)*.008);
    }

    private static Map<Direction, VoxelShape[]> shapes() {
        var result=new EnumMap<Direction,VoxelShape[]>(Direction.class);
        for (Direction facing : Direction.values()) {
            VoxelShape[] stages=new VoxelShape[MAX_CRYSTALS];
            for (int count=1;count<=MAX_CRYSTALS;count++) {
                VoxelShape shape=Shapes.empty();
                for (int crystal=0;crystal<count;crystal++) {
                    double[] min={16,16,16},max={0,0,0};
                    for (int variant=0;variant<3;variant++) {
                        var prism=CrystalFormation.crystals(count,variant).get(crystal);
                        var points=new java.util.ArrayList<net.minecraft.world.phys.Vec3>();
                        points.add(prism.tip());
                        for (int i=0;i<6;i++) { points.add(prism.vertex(i,false)); points.add(prism.vertex(i,true)); }
                        for (var raw : points) {
                            var p=CrystalFormation.orient(raw,facing);
                            double[] coords={p.x,p.y,p.z};
                            for (int j=0;j<3;j++) { min[j]=Math.min(min[j],coords[j]); max[j]=Math.max(max[j],coords[j]); }
                        }
                    }
                    shape=Shapes.or(shape,box(min[0],min[1],min[2],max[0],max[1],max[2]));
                }
                stages[count-1]=shape;
            }
            result.put(facing,stages);
        }
        return result;
    }
}
