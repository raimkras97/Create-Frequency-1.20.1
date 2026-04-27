package dev.ripiters.create_frequency.common.network.controller;

import java.util.Objects;

import dev.ripiters.create_frequency.common.link.controller.LecternFrequencyControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FrequencyControllerStopLecternPacket extends FrequencyControllerPacketBase {

    public FrequencyControllerStopLecternPacket(BlockPos lecternPos) {
        super(Objects.requireNonNull(lecternPos));
    }

    public static FrequencyControllerStopLecternPacket decode(FriendlyByteBuf buf) {
        return new FrequencyControllerStopLecternPacket(buf.readBlockPos());
    }

    @Override
    public void encodeData(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(getLecternPos()));
    }

    public void encode(FriendlyByteBuf buf) {
        encodeData(buf);
    }

    @Override
    protected void handleLectern(ServerPlayer player, LecternFrequencyControllerBlockEntity lectern) {
        lectern.tryStopUsing(player);
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) { }
}
