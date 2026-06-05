package dev.ripiters.create_frequency.common.link;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.ripiters.create_frequency.client.gui.FrequencyConfigScreen;
import dev.ripiters.create_frequency.common.CFBlocks;
import dev.ripiters.create_frequency.common.CFBlockEntityTypes;
import dev.ripiters.create_frequency.common.CFShapes;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@SuppressWarnings("deprecation")

public class FrequencySignalReceiver extends WrenchableDirectionalBlock implements IBE<FrequencyReceiverBlockEntity> {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FrequencySignalReceiver(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    public int getSignal(@Nonnull BlockState state, @Nonnull BlockGetter blockAccess, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return getBlockEntityOptional(blockAccess, pos).map(FrequencyReceiverBlockEntity::getReceivedSignal)
                .orElse(0);
    }

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        // Wrench in main hand is handled by onWrenched(); do not open GUI.
        if (hand == InteractionHand.MAIN_HAND
                && player.getMainHandItem().getItem() instanceof com.simibubi.create.content.equipment.wrench.WrenchItem) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, FrequencyBlockEntity::refreshNetworkData);
        }

        if (level.isClientSide) {
            distributeGui(pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return FrequencyLinkVariantHelper.toggle(context.getLevel(), context.getClickedPos(), state, CFBlocks.FREQUENCY_TRANSMITTER.get());
    }

    @OnlyIn(Dist.CLIENT)
    private void distributeGui(BlockPos pos) {
        ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof FrequencyBlockEntity be) {
            ScreenOpener.open(new FrequencyConfigScreen(pos, be));
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos, @Nonnull BlockState pNewState, boolean pMovedByPiston) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction _targetedFace) {
        return originalState;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return side != null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        BlockPos neighbourPos = pos.relative(state.getValue(FACING)
                .getOpposite());
        BlockState neighbour = worldIn.getBlockState(neighbourPos);
        return !neighbour.canBeReplaced();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        state = state.setValue(FACING, context.getClickedFace());
        return state;
    }

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return CFShapes.FREQUENCY_BRIDGE.get(state.getValue(FACING));
    }

    @Override
    public boolean isPathfindable(@Nonnull BlockState state, @Nonnull BlockGetter getter, @Nonnull BlockPos pos, @Nonnull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public Class<FrequencyReceiverBlockEntity> getBlockEntityClass() {
        return FrequencyReceiverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FrequencyReceiverBlockEntity> getBlockEntityType() {
        return CFBlockEntityTypes.LINKED_RECEIVER.get();
    }
}