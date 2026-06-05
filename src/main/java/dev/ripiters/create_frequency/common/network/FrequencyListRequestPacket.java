package dev.ripiters.create_frequency.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class FrequencyListRequestPacket {

    private final BlockPos pos;

    public FrequencyListRequestPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static FrequencyListRequestPacket decode(FriendlyByteBuf buf) {
        return new FrequencyListRequestPacket(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Level level = player.level();
            if (!level.isLoaded(pos) || player.blockPosition().distSqr(pos) > 4096) {
                return;
            }

            CFPackets.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new FrequencyListResponsePacket(pos, FrequencyNetworkHandler.getActiveFrequencies(level))
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
