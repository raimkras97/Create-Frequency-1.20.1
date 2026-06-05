package dev.ripiters.create_frequency.common.link;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FrequencyLinkVariantHelper {

    private FrequencyLinkVariantHelper() {
    }

    public static InteractionResult toggle(Level level, BlockPos pos, BlockState state, Block targetBlock) {
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        String networkName = "";
        float frequency = 0.0f;

        if (level.getBlockEntity(pos) instanceof FrequencyBlockEntity be) {
            networkName = be.getNetworkName();
            frequency = be.getFrequency();
        }

        BlockState newState = targetBlock.defaultBlockState()
                .setValue(BlockStateProperties.FACING, state.getValue(BlockStateProperties.FACING));

        if (state.hasProperty(BlockStateProperties.POWERED)) {
            newState = newState.setValue(BlockStateProperties.POWERED, state.getValue(BlockStateProperties.POWERED));
        }

        level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

        if (level.getBlockEntity(pos) instanceof FrequencyBlockEntity newBe) {
            newBe.setFrequency(frequency);
            newBe.setNetworkName(networkName);
        }

        return InteractionResult.SUCCESS;
    }
}
