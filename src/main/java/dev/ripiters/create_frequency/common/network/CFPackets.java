package dev.ripiters.create_frequency.common.network;

import dev.ripiters.create_frequency.CreateFrequency;
import dev.ripiters.create_frequency.common.network.controller.FrequencyControllerBindPacket;
import dev.ripiters.create_frequency.common.network.controller.FrequencyControllerInputPacket;
import dev.ripiters.create_frequency.common.network.controller.FrequencyControllerStopLecternPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CFPackets {
    private static final String PROTOCOL_VERSION = "1";

    @SuppressWarnings("removal")
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CreateFrequency.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, ConfigureFrequencyPacket.class,
            ConfigureFrequencyPacket::encode,
            ConfigureFrequencyPacket::decode,
            ConfigureFrequencyPacket::handle);
        CHANNEL.registerMessage(id++, FrequencyListRequestPacket.class,
            FrequencyListRequestPacket::encode,
            FrequencyListRequestPacket::decode,
            FrequencyListRequestPacket::handle);
        CHANNEL.registerMessage(id++, FrequencyListResponsePacket.class,
            FrequencyListResponsePacket::encode,
            FrequencyListResponsePacket::decode,
            FrequencyListResponsePacket::handle);
        CHANNEL.registerMessage(id++, FrequencyControllerBindPacket.class,
            FrequencyControllerBindPacket::encode,
            FrequencyControllerBindPacket::decode,
            FrequencyControllerBindPacket::handle);
        CHANNEL.registerMessage(id++, FrequencyControllerInputPacket.class,
            FrequencyControllerInputPacket::encode,
            FrequencyControllerInputPacket::decode,
            FrequencyControllerInputPacket::handle);
        CHANNEL.registerMessage(id++, FrequencyControllerStopLecternPacket.class,
            FrequencyControllerStopLecternPacket::encode,
            FrequencyControllerStopLecternPacket::decode,
            FrequencyControllerStopLecternPacket::handle);
    }
}