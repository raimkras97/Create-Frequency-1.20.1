package dev.ripiters.create_frequency.common.network;

import dev.ripiters.create_frequency.common.link.FrequencyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigureFrequencyPacket {

    private final BlockPos pos;
    private final float frequency;
    private final String networkName;

    public ConfigureFrequencyPacket(BlockPos pos, float frequency, String networkName) {
        this.pos = pos;
        this.frequency = frequency;
        this.networkName = networkName;
    }

    public static ConfigureFrequencyPacket decode(FriendlyByteBuf buf) {
        return new ConfigureFrequencyPacket(buf.readBlockPos(), buf.readFloat(), buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(frequency);
        buf.writeUtf(networkName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Level level = player.level();

            if (player.blockPosition().distSqr(pos) > 4096) return;

            String finalName = networkName;
            if (finalName.isEmpty()) {
                finalName = FrequencyNetworkHandler.getNetworkName(level, frequency);
            } else {
                FrequencyNetworkHandler.setNetworkName(level, frequency, finalName);
            }

            if (level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof FrequencyBlockEntity frequencyBE) {
                    frequencyBE.setFrequency(frequency);
                    frequencyBE.setNetworkName(finalName);
                    frequencyBE.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}