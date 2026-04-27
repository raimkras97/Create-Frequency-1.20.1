package dev.ripiters.create_frequency;

import dev.ripiters.create_frequency.common.link.controller.FrequencyControllerServerHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateFrequency.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CreateFrequencyServer {

    @SubscribeEvent
    public static void onServerWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level world = (Level) event.level;
        if (world.isClientSide())
            return;
        FrequencyControllerServerHandler.tick(world);
    }

    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event) {
        LevelAccessor world = event.getLevel();
        CreateFrequency.FREQUENCY_NETWORK_HANDLER.onLoadWorld(world);
    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        LevelAccessor world = event.getLevel();
        CreateFrequency.FREQUENCY_NETWORK_HANDLER.onUnloadWorld(world);
    }
}
