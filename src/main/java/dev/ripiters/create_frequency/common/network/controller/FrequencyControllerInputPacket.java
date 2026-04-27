package dev.ripiters.create_frequency.common.network.controller;

import dev.ripiters.create_frequency.common.link.controller.FrequencyControllerItem;
import dev.ripiters.create_frequency.common.link.controller.FrequencyControllerServerHandler;
import dev.ripiters.create_frequency.common.link.controller.LecternFrequencyControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.stream.Collectors;

public class FrequencyControllerInputPacket extends FrequencyControllerPacketBase {

    private final Collection<Integer> activatedButtons;
    private final boolean press;

    public FrequencyControllerInputPacket(List<Integer> activatedButtons, boolean press, Optional<BlockPos> lecternPos) {
        super(lecternPos.orElse(null));
        this.activatedButtons = activatedButtons;
        this.press = press;
    }

    public FrequencyControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(new ArrayList<>(activatedButtons), press, Optional.empty());
    }

    public static FrequencyControllerInputPacket decode(FriendlyByteBuf buf) {
        List<Integer> activatedButtons = buf.readList(FriendlyByteBuf::readVarInt);
        boolean press = buf.readBoolean();
        BlockPos lecternPos = buf.readNullable(FriendlyByteBuf::readBlockPos);
        return new FrequencyControllerInputPacket(activatedButtons, press, Optional.ofNullable(lecternPos));
    }

    @Override
    public void encodeData(FriendlyByteBuf buf) {
        buf.writeCollection(new ArrayList<>(activatedButtons), FriendlyByteBuf::writeVarInt);
        buf.writeBoolean(press);
        buf.writeNullable(getLecternPos(), FriendlyByteBuf::writeBlockPos);
    }

    public void encode(FriendlyByteBuf buf) {
        encodeData(buf);
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        if (player.isSpectator() && press) return;

        List<Float> frequencies = activatedButtons.stream()
                .map(i -> FrequencyControllerItem.getBindFrequency(heldItem, i))
                .collect(Collectors.toList());

        FrequencyControllerServerHandler.receivePressed(
                player.level(),
                player.blockPosition(),
                player.getUUID(),
                frequencies,
                press
        );
    }

    @Override
    protected void handleLectern(ServerPlayer player, @UnknownNullability LecternFrequencyControllerBlockEntity lectern) {
        handleItem(player, lectern.getController());
    }
}