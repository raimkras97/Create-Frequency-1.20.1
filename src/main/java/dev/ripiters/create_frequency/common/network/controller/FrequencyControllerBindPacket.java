package dev.ripiters.create_frequency.common.network.controller;

import dev.ripiters.create_frequency.common.CFDataComponents;
import dev.ripiters.create_frequency.common.link.controller.LecternFrequencyControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FrequencyControllerBindPacket extends FrequencyControllerPacketBase {

    private final List<Float> frequencies;
    private final int singleButtonIndex;

    public FrequencyControllerBindPacket(List<Float> frequencies, Optional<BlockPos> lecternPos) {
        super(lecternPos.orElse(null));
        this.frequencies = frequencies;
        this.singleButtonIndex = -1;
    }

    public FrequencyControllerBindPacket(int button, float frequency, Optional<BlockPos> lecternPos) {
        super(lecternPos.orElse(null));
        this.frequencies = List.of(frequency);
        this.singleButtonIndex = button;
    }

    private FrequencyControllerBindPacket(List<Float> frequencies, int singleButtonIndex, Optional<BlockPos> lecternPos) {
        super(lecternPos.orElse(null));
        this.frequencies = frequencies;
        this.singleButtonIndex = singleButtonIndex;
    }

    public static FrequencyControllerBindPacket decode(FriendlyByteBuf buf) {
        List<Float> frequencies = buf.readList(FriendlyByteBuf::readFloat);
        int singleButtonIndex = buf.readVarInt();
        BlockPos lecternPos = buf.readNullable(FriendlyByteBuf::readBlockPos);
        return new FrequencyControllerBindPacket(frequencies, singleButtonIndex, Optional.ofNullable(lecternPos));
    }

    @Override
    public void encodeData(FriendlyByteBuf buf) {
        buf.writeCollection(frequencies, FriendlyByteBuf::writeFloat);
        buf.writeVarInt(singleButtonIndex);
        buf.writeNullable(getLecternPos(), FriendlyByteBuf::writeBlockPos);
    }

    public void encode(FriendlyByteBuf buf) {
        encodeData(buf);
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        if (singleButtonIndex == -1) {
            CFDataComponents.setBinds(heldItem, frequencies);
        } else {
            List<Float> current = new ArrayList<>(CFDataComponents.getBinds(heldItem));
            if (current.size() < 6) {
                while (current.size() < 6) current.add(0f);
            }
            if (singleButtonIndex >= 0 && singleButtonIndex < 6) {
                float newFreq = frequencies.get(0);
                current.set(singleButtonIndex, newFreq < 0 ? 0f : newFreq);
                CFDataComponents.setBinds(heldItem, current);
            }
        }
    }

    @Override
    protected void handleLectern(ServerPlayer player, @UnknownNullability LecternFrequencyControllerBlockEntity lectern) {
        ItemStack controller = lectern.getController();
        if (!controller.isEmpty()) {
            handleItem(player, controller);
            lectern.setChanged();
        }
    }
}