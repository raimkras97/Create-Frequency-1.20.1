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
    private final boolean keepExistingNameWhenBlank;

    public ConfigureFrequencyPacket(BlockPos pos, float frequency, String networkName, boolean keepExistingNameWhenBlank) {
        this.pos = pos;
        this.frequency = frequency;
        this.networkName = networkName;
        this.keepExistingNameWhenBlank = keepExistingNameWhenBlank;
    }

    public static ConfigureFrequencyPacket decode(FriendlyByteBuf buf) {
        return new ConfigureFrequencyPacket(buf.readBlockPos(), buf.readFloat(), buf.readUtf(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(frequency);
        buf.writeUtf(networkName);
        buf.writeBoolean(keepExistingNameWhenBlank);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Level level = player.level();

            if (player.blockPosition().distSqr(pos) > 4096) return;
            if (!level.isLoaded(pos)) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof FrequencyBlockEntity frequencyBE)) return;

            // Read the block's frequency BEFORE changing it so we can gate the global rename.
            float currentFrequency = frequencyBE.getFrequency();
            boolean changingFrequency = Float.compare(currentFrequency, frequency) != 0;

            String finalName = networkName.trim();
            if (finalName.isEmpty() && keepExistingNameWhenBlank) {
                finalName = FrequencyNetworkHandler.getNetworkName(level, frequency);
            } else {
                // Allow the global rename only when:
                //   (a) the block is already on this frequency (the player owns/is-on this network), or
                //   (b) the target frequency is unnamed (player is creating a new named network).
                // When moving to an already-named frequency, preserve that name to prevent
                // one player from silently renaming another player's shared network.
                boolean targetIsUnnamed = FrequencyNetworkHandler.getNetworkName(level, frequency).isEmpty();
                if (!changingFrequency || targetIsUnnamed) {
                    FrequencyNetworkHandler.setNetworkName(level, frequency, finalName);
                } else {
                    finalName = FrequencyNetworkHandler.getNetworkName(level, frequency);
                }
            }

            frequencyBE.setFrequency(frequency);
            frequencyBE.setNetworkName(finalName);
            frequencyBE.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
        });
        ctx.get().setPacketHandled(true);
    }
}