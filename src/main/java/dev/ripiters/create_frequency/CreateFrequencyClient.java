package dev.ripiters.create_frequency;

import dev.ripiters.create_frequency.common.link.controller.FrequencyControllerClientHandler;
import dev.ripiters.create_frequency.config.FrequencyConfig;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraft.client.player.Input;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

@Mod.EventBusSubscriber(modid = CreateFrequency.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CreateFrequencyClient {

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (FrequencyControllerClientHandler.MODE != FrequencyControllerClientHandler.Mode.IDLE) {
            Input input = event.getInput();
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onClientInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (FrequencyControllerClientHandler.MODE == FrequencyControllerClientHandler.Mode.ACTIVE && FrequencyControllerClientHandler.inLectern()) {
            if (event.isUseItem()) {
                FrequencyControllerClientHandler.deactivateInLectern();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        FrequencyControllerClientHandler.tick();
    }

    @Mod.EventBusSubscriber(modid = CreateFrequency.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                            new BaseConfigScreen(parent, CreateFrequency.MODID)
                                    .withSpecs(null, FrequencyConfig.CLIENT.specification, null))
            );
            CreateFrequency.LOGGER.info("Create Frequency: Config Screen Factory registered successfully.");
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            CreateFrequency.LOGGER.info("Create Frequency Client Setup complete.");
            if (FrequencyConfig.CLIENT.enableExtendedLogging.get()) {
                CreateFrequency.LOGGER.info("Extended logging enabled.");
            }
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(),
                    "frequency_controller",
                    FrequencyControllerClientHandler.OVERLAY);
        }
    }
}