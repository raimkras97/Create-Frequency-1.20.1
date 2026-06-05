package dev.ripiters.create_frequency.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class FrequencyListResponsePacket {

    private final BlockPos pos;
    private final List<FrequencyNetworkHandler.FrequencyListEntry> entries;

    public FrequencyListResponsePacket(BlockPos pos, List<FrequencyNetworkHandler.FrequencyListEntry> entries) {
        this.pos = pos;
        this.entries = List.copyOf(entries);
    }

    public static FrequencyListResponsePacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<FrequencyNetworkHandler.FrequencyListEntry> entries = buf.readList(FrequencyListResponsePacket::readEntry);
        return new FrequencyListResponsePacket(pos, entries);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeCollection(entries, FrequencyListResponsePacket::writeEntry);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(pos, entries)));
        ctx.get().setPacketHandled(true);
    }

    private static FrequencyNetworkHandler.FrequencyListEntry readEntry(FriendlyByteBuf buf) {
        return new FrequencyNetworkHandler.FrequencyListEntry(buf.readFloat(), buf.readUtf());
    }

    private static void writeEntry(FriendlyByteBuf buf, FrequencyNetworkHandler.FrequencyListEntry entry) {
        buf.writeFloat(entry.frequency());
        buf.writeUtf(entry.name());
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientHandler {
        private static void handle(BlockPos pos, List<FrequencyNetworkHandler.FrequencyListEntry> entries) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.screen instanceof dev.ripiters.create_frequency.client.gui.FrequencyConfigScreen screen && screen.matches(pos)) {
                screen.receiveFrequencyEntries(entries);
            }
        }
    }
}
