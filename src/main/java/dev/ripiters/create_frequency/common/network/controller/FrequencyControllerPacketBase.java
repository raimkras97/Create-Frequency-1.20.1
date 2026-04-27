package dev.ripiters.create_frequency.common.network.controller;

import dev.ripiters.create_frequency.common.CFItems;
import dev.ripiters.create_frequency.common.link.controller.LecternFrequencyControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class FrequencyControllerPacketBase {
    @Nullable
    private final BlockPos lecternPos;

    public FrequencyControllerPacketBase(@Nullable BlockPos lecternPos) {
        this.lecternPos = lecternPos;
    }

    @Nullable
    public BlockPos getLecternPos() {
        return lecternPos;
    }

    public void handle(ServerPlayer player) {
        if (this.lecternPos != null) {
            BlockEntity be = player.level().getBlockEntity(this.lecternPos);
            if (!(be instanceof LecternFrequencyControllerBlockEntity lectern))
                return;
            handleLectern(player, lectern);
        } else {
            ItemStack controller = player.getMainHandItem();
            if (!CFItems.FREQUENCY_CONTROLLER.isIn(controller)) {
                controller = player.getOffhandItem();
                if (!CFItems.FREQUENCY_CONTROLLER.isIn(controller))
                    return;
            }
            handleItem(player, controller);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) handle(player);
        });
        ctx.get().setPacketHandled(true);
    }

    protected abstract void encodeData(FriendlyByteBuf buf);
    protected abstract void handleItem(ServerPlayer player, ItemStack heldItem);
    protected abstract void handleLectern(ServerPlayer player, LecternFrequencyControllerBlockEntity lectern);
}