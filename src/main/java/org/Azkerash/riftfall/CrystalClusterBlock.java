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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CrystalClusterBlock extends BaseEntityBlock {
    public static final int MAX_CRYSTALS = 5;
    public static final MapCodec<CrystalClusterBlock> CODEC = simpleCodec(CrystalClusterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty CRYSTALS = IntegerProperty.create("crystals", 1, MAX_CRYSTALS);
    private static final Map<Direction, VoxelShape[]> SHAPES = shapes();

    public CrystalClusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(CRYSTALS, 1));
    }

    @Override protected MapCodec<CrystalClusterBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(FACING, CRYSTALS); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrystalClusterBlockEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING))[state.getValue(CRYSTALS) - 1];
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.setValue(FACING, mirror.mirror(state.getValue(FACING))); }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1F; }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, RiftBlocks.CRYSTAL_CLUSTER_ENTITY.get(), CrystalClusterBlockEntity::tick);
    }

    private static Map<Direction, VoxelShape[]> shapes() {
        double[][] boxes = {{6,0,6,10,14,10},{2,0,6,5,8,9},{11,0,7,14,10,10},{5,0,11,8,7,14},{9,0,2,12,9,5}};
        var result = new EnumMap<Direction, VoxelShape[]>(Direction.class);
        for (Direction facing : Direction.values()) {
            VoxelShape[] stages = new VoxelShape[MAX_CRYSTALS];
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < MAX_CRYSTALS; i++) {
                double[] b = boxes[i], min = {16,16,16}, max = {0,0,0};
                for (double x : new double[]{b[0],b[3]}) for (double y : new double[]{b[1],b[4]}) for (double z : new double[]{b[2],b[5]}) {
                    double[] p = switch (facing) {
                        case UP -> new double[]{x,y,z};
                        case DOWN -> new double[]{x,16-y,16-z};
                        case NORTH -> new double[]{x,z,16-y};
                        case SOUTH -> new double[]{x,16-z,y};
                        case WEST -> new double[]{16-y,z,16-x};
                        case EAST -> new double[]{y,z,x};
                    };
                    for (int j = 0; j < 3; j++) { min[j] = Math.min(min[j],p[j]); max[j] = Math.max(max[j],p[j]); }
                }
                shape = Shapes.or(shape, box(min[0],min[1],min[2],max[0],max[1],max[2]));
                stages[i] = shape;
            }
            result.put(facing, stages);
        }
        return result;
    }
}
